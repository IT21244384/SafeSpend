package com.safespend.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val currencySymbol: String = "Rs",
) {
    val totalSavedMinor: Long get() = goals.sumOf { it.savedMinor }
    val totalTargetMinor: Long get() = goals.sumOf { it.targetMinor }
}

class GoalsViewModel(
    private val repository: FinanceRepository,
    preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        repository.observeGoals(),
        preferences.settings,
    ) { goals, settings ->
        GoalsUiState(goals = goals, currencySymbol = settings.currencySymbol)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState(),
    )

    fun addGoal(name: String, targetMinor: Long, targetDate: LocalDate?) {
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    name = name.trim(),
                    targetMinor = targetMinor,
                    targetDate = targetDate,
                    // Cycles through the chart ramp so two new goals never look alike.
                    colorIndex = (uiState.value.goals.size) % 8,
                ),
            )
        }
    }

    fun contribute(goal: GoalEntity, deltaMinor: Long) {
        viewModelScope.launch { repository.contributeToGoal(goal.id, deltaMinor) }
    }

    fun delete(goal: GoalEntity) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
