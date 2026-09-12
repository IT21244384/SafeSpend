package com.safespend.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safespend.app.data.entity.BudgetEntity
import com.safespend.app.data.model.BudgetWithSpend
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period_month = :month")
    suspend fun find(categoryId: Long, month: String): BudgetEntity?

    /**
     * Every envelope for a month with its spend already summed.
     *
     * The spend is a correlated subquery rather than a JOIN + GROUP BY: joining
     * transactions would multiply budget rows before aggregation, and an envelope
     * with no spending yet must still appear at zero.
     */
    @Query(
        """
        SELECT b.id AS budget_id, b.category_id AS category_id, c.name AS category_name,
               c.icon_key AS category_icon, c.color_index AS category_color,
               b.period_month AS period_month, b.limit_minor AS limit_minor,
               IFNULL((
                   SELECT SUM(t.amount_minor) FROM transactions t
                   WHERE t.category_id = b.category_id
                     AND t.type = 'EXPENSE'
                     AND t.date BETWEEN :from AND :to
               ), 0) AS spent_minor
        FROM budgets b
        JOIN categories c ON c.id = b.category_id
        WHERE b.period_month = :month
        ORDER BY (CAST(spent_minor AS REAL) / MAX(b.limit_minor, 1)) DESC
        """
    )
    fun observeForMonth(month: String, from: LocalDate, to: LocalDate): Flow<List<BudgetWithSpend>>

    @Query("SELECT IFNULL(SUM(limit_minor), 0) FROM budgets WHERE period_month = :month")
    fun observeTotalBudget(month: String): Flow<Long>

    /** Copies a month's envelopes forward so the user doesn't re-enter them. */
    @Query(
        """
        INSERT OR IGNORE INTO budgets (category_id, period_month, limit_minor)
        SELECT category_id, :toMonth, limit_minor FROM budgets WHERE period_month = :fromMonth
        """
    )
    suspend fun copyMonth(fromMonth: String, toMonth: String)
}
