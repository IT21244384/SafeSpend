package com.safespend.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.model.TxType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND is_archived = 0 ORDER BY sort_order, name")
    fun observeByType(type: TxType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    /**
     * Categories are archived, never deleted: transactions reference them with
     * ON DELETE RESTRICT, and history must stay readable.
     */
    @Query("UPDATE categories SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
