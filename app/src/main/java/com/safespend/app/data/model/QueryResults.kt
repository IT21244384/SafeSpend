package com.safespend.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.safespend.app.data.entity.TransactionEntity
import java.time.LocalDate

/** A transaction joined to the category it belongs to — what every list row needs. */
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: Int,
)

/** Total spend in one category over a period — the donut chart's input. */
data class CategorySpend(
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: Int,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
)

/** Spend for a single day — the trend chart's input. */
data class DailyTotal(
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
)

/** Money in and money out for a period, in one row. */
data class PeriodTotals(
    @ColumnInfo(name = "income_minor") val incomeMinor: Long,
    @ColumnInfo(name = "expense_minor") val expenseMinor: Long,
) {
    val netMinor: Long get() = incomeMinor - expenseMinor
}

/** A budget envelope with the spend already counted against it. */
data class BudgetWithSpend(
    @ColumnInfo(name = "budget_id") val budgetId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: Int,
    @ColumnInfo(name = "period_month") val periodMonth: String,
    @ColumnInfo(name = "limit_minor") val limitMinor: Long,
    @ColumnInfo(name = "spent_minor") val spentMinor: Long,
) {
    val remainingMinor: Long get() = limitMinor - spentMinor
    val isOverBudget: Boolean get() = spentMinor > limitMinor
    /** Clamped so a 300%-over envelope still draws a full bar rather than overflowing. */
    val progress: Float
        get() = if (limitMinor <= 0) 0f else (spentMinor.toFloat() / limitMinor).coerceIn(0f, 1f)
}
