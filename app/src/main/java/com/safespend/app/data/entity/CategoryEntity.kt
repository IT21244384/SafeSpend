package com.safespend.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safespend.app.data.model.TxType

@Entity(
    tableName = "categories",
    indices = [Index("type")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key into [com.safespend.app.ui.components.CategoryIcons]. */
    @ColumnInfo(name = "icon_key") val iconKey: String,
    /** Index into the theme's chart series ramp, so category colours stay on-palette. */
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    val type: TxType,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)
