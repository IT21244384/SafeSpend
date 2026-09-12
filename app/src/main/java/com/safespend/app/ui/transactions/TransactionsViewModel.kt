package com.safespend.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.entity.TransactionEntity
import com.safespend.app.data.model.TransactionWithCategory
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.util.MonthPeriod
import com.safespend.app.util.relativeLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Transactions grouped under a date heading, the way a bank statement reads. */
data class DayGroup(
    val date: LocalDate,
    val label: String,
    val items: List<TransactionWithCategory>,
    val netMinor: Long,
)

data class TransactionsUiState(
    val monthLabel: String = "",
    val canGoNext: Boolean = false,
    val query: String = "",
    val typeFilter: TxType? = null,
    val categoryFilter: Long? = null,
    val groups: List<DayGroup> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "Rs",
    val matchCount: Int = 0,
    val matchTotalMinor: Long = 0,
    val hasFilters: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val repository: FinanceRepository,
    preferences: UserPreferences,
) : ViewModel() {

    private val month = MutableStateFlow(MonthPeriod.current())
    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TxType?>(null)
    private val categoryFilter = MutableStateFlow<Long?>(null)

    /** The last deleted transaction, held so the snackbar can put it back. */
    private var lastDeleted: TransactionEntity? = null

    private val filters = combine(month, query, typeFilter, categoryFilter) { m, q, t, c ->
        Filters(m, q, t, c)
    }

    private val results = filters.flatMapLatest { f ->
        repository.observeFiltered(f.month, f.type, f.category, f.query.trim())
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        filters,
        results,
        repository.observeCategories(),
        preferences.settings,
    ) { f, items, categories, settings ->
        TransactionsUiState(
            monthLabel = f.month.label,
            canGoNext = f.month.yearMonth < YearMonth.now(),
            query = f.query,
            typeFilter = f.type,
            categoryFilter = f.category,
            groups = items.groupIntoDays(),
            categories = categories,
            currencySymbol = settings.currencySymbol,
            matchCount = items.size,
            matchTotalMinor = items.sumOf {
                if (it.transaction.type == TxType.EXPENSE) it.transaction.amountMinor else 0L
            },
            hasFilters = f.query.isNotBlank() || f.type != null || f.category != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(monthLabel = MonthPeriod.current().label),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setTypeFilter(type: TxType?) {
        typeFilter.value = type
    }

    fun setCategoryFilter(id: Long?) {
        categoryFilter.value = id
    }

    fun clearFilters() {
        query.value = ""
        typeFilter.value = null
        categoryFilter.value = null
    }

    fun previousMonth() {
        month.value = month.value.previous()
    }

    fun nextMonth() {
        val next = month.value.next()
        if (next.yearMonth <= YearMonth.now()) month.value = next
    }

    /**
     * Deletes immediately and keeps a copy in memory. An "are you sure?" dialog for
     * something this cheap to undo is friction; the snackbar is the confirmation.
     */
    fun delete(item: TransactionWithCategory) {
        viewModelScope.launch {
            lastDeleted = item.transaction
            repository.deleteTransaction(item.transaction)
        }
    }

    fun undoDelete() {
        val restore = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            // id is reset so Room assigns a fresh one rather than colliding with a
            // row the user may have created in the meantime.
            repository.addTransaction(restore.copy(id = 0))
        }
    }

    private fun List<TransactionWithCategory>.groupIntoDays(): List<DayGroup> =
        groupBy { it.transaction.date }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) ->
                DayGroup(
                    date = date,
                    label = date.relativeLabel(),
                    items = items,
                    netMinor = items.sumOf {
                        if (it.transaction.type == TxType.INCOME) it.transaction.amountMinor
                        else -it.transaction.amountMinor
                    },
                )
            }

    private data class Filters(
        val month: MonthPeriod,
        val query: String,
        val type: TxType?,
        val category: Long?,
    )
}
