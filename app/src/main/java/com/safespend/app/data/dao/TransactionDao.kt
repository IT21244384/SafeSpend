package com.safespend.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.safespend.app.data.entity.TransactionEntity
import com.safespend.app.data.model.CategorySpend
import com.safespend.app.data.model.DailyTotal
import com.safespend.app.data.model.PeriodTotals
import com.safespend.app.data.model.TransactionWithCategory
import com.safespend.app.data.model.TxType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon_key AS category_icon, c.color_index AS category_color
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        WHERE t.date BETWEEN :from AND :to
        ORDER BY t.date DESC, t.created_at DESC
        """
    )
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon_key AS category_icon, c.color_index AS category_color
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        ORDER BY t.date DESC, t.created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TransactionWithCategory>>

    /**
     * Free-text search across note, merchant and category name. The type and
     * category filters are switched off by passing null, which keeps this to one
     * query instead of four permutations.
     */
    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon_key AS category_icon, c.color_index AS category_color
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        WHERE t.date BETWEEN :from AND :to
          AND (:type IS NULL OR t.type = :type)
          AND (:categoryId IS NULL OR t.category_id = :categoryId)
          AND (
                :query = ''
                OR t.note LIKE '%' || :query || '%'
                OR IFNULL(t.merchant, '') LIKE '%' || :query || '%'
                OR c.name LIKE '%' || :query || '%'
              )
        ORDER BY t.date DESC, t.created_at DESC
        """
    )
    fun observeFiltered(
        from: LocalDate,
        to: LocalDate,
        type: TxType?,
        categoryId: Long?,
        query: String,
    ): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT
            IFNULL(SUM(CASE WHEN type = 'INCOME'  THEN amount_minor END), 0) AS income_minor,
            IFNULL(SUM(CASE WHEN type = 'EXPENSE' THEN amount_minor END), 0) AS expense_minor
        FROM transactions
        WHERE date BETWEEN :from AND :to
        """
    )
    fun observeTotals(from: LocalDate, to: LocalDate): Flow<PeriodTotals>

    @Query(
        """
        SELECT c.id AS category_id, c.name AS category_name, c.icon_key AS category_icon,
               c.color_index AS category_color, SUM(t.amount_minor) AS total_minor
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        WHERE t.type = :type AND t.date BETWEEN :from AND :to
        GROUP BY c.id
        ORDER BY total_minor DESC
        """
    )
    fun observeCategoryTotals(type: TxType, from: LocalDate, to: LocalDate): Flow<List<CategorySpend>>

    @Query(
        """
        SELECT date, SUM(amount_minor) AS total_minor
        FROM transactions
        WHERE type = :type AND date BETWEEN :from AND :to
        GROUP BY date
        ORDER BY date
        """
    )
    fun observeDailyTotals(type: TxType, from: LocalDate, to: LocalDate): Flow<List<DailyTotal>>

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
