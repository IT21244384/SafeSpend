package com.safespend.app.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.model.BudgetWithSpend
import com.safespend.app.ui.components.CategoryBadge
import com.safespend.app.ui.components.EmptyState
import com.safespend.app.ui.components.MonthSwitcher
import com.safespend.app.ui.components.ProgressTrack
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.SectionHeader
import com.safespend.app.ui.components.StatTile
import com.safespend.app.ui.components.atIndex
import com.safespend.app.ui.theme.moneyColors
import com.safespend.app.util.Money

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Budgets",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = state.monthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MonthSwitcher(
                    label = "",
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    nextEnabled = state.canGoNext,
                )
            }
        }

        if (!state.isEmpty) {
            item {
                SectionCard(contentPadding = 20) {
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Budgeted",
                            value = Money.format(state.totalLimitMinor, state.currencySymbol),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Spent",
                            value = Money.format(state.totalSpentMinor, state.currencySymbol),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Left",
                            value = Money.format(state.totalRemainingMinor, state.currencySymbol),
                            valueColor = if (state.totalRemainingMinor < 0) moneyColors.expense
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    ProgressTrack(
                        progress = state.overallProgress,
                        color = if (state.totalRemainingMinor < 0) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                    )
                    if (state.overCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${state.overCount} ${if (state.overCount == 1) "envelope is" else "envelopes are"} over budget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }

        if (state.isEmpty) {
            item {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.Savings,
                        title = "No envelopes yet",
                        message = "Give each category a monthly ceiling. SafeSpend then uses the total to work out what you can safely spend each day.",
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AssistChip(
                            onClick = viewModel::copyFromPreviousMonth,
                            label = { Text("Copy last month") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        items(state.envelopes, key = { it.budgetId }) { envelope ->
            EnvelopeCard(
                envelope = envelope,
                currencySymbol = state.currencySymbol,
                onEdit = {
                    editing = EditTarget(
                        categoryId = envelope.categoryId,
                        categoryName = envelope.categoryName,
                        currentMinor = envelope.limitMinor,
                    )
                },
                onRemove = { viewModel.removeBudget(envelope.budgetId) },
            )
        }

        if (state.unbudgeted.isNotEmpty()) {
            item {
                SectionHeader(title = "Add an envelope")
            }
            item {
                SectionCard {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.unbudgeted.forEach { category ->
                            AssistChip(
                                onClick = {
                                    editing = EditTarget(
                                        categoryId = category.id,
                                        categoryName = category.name,
                                        currentMinor = 0,
                                    )
                                },
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { target ->
        BudgetAmountDialog(
            target = target,
            currencySymbol = state.currencySymbol,
            onDismiss = { editing = null },
            onConfirm = { minor ->
                viewModel.setBudget(target.categoryId, minor)
                editing = null
            },
        )
    }
}

private data class EditTarget(
    val categoryId: Long,
    val categoryName: String,
    val currentMinor: Long,
)

@Composable
private fun EnvelopeCard(
    envelope: BudgetWithSpend,
    currencySymbol: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val palette = moneyColors.chartSeries
    val base = palette.atIndex(envelope.categoryColor)
    val barColor = if (envelope.isOverBudget) MaterialTheme.colorScheme.tertiary else base

    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(iconKey = envelope.categoryIcon, tint = base, size = 38)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = envelope.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (envelope.isOverBudget) {
                        "${Money.format(-envelope.remainingMinor, currencySymbol)} over"
                    } else {
                        "${Money.format(envelope.remainingMinor, currencySymbol)} left"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (envelope.isOverBudget) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove ${envelope.categoryName} budget",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ProgressTrack(progress = envelope.progress, color = barColor)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = Money.format(envelope.spentMinor, currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "of ${Money.format(envelope.limitMinor, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetAmountDialog(
    target: EditTarget,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var input by remember {
        mutableStateOf(if (target.currentMinor > 0) Money.formatPlain(target.currentMinor) else "")
    }
    val parsed = Money.parse(input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${target.categoryName} budget") },
        text = {
            Column {
                Text(
                    text = "How much do you want to allow for ${target.categoryName} this month?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value.filter { it.isDigit() || it == '.' }
                    },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && parsed > 0,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
