package com.safespend.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.data.model.CategorySpend
import com.safespend.app.domain.SafeToSpend
import com.safespend.app.ui.components.CategoryBadge
import com.safespend.app.ui.components.EmptyState
import com.safespend.app.ui.components.MonthSwitcher
import com.safespend.app.ui.components.ProgressTrack
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.SectionHeader
import com.safespend.app.ui.components.StatTile
import com.safespend.app.ui.components.TransactionRow
import com.safespend.app.ui.components.atIndex
import com.safespend.app.ui.theme.moneyColors
import com.safespend.app.util.Money

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onSeeInsights: () -> Unit,
    onSeeGoals: () -> Unit,
    onSeeSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "SafeSpend",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = state.monthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonthSwitcher(
                        label = "",
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                        nextEnabled = state.canGoNext,
                    )
                    IconButton(onClick = onSeeSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SafeToSpendCard(
                safeToSpend = state.safeToSpend,
                currencySymbol = state.currencySymbol,
                isCurrentMonth = state.isCurrentMonth,
            )
        }

        item {
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Income",
                        value = Money.format(state.incomeMinor, state.currencySymbol),
                        valueColor = moneyColors.income,
                        icon = Icons.Outlined.ArrowDownward,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Spent",
                        value = Money.format(state.expenseMinor, state.currencySymbol),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        icon = Icons.Outlined.ArrowUpward,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Net",
                        value = Money.format(state.netMinor, state.currencySymbol),
                        valueColor = if (state.netMinor < 0) moneyColors.expense else moneyColors.income,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.topCategories.isNotEmpty()) {
            item {
                SectionHeader(title = "Where it went", action = "Insights", onActionClick = onSeeInsights)
            }
            item {
                SectionCard {
                    val total = state.topCategories.sumOf { it.totalMinor }.coerceAtLeast(1)
                    state.topCategories.forEachIndexed { index, spend ->
                        CategoryShareRow(
                            spend = spend,
                            shareOfTotal = spend.totalMinor.toFloat() / total,
                            currencySymbol = state.currencySymbol,
                        )
                        if (index != state.topCategories.lastIndex) Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }

        if (state.goals.isNotEmpty()) {
            item {
                SectionHeader(title = "Savings goals", action = "All goals", onActionClick = onSeeGoals)
            }
            item {
                SectionCard {
                    state.goals.forEachIndexed { index, goal ->
                        GoalMiniRow(goal = goal, currencySymbol = state.currencySymbol)
                        if (index != state.goals.lastIndex) Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent activity",
                action = if (state.recent.isNotEmpty()) "See all" else null,
                onActionClick = if (state.recent.isNotEmpty()) onSeeAllTransactions else null,
            )
        }

        item {
            SectionCard {
                if (state.recent.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No transactions yet",
                        message = "Tap the + button to record your first expense, or paste a bank message to capture one instantly.",
                    )
                } else {
                    state.recent.forEachIndexed { index, item ->
                        TransactionRow(
                            item = item,
                            currencySymbol = state.currencySymbol,
                            onClick = { onOpenTransaction(item.transaction.id) },
                        )
                        if (index != state.recent.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The hero. This is the only place in the app that fills a whole card with the 30%
 * brand colour, which is what makes it read as *the* number on the screen.
 */
@Composable
private fun SafeToSpendCard(
    safeToSpend: SafeToSpend?,
    currencySymbol: String,
    isCurrentMonth: Boolean,
) {
    val accent = moneyColors.accentOnBrand
    val onBrand = MaterialTheme.colorScheme.onPrimary

    SectionCard(
        containerColor = MaterialTheme.colorScheme.primary,
        contentPadding = 20,
    ) {
        if (safeToSpend == null || !safeToSpend.hasPool) {
            Text(
                text = "Set up your month",
                style = MaterialTheme.typography.titleMedium,
                color = onBrand,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add your monthly income in Settings, or set category budgets, and SafeSpend will work out what you can safely spend each day.",
                style = MaterialTheme.typography.bodyMedium,
                color = onBrand.copy(alpha = 0.8f),
            )
            return@SectionCard
        }

        val headlineLabel = if (isCurrentMonth) "Safe to spend today" else "Left in this month"
        val headlineMinor =
            if (isCurrentMonth) safeToSpend.todayRemainingMinor else safeToSpend.remainingMinor
        val headlineColor = when {
            headlineMinor <= 0 -> accent
            safeToSpend.status == SafeToSpend.Status.TIGHT -> accent
            else -> onBrand
        }

        Text(
            text = headlineLabel,
            style = MaterialTheme.typography.labelLarge,
            color = onBrand.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Money.format(headlineMinor.coerceAtLeast(0), currencySymbol),
            style = MaterialTheme.typography.displaySmall,
            color = headlineColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = statusMessage(safeToSpend, currencySymbol, isCurrentMonth),
            style = MaterialTheme.typography.bodyMedium,
            color = onBrand.copy(alpha = 0.8f),
        )

        Spacer(Modifier.height(16.dp))

        ProgressTrack(
            progress = if (safeToSpend.poolMinor <= 0) 0f
            else (safeToSpend.spentMinor.toFloat() / safeToSpend.poolMinor),
            color = if (safeToSpend.status == SafeToSpend.Status.OVER) accent else onBrand,
            trackColor = onBrand.copy(alpha = 0.22f),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${Money.formatCompact(safeToSpend.spentMinor, currencySymbol)} spent",
                style = MaterialTheme.typography.bodySmall,
                color = onBrand.copy(alpha = 0.8f),
            )
            Text(
                text = "of ${Money.formatCompact(safeToSpend.poolMinor, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                color = onBrand.copy(alpha = 0.8f),
            )
        }
    }
}

private fun statusMessage(
    safeToSpend: SafeToSpend,
    currencySymbol: String,
    isCurrentMonth: Boolean,
): String {
    if (!isCurrentMonth) {
        return "${Money.format(safeToSpend.spentMinor, currencySymbol)} spent over the month."
    }
    val daily = Money.format(safeToSpend.dailyAllowanceMinor, currencySymbol)
    val days = safeToSpend.daysRemaining
    return when (safeToSpend.status) {
        SafeToSpend.Status.OVER ->
            "You've used the whole month's budget with $days ${dayWord(days)} still to go."
        SafeToSpend.Status.TIGHT ->
            "Today's $daily allowance is spent. Tomorrow resets — the rest of the month is still on track."
        SafeToSpend.Status.HEALTHY ->
            "$daily a day for the $days ${dayWord(days)} left."
        SafeToSpend.Status.UNSET -> ""
    }
}

private fun dayWord(days: Int) = if (days == 1) "day" else "days"

@Composable
private fun CategoryShareRow(
    spend: CategorySpend,
    shareOfTotal: Float,
    currencySymbol: String,
) {
    val color = moneyColors.chartSeries.atIndex(spend.categoryColor)
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryBadge(iconKey = spend.categoryIcon, tint = color, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = spend.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = Money.format(spend.totalMinor, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(progress = shareOfTotal, color = color, height = 6)
        }
    }
}

@Composable
private fun GoalMiniRow(goal: GoalEntity, currencySymbol: String) {
    val color = moneyColors.chartSeries.atIndex(goal.colorIndex)
    val progress = if (goal.targetMinor <= 0) 0f
    else (goal.savedMinor.toFloat() / goal.targetMinor).coerceIn(0f, 1f)

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "${Money.formatCompact(goal.savedMinor, currencySymbol)} / ${Money.formatCompact(goal.targetMinor, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressTrack(progress = progress, color = color, height = 6)
    }
}
