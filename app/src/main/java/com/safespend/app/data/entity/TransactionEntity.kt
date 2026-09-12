package com.safespend.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safespend.app.data.model.TxSource
import com.safespend.app.data.model.TxType
import java.time.LocalDate

/**
 * Amounts are stored in **minor units** (cents) as a Long, never as a Double.
 * 0.1 + 0.2 != 0.3 in binary floating point, and a ledger that drifts by a cent
 * per entry is worse than useless. Formatting back to "Rs 1,250.00" happens once,
 * at the UI edge, in [com.safespend.app.util.Money].
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id"), Index("date"), Index("type")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val type: TxType,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    /** Stored as epoch day so date ranges are plain integer comparisons in SQL. */
    val date: LocalDate,
    val note: String = "",
    val merchant: String? = null,
    val source: TxSource = TxSource.MANUAL,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
