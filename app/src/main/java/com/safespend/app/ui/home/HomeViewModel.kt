package com.safespend.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.data.model.CategorySpend
import com.safespend.app.data.model.PeriodTotals
import com.safespend.app.data.model.TransactionWithCategory
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.domain.SafeToSpend
import com.safespend.app.domain.SafeToSpendCalculator
import com.safespend.app.util.MonthPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val monthLabel: String = "",
    val canGoNext: Boolean = false,
    val isCurrentMonth: Boolean = true,
    val currencySymbol: String = "Rs",
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val netMinor: Long = 0,
    val safeToSpend: SafeToSpend? = null,
    val topCategories: List<CategorySpend> = emptyList(),
    val recent: List<TransactionWithCategory> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val hasAnyData: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: FinanceRepository,
    preferences: UserPreferences,
) : ViewModel() {

    private val _month = MutableStateFlow(MonthPeriod.current())
    val month: StateFlow<MonthPeriod> = _month.asStateFlow()

    private val monthTotals = _month.flatMapLatest { repository.observeTotals(it) }
    private val monthBudget = _month.flatMapLatest { repository.observeTotalBudget(it) }
    private val topCategories = _month.flatMapLatest { repository.observeCategoryTotals(TxType.EXPENSE, it) }
    private val todayTotals = repository.observeDayTotals(LocalDate.now())

    /**
     * Split into two combines because `combine` is only typed up to five sources.
     * Grouping the month-scoped money figures together also means the safe-to-spend
     * calculation happens in one place with one consistent snapshot, rather than
     * recomputing from whichever flow emitted last.
     */
    private val core = combine(
        _month,
        preferences.settings,
        monthTotals,
        monthBudget,
        todayTotals,
    ) { month, settings, totals, budgeted, today ->
        Core(
            month = month,
            currencySymbol = settings.currencySymbol,
            totals = totals,
            safeToSpend = SafeToSpendCalculator.calculate(
                plannedIncomeMinor = settings.plannedIncomeMinor,
                actualIncomeMinor = totals.incomeMinor,
                budgetedMinor = budgeted,
                savingsTargetMinor = settings.savingsTargetMinor,
                spentMinor = totals.expenseMinor,
                // Only the month that contains today has a "today" to spend against.
                spentTodayMinor = if (month.contains(LocalDate.now())) today.expenseMinor else 0,
                monthLengthDays = month.lengthInDays,
                daysRemaining = month.daysRemaining(),
                dayOfMonth = if (month.contains(LocalDate.now())) LocalDate.now().dayOfMonth else month.lengthInDays,
            ),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        core,
        repository.observeRecent(limit = 5),
        topCategories,
        repository.observeGoals(),
    ) { core, recent, categories, goals ->
        HomeUiState(
            monthLabel = core.month.label,
            canGoNext = core.month.yearMonth < YearMonth.now(),
            isCurrentMonth = core.month.yearMonth == YearMonth.now(),
            currencySymbol = core.currencySymbol,
            incomeMinor = core.totals.incomeMinor,
            expenseMinor = core.totals.expenseMinor,
            netMinor = core.totals.netMinor,
            safeToSpend = core.safeToSpend,
            topCategories = categories.take(4),
            recent = recent,
            goals = goals.take(2),
            hasAnyData = recent.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(monthLabel = MonthPeriod.current().label),
    )

    fun previousMonth() {
        _month.value = _month.value.previous()
    }

    fun nextMonth() {
        val next = _month.value.next()
        // Future months hold nothing but an empty ledger and a misleading allowance.
        if (next.yearMonth <= YearMonth.now()) _month.value = next
    }

    private data class Core(
        val month: MonthPeriod,
        val currencySymbol: String,
        val totals: PeriodTotals,
        val safeToSpend: SafeToSpend,
    )
}
