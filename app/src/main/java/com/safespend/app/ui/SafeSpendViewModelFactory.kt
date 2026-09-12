package com.safespend.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.safespend.app.SafeSpendApp
import com.safespend.app.ui.budgets.BudgetsViewModel
import com.safespend.app.ui.entry.AddEditTransactionViewModel
import com.safespend.app.ui.goals.GoalsViewModel
import com.safespend.app.ui.home.HomeViewModel
import com.safespend.app.ui.insights.InsightsViewModel
import com.safespend.app.ui.settings.SettingsViewModel
import com.safespend.app.ui.transactions.TransactionsViewModel

/**
 * One factory for every ViewModel in the app.
 *
 * With a hand-rolled container this is the join between the two: the factory reads
 * the container off the Application instance in [CreationExtras], so screens can
 * call `viewModel(factory = SafeSpendViewModelFactory)` without threading
 * dependencies through composable parameters.
 */
object SafeSpendViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
            "SafeSpendViewModelFactory needs the Application in CreationExtras"
        } as SafeSpendApp
        val container = application.container

        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.repository, container.preferences)

            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(container.repository, container.preferences)

            modelClass.isAssignableFrom(BudgetsViewModel::class.java) ->
                BudgetsViewModel(container.repository, container.preferences)

            modelClass.isAssignableFrom(InsightsViewModel::class.java) ->
                InsightsViewModel(container.repository, container.preferences)

            modelClass.isAssignableFrom(GoalsViewModel::class.java) ->
                GoalsViewModel(container.repository, container.preferences)

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.repository, container.preferences)

            // The only one that needs its navigation argument, so the only one that
            // pays for a SavedStateHandle.
            modelClass.isAssignableFrom(AddEditTransactionViewModel::class.java) ->
                AddEditTransactionViewModel(
                    container.repository,
                    container.preferences,
                    extras.createSavedStateHandle(),
                )

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
