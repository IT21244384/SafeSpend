package com.safespend.app.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The month the whole app is scoped to. Screens pass one of these around instead
 * of juggling loose start/end dates, which is what kept the "spent this month"
 * figure and the budget bars from ever disagreeing.
 */
data class MonthPeriod(val yearMonth: YearMonth) {

    val start: LocalDate get() = yearMonth.atDay(1)
    val end: LocalDate get() = yearMonth.atEndOfMonth()

    /** "2026-09" — the key budgets are stored under. */
    val key: String get() = yearMonth.format(KEY_FORMAT)

    /** "September 2026" for headers. */
    val label: String
        get() = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"

    /** "Sep" for compact chips. */
    val shortLabel: String
        get() = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    val lengthInDays: Int get() = yearMonth.lengthOfMonth()

    fun previous(): MonthPeriod = MonthPeriod(yearMonth.minusMonths(1))

    fun next(): MonthPeriod = MonthPeriod(yearMonth.plusMonths(1))

    fun contains(date: LocalDate): Boolean = YearMonth.from(date) == yearMonth

    /**
     * Days still to come, counting today. A past month returns 0, a future month
     * returns its full length — so the safe-to-spend maths never divides by zero
     * and never quietly assumes "today" is inside the month being viewed.
     */
    fun daysRemaining(today: LocalDate = LocalDate.now()): Int = when {
        YearMonth.from(today) == yearMonth -> lengthInDays - today.dayOfMonth + 1
        today.isBefore(start) -> lengthInDays
        else -> 0
    }

    companion object {
        private val KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        fun current(today: LocalDate = LocalDate.now()): MonthPeriod = MonthPeriod(YearMonth.from(today))

        fun of(key: String): MonthPeriod = MonthPeriod(YearMonth.parse(key, KEY_FORMAT))
    }
}

/** "12 Sep", "12 Sep 2025" once the year stops being the current one. */
fun LocalDate.displayLabel(today: LocalDate = LocalDate.now()): String =
    if (year == today.year) format(DAY_MONTH) else format(DAY_MONTH_YEAR)

/** "Today" / "Yesterday" / "12 Sep" — used for the date headers in the ledger. */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> displayLabel(today)
}

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
