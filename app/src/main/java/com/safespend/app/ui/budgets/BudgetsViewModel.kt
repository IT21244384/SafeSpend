package com.safespend.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.model.BudgetWithSpend
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.util.MonthPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetsUiState(
    val monthLabel: String = "",
    val canGoNext: Boolean = false,
    val envelopes: List<BudgetWithSpend> = emptyList(),
    val unbudgeted: List<CategoryEntity> = emptyList(),
    val totalLimitMinor: Long = 0,
    val totalSpentMinor: Long = 0,
    val currencySymbol: String = "Rs",
) {
    val totalRemainingMinor: Long get() = totalLimitMinor - totalSpentMinor
    val overCount: Int get() = envelopes.count { it.isOverBudget }
    val isEmpty: Boolean get() = envelopes.isEmpty()
    val overallProgress: Float
        get() = if (totalLimitMinor <= 0) 0f
        else (totalSpentMinor.toFloat() / totalLimitMinor).coerceIn(0f, 1f)
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val repository: FinanceRepository,
    preferences: UserPreferences,
) : ViewModel() {

    private val _month = MutableStateFlow(MonthPeriod.current())

    private val envelopes = _month.flatMapLatest { repository.observeBudgets(it) }

    val uiState: StateFlow<BudgetsUiState> = combine(
        _month,
        envelopes,
        repository.observeCategories(TxType.EXPENSE),
        preferences.settings,
    ) { month, envelopes, categories, settings ->
        val budgetedIds = envelopes.map { it.categoryId }.toSet()
        BudgetsUiState(
            monthLabel = month.label,
            canGoNext = month.yearMonth < YearMonth.now().plusMonths(1),
            envelopes = envelopes,
            unbudgeted = categories.filterNot { it.id in budgetedIds },
            totalLimitMinor = envelopes.sumOf { it.limitMinor },
            totalSpentMinor = envelopes.sumOf { it.spentMinor },
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState(monthLabel = MonthPeriod.current().label),
    )

    fun previousMonth() {
        _month.value = _month.value.previous()
    }

    /**
     * Budgets are the one screen that may look one month ahead: planning next
     * month's envelopes before it starts is the whole point of envelope budgeting.
     */
    fun nextMonth() {
        val next = _month.value.next()
        if (next.yearMonth <= YearMonth.now().plusMonths(1)) _month.value = next
    }

    fun setBudget(categoryId: Long, limitMinor: Long) {
        viewModelScope.launch {
            repository.setBudget(categoryId, _month.value, limitMinor)
        }
    }

    fun removeBudget(budgetId: Long) {
        viewModelScope.launch { repository.removeBudget(budgetId) }
    }

    /** Pulls last month's envelopes forward; existing ones are left untouched. */
    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val target = _month.value
            repository.copyBudgets(from = target.previous(), to = target)
        }
    }

    suspend fun hasPreviousMonthBudgets(): Boolean =
        repository.observeTotalBudget(_month.value.previous()).first() > 0
}
