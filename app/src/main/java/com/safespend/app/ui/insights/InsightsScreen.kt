package com.safespend.app.ui.insights

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.InsertChartOutlined
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
import com.safespend.app.ui.components.BarChart
import com.safespend.app.ui.components.ChartSlice
import com.safespend.app.ui.components.DonutChart
import com.safespend.app.ui.components.EmptyState
import com.safespend.app.ui.components.MonthSwitcher
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.SectionHeader
import com.safespend.app.ui.components.StatTile
import com.safespend.app.ui.components.atIndex
import com.safespend.app.ui.theme.moneyColors
import com.safespend.app.util.Money
import com.safespend.app.util.displayLabel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = moneyColors.chartSeries

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
                        text = "Insights",
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

        if (!state.hasData) {
            item {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.InsertChartOutlined,
                        title = "Nothing to chart yet",
                        message = "Once you've recorded a few expenses this month, the breakdown and daily pattern will appear here.",
                    )
                }
            }
            return@LazyColumn
        }

        item {
            SectionCard(contentPadding = 20) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutChart(
                        slices = state.categories.map {
                            ChartSlice(it.categoryName, it.totalMinor, palette.atIndex(it.categoryColor))
                        },
                        centerLabel = "Spent",
                        centerValue = Money.formatCompact(state.totals.expenseMinor, state.currencySymbol),
                    )
                }

                Spacer(Modifier.height(18.dp))

                val total = state.categories.sumOf { it.totalMinor }.coerceAtLeast(1)
                state.categories.forEach { spend ->
                    LegendRow(
                        color = palette.atIndex(spend.categoryColor),
                        label = spend.categoryName,
                        value = Money.format(spend.totalMinor, state.currencySymbol),
                        share = (spend.totalMinor * 100.0 / total).roundToInt(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        item { SectionHeader(title = "Daily pattern") }

        item {
            SectionCard(contentPadding = 20) {
                // Every day of the month appears, including the empty ones — gaps in
                // spending are information, and a chart that skips them misleads.
                val byDay = state.dailySpend.associateBy { it.date.dayOfMonth }
                val slices = (1..state.monthLengthDays).map { day ->
                    ChartSlice(
                        label = day.toString(),
                        value = byDay[day]?.totalMinor ?: 0L,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                BarChart(slices = slices)

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Average / active day",
                        value = Money.formatCompact(state.averagePerActiveDay, state.currencySymbol),
                        modifier = Modifier.weight(1f),
                    )
                    state.busiestDay?.let { busiest ->
                        StatTile(
                            label = "Heaviest day",
                            value = Money.formatCompact(busiest.totalMinor, state.currencySymbol),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                state.busiestDay?.let { busiest ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Heaviest day was ${busiest.date.displayLabel()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeader(title = "Compared with last month") }

        item {
            SectionCard(contentPadding = 20) {
                val change = state.changeVsLastMonth
                if (change == null) {
                    Text(
                        text = "No spending recorded last month, so there's nothing to compare against yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val percent = (abs(change) * 100).roundToInt()
                    val up = change > 0
                    Text(
                        text = when {
                            percent == 0 -> "Spending is level with last month."
                            up -> "You're spending $percent% more than last month."
                            else -> "You're spending $percent% less than last month."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (up) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "This month",
                            value = Money.format(state.totals.expenseMinor, state.currencySymbol),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Last month",
                            value = Money.format(state.previousMonthExpenseMinor, state.currencySymbol),
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String, share: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$share%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
