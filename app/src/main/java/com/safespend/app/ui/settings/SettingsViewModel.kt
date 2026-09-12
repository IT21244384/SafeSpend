package com.safespend.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.prefs.Settings
import com.safespend.app.data.prefs.ThemeMode
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: FinanceRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    val settings: StateFlow<Settings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    fun setPlannedIncome(minor: Long) {
        viewModelScope.launch { preferences.setPlannedIncome(minor) }
    }

    fun setSavingsTarget(minor: Long) {
        viewModelScope.launch { preferences.setSavingsTarget(minor) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { preferences.setCurrencySymbol(symbol.trim().ifEmpty { "Rs" }) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    /**
     * Wipes transactions only. Categories, budgets and goals survive, because the
     * thing people actually want here is a clean ledger, not a factory reset — and
     * re-entering sixteen categories to clear a test entry would be punishment.
     */
    fun clearAllTransactions() {
        viewModelScope.launch { repository.deleteAllTransactions() }
    }
}
