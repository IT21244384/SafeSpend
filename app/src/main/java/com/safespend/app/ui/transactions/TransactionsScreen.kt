package com.safespend.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.model.TxType
import com.safespend.app.ui.components.EmptyState
import com.safespend.app.ui.components.MonthSwitcher
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.TransactionRow
import com.safespend.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenTransaction: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
                Text(
                    text = "Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                MonthSwitcher(
                    label = state.monthLabel.substringBefore(" "),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    nextEnabled = state.canGoNext,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes, shops or categories") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.typeFilter == TxType.EXPENSE,
                    onClick = {
                        viewModel.setTypeFilter(
                            if (state.typeFilter == TxType.EXPENSE) null else TxType.EXPENSE,
                        )
                    },
                    label = { Text("Expenses") },
                )
                FilterChip(
                    selected = state.typeFilter == TxType.INCOME,
                    onClick = {
                        viewModel.setTypeFilter(
                            if (state.typeFilter == TxType.INCOME) null else TxType.INCOME,
                        )
                    },
                    label = { Text("Income") },
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.categoryFilter == category.id,
                        onClick = {
                            viewModel.setCategoryFilter(
                                if (state.categoryFilter == category.id) null else category.id,
                            )
                        },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }

        if (state.hasFilters) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.matchCount} ${if (state.matchCount == 1) "match" else "matches"} · " +
                            "${Money.format(state.matchTotalMinor, state.currencySymbol)} spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::clearFilters) {
                        Text("Clear", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (state.groups.isEmpty()) {
            item {
                SectionCard {
                    EmptyState(
                        icon = if (state.hasFilters) Icons.Outlined.SearchOff else Icons.Outlined.DeleteOutline,
                        title = if (state.hasFilters) "Nothing matches" else "No transactions this month",
                        message = if (state.hasFilters) {
                            "Try a different search term, or clear the filters."
                        } else {
                            "Move to another month with the arrows above, or add a transaction with the + button."
                        },
                    )
                }
            }
        }

        items(state.groups, key = { it.date.toEpochDay() }) { group ->
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(group.netMinor, state.currencySymbol),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SectionCard(contentPadding = 12) {
                    group.items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TransactionRow(
                                item = item,
                                currencySymbol = state.currencySymbol,
                                showDate = false,
                                onClick = { onOpenTransaction(item.transaction.id) },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    viewModel.delete(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Transaction deleted",
                                            actionLabel = "Undo",
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete ${item.categoryName} transaction",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}
