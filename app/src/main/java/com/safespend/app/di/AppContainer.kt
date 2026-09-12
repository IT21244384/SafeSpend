package com.safespend.app.di

import android.content.Context
import com.safespend.app.data.local.SafeSpendDatabase
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository

/**
 * Hand-rolled dependency container.
 *
 * Hilt would work, but for an app with one database, one preferences store and one
 * repository it adds an annotation processor and a layer of generated code to
 * understand for no behavioural gain. Everything is constructed here, lazily, and
 * ViewModels receive it through a factory.
 */
class AppContainer(context: Context) {

    private val database: SafeSpendDatabase by lazy { SafeSpendDatabase.get(context) }

    val repository: FinanceRepository by lazy {
        FinanceRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            goalDao = database.goalDao(),
        )
    }

    val preferences: UserPreferences by lazy { UserPreferences(context) }
}
