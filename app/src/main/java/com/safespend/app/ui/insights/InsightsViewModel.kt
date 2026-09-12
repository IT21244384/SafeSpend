package com.safespend.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.model.CategorySpend
import com.safespend.app.data.model.DailyTotal
import com.safespend.app.data.model.PeriodTotals
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.util.MonthPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class InsightsUiState(
    val monthLabel: String = "",
    val canGoNext: Boolean = false,
    val currencySymbol: String = "Rs",
    val totals: PeriodTotals = PeriodTotals(0, 0),
    val categories: List<CategorySpend> = emptyList(),
    val dailySpend: List<DailyTotal> = emptyList(),
    val monthLengthDays: Int = 30,
    val monthStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val previousMonthExpenseMinor: Long = 0,
) {
    val hasData: Boolean get() = categories.isNotEmpty()

    /** Positive means this month is heavier than last. Null when there's nothing to compare. */
    val changeVsLastMonth: Float?
        get() = if (previousMonthExpenseMinor <= 0) null
        else (totals.expenseMinor - previousMonthExpenseMinor).toFloat() / previousMonthExpenseMinor

    val busiestDay: DailyTotal? get() = dailySpend.maxByOrNull { it.totalMinor }

    val averagePerActiveDay: Long
        get() = dailySpend.filter { it.totalMinor > 0 }
            .let { active -> if (active.isEmpty()) 0 else active.sumOf { it.totalMinor } / active.size }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: FinanceRepository,
    preferences: UserPreferences,
) : ViewModel() {

    private val _month = MutableStateFlow(MonthPeriod.current())

    private val categories = _month.flatMapLatest {
        repository.observeCategoryTotals(TxType.EXPENSE, it)
    }
    private val daily = _month.flatMapLatest {
        repository.observeDailyTotals(TxType.EXPENSE, it)
    }
    private val totals = _month.flatMapLatest { repository.observeTotals(it) }
    private val previousTotals = _month.flatMapLatest { repository.observeTotals(it.previous()) }

    val uiState: StateFlow<InsightsUiState> = combine(
        _month,
        categories,
        daily,
        totals,
        combine(previousTotals, preferences.settings) { prev, settings -> prev to settings },
    ) { month, categories, daily, totals, (previous, settings) ->
        InsightsUiState(
            monthLabel = month.label,
            canGoNext = month.yearMonth < YearMonth.now(),
            currencySymbol = settings.currencySymbol,
            totals = totals,
            categories = categories,
            dailySpend = daily,
            monthLengthDays = month.lengthInDays,
            monthStart = month.start,
            previousMonthExpenseMinor = previous.expenseMinor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(monthLabel = MonthPeriod.current().label),
    )

    fun previousMonth() {
        _month.value = _month.value.previous()
    }

    fun nextMonth() {
        val next = _month.value.next()
        if (next.yearMonth <= YearMonth.now()) _month.value = next
    }
}
