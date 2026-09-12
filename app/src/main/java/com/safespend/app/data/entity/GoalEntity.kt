package com.safespend.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_minor") val targetMinor: Long,
    @ColumnInfo(name = "saved_minor") val savedMinor: Long = 0,
    @ColumnInfo(name = "target_date") val targetDate: LocalDate? = null,
    @ColumnInfo(name = "color_index") val colorIndex: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
