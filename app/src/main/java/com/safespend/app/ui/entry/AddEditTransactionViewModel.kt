package com.safespend.app.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.entity.TransactionEntity
import com.safespend.app.data.model.TxSource
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.domain.SmsTransactionParser
import com.safespend.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EntryUiState(
    val isEditing: Boolean = false,
    val type: TxType = TxType.EXPENSE,
    val amountInput: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val merchant: String = "",
    val currencySymbol: String = "Rs",
    val source: TxSource = TxSource.MANUAL,
    val smsSheetOpen: Boolean = false,
    val smsInput: String = "",
    val smsError: String? = null,
    val smsApplied: Boolean = false,
    val amountError: String? = null,
    val finished: Boolean = false,
) {
    val canSave: Boolean
        get() = Money.parse(amountInput).let { it != null && it > 0 } && selectedCategoryId != null
}

class AddEditTransactionViewModel(
    private val repository: FinanceRepository,
    private val preferences: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<String>("txId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(EntryUiState(isEditing = editingId != 0L))
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val symbol = preferences.settings.first().currencySymbol
            _state.update { it.copy(currencySymbol = symbol) }

            if (editingId != 0L) {
                repository.transaction(editingId)?.let { tx ->
                    _state.update {
                        it.copy(
                            type = tx.type,
                            amountInput = Money.formatPlain(tx.amountMinor),
                            selectedCategoryId = tx.categoryId,
                            date = tx.date,
                            note = tx.note,
                            merchant = tx.merchant.orEmpty(),
                            source = tx.source,
                        )
                    }
                }
            }
            loadCategories(_state.value.type)
        }
    }

    private suspend fun loadCategories(type: TxType) {
        val categories = repository.observeCategories(type).first()
        _state.update { current ->
            current.copy(
                categories = categories,
                // Keep the selection only if it still belongs to the visible list,
                // otherwise an income category could survive a switch to Expense.
                selectedCategoryId = current.selectedCategoryId
                    ?.takeIf { id -> categories.any { it.id == id } },
            )
        }
    }

    fun setType(type: TxType) {
        if (type == _state.value.type) return
        _state.update { it.copy(type = type) }
        viewModelScope.launch { loadCategories(type) }
    }

    fun setAmount(value: String) {
        // Accept only digits and a single decimal point while typing; rejecting on
        // save instead would let the user fill the whole form before being told.
        val filtered = value.filter { it.isDigit() || it == '.' }
        val normalised = filtered.split(".").let { parts ->
            if (parts.size <= 1) filtered
            else parts[0] + "." + parts.drop(1).joinToString("").take(2)
        }
        _state.update { it.copy(amountInput = normalised, amountError = null) }
    }

    fun selectCategory(id: Long) = _state.update { it.copy(selectedCategoryId = id) }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun setMerchant(value: String) = _state.update { it.copy(merchant = value) }

    // ---- Smart paste ------------------------------------------------------

    fun openSmsSheet() = _state.update { it.copy(smsSheetOpen = true, smsError = null) }

    fun closeSmsSheet() = _state.update { it.copy(smsSheetOpen = false, smsInput = "", smsError = null) }

    fun setSmsInput(value: String) = _state.update { it.copy(smsInput = value, smsError = null) }

    /**
     * Fills the form from a pasted bank alert. Everything it sets stays editable —
     * the parser is a head start, not an authority, and the user reviews the draft
     * before it ever reaches the database.
     */
    fun applySms() {
        val text = _state.value.smsInput
        val parsed = SmsTransactionParser.parse(text)
        if (parsed == null) {
            _state.update {
                it.copy(smsError = "Couldn't find an amount in that message. Enter it manually below.")
            }
            return
        }

        viewModelScope.launch {
            val categories = repository.observeCategories(parsed.type).first()
            val matched = parsed.suggestedCategory
                ?.let { name -> categories.firstOrNull { it.name.equals(name, ignoreCase = true) } }

            _state.update { current ->
                current.copy(
                    type = parsed.type,
                    categories = categories,
                    amountInput = Money.formatPlain(parsed.amountMinor),
                    merchant = parsed.merchant ?: current.merchant,
                    date = parsed.date ?: current.date,
                    selectedCategoryId = matched?.id ?: current.selectedCategoryId
                        ?.takeIf { id -> categories.any { it.id == id } },
                    source = TxSource.SMS_IMPORT,
                    smsSheetOpen = false,
                    smsInput = "",
                    smsError = null,
                    smsApplied = true,
                )
            }
        }
    }

    fun dismissSmsApplied() = _state.update { it.copy(smsApplied = false) }

    // ---- Persistence ------------------------------------------------------

    fun save() {
        val current = _state.value
        val amount = Money.parse(current.amountInput)
        if (amount == null || amount <= 0) {
            _state.update { it.copy(amountError = "Enter an amount greater than zero") }
            return
        }
        val categoryId = current.selectedCategoryId ?: return

        viewModelScope.launch {
            val entity = TransactionEntity(
                id = editingId,
                amountMinor = amount,
                type = current.type,
                categoryId = categoryId,
                date = current.date,
                note = current.note.trim(),
                merchant = current.merchant.trim().takeIf { it.isNotEmpty() },
                source = current.source,
            )
            if (editingId == 0L) repository.addTransaction(entity) else repository.updateTransaction(entity)
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        if (editingId == 0L) return
        viewModelScope.launch {
            repository.transaction(editingId)?.let { repository.deleteTransaction(it) }
            _state.update { it.copy(finished = true) }
        }
    }
}
