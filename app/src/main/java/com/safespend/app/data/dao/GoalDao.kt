package com.safespend.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.safespend.app.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    /** Clamped at zero so withdrawing more than was saved cannot go negative. */
    @Query("UPDATE goals SET saved_minor = MAX(0, saved_minor + :deltaMinor) WHERE id = :id")
    suspend fun addToSaved(id: Long, deltaMinor: Long)
}
