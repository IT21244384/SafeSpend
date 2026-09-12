package com.safespend.app.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.ui.components.EmptyState
import com.safespend.app.ui.components.ProgressTrack
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.atIndex
import com.safespend.app.ui.theme.moneyColors
import com.safespend.app.util.Money
import com.safespend.app.util.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var contributeTo by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Savings goals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New goal")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.goals.isEmpty()) {
                item {
                    SectionCard {
                        EmptyState(
                            icon = Icons.Outlined.Savings,
                            title = "No goals yet",
                            message = "Set aside money for something specific — a laptop, an emergency fund, a trip — and watch it fill up.",
                        )
                    }
                }
            } else {
                item {
                    SectionCard(contentPadding = 20) {
                        Text(
                            text = "Saved across all goals",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.format(state.totalSavedMinor, state.currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "of ${Money.format(state.totalTargetMinor, state.currencySymbol)} targeted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(state.goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    currencySymbol = state.currencySymbol,
                    onContribute = { contributeTo = goal },
                    onDelete = { viewModel.delete(goal) },
                )
            }
        }
    }

    if (showAdd) {
        AddGoalDialog(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAdd = false },
            onConfirm = { name, target ->
                viewModel.addGoal(name, target, null)
                showAdd = false
            },
        )
    }

    contributeTo?.let { goal ->
        ContributeDialog(
            goal = goal,
            currencySymbol = state.currencySymbol,
            onDismiss = { contributeTo = null },
            onConfirm = { delta ->
                viewModel.contribute(goal, delta)
                contributeTo = null
            },
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    currencySymbol: String,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = moneyColors.chartSeries.atIndex(goal.colorIndex)
    val progress = if (goal.targetMinor <= 0) 0f
    else (goal.savedMinor.toFloat() / goal.targetMinor).coerceIn(0f, 1f)
    val complete = goal.savedMinor >= goal.targetMinor && goal.targetMinor > 0

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (complete) {
                        "Goal reached"
                    } else {
                        "${Money.format(goal.targetMinor - goal.savedMinor, currencySymbol)} to go" +
                            (goal.targetDate?.let { " · by ${it.displayLabel()}" } ?: "")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (complete) moneyColors.income
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${goal.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ProgressTrack(progress = progress, color = color, height = 10)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${Money.format(goal.savedMinor, currencySymbol)} of ${Money.format(goal.targetMinor, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
                onClick = onContribute,
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Add money")
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val parsed = Money.parse(target)
    val valid = name.isNotBlank() && parsed != null && parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What are you saving for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { value -> target = value.filter { it.isDigit() || it == '.' } },
                    label = { Text("Target amount") },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onConfirm(name, it) } }, enabled = valid) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ContributeDialog(
    goal: GoalEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    val parsed = Money.parse(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.name) },
        text = {
            Column {
                Text(
                    text = "Move money into this goal, or take some back out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == '.' } },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { parsed?.let { onConfirm(-it) } },
                    enabled = parsed != null && parsed > 0,
                ) { Text("Withdraw") }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { parsed?.let { onConfirm(it) } },
                    enabled = parsed != null && parsed > 0,
                    shape = MaterialTheme.shapes.small,
                ) { Text("Add") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
