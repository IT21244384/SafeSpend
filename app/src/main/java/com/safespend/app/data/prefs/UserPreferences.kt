package com.safespend.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safespend_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Small, single-value settings. These live in DataStore rather than Room because
 * they are preferences, not records: there is exactly one of each, they have no
 * history, and nothing joins against them.
 */
data class Settings(
    val plannedIncomeMinor: Long = 0,
    val savingsTargetMinor: Long = 0,
    val currencySymbol: String = "Rs",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val PLANNED_INCOME = longPreferencesKey("planned_income_minor")
        val SAVINGS_TARGET = longPreferencesKey("savings_target_minor")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            plannedIncomeMinor = prefs[Keys.PLANNED_INCOME] ?: 0,
            savingsTargetMinor = prefs[Keys.SAVINGS_TARGET] ?: 0,
            currencySymbol = prefs[Keys.CURRENCY] ?: "Rs",
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            onboardingDone = prefs[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setPlannedIncome(minor: Long) = edit { it[Keys.PLANNED_INCOME] = minor }

    suspend fun setSavingsTarget(minor: Long) = edit { it[Keys.SAVINGS_TARGET] = minor }

    suspend fun setCurrencySymbol(symbol: String) = edit { it[Keys.CURRENCY] = symbol }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDED] = done }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
