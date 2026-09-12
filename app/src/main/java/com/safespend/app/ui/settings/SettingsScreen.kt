package com.safespend.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.prefs.ThemeMode
import com.safespend.app.ui.components.SectionCard
import com.safespend.app.ui.components.SectionHeader
import com.safespend.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    var incomeInput by remember { mutableStateOf("") }
    var savingsInput by remember { mutableStateOf("") }
    var symbolInput by remember { mutableStateOf("") }

    // Seed the fields once the stored values arrive, but never overwrite what the
    // user is mid-way through typing.
    LaunchedEffect(settings) {
        if (incomeInput.isEmpty() && settings.plannedIncomeMinor > 0) {
            incomeInput = Money.formatPlain(settings.plannedIncomeMinor)
        }
        if (savingsInput.isEmpty() && settings.savingsTargetMinor > 0) {
            savingsInput = Money.formatPlain(settings.savingsTargetMinor)
        }
        if (symbolInput.isEmpty()) symbolInput = settings.currencySymbol
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title = "Your month")

            SectionCard(contentPadding = 20) {
                Text(
                    text = "SafeSpend divides what's left of your month across the days that remain. It needs to know how much there is to divide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { value ->
                        incomeInput = value.filter { it.isDigit() || it == '.' }
                        Money.parse(incomeInput)?.let(viewModel::setPlannedIncome)
                    },
                    label = { Text("Expected monthly income") },
                    prefix = { Text(settings.currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = savingsInput,
                    onValueChange = { value ->
                        savingsInput = value.filter { it.isDigit() || it == '.' }
                        Money.parse(savingsInput)?.let(viewModel::setSavingsTarget)
                    },
                    label = { Text("Set aside for savings each month") },
                    prefix = { Text(settings.currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Savings come off the top, before anything is spendable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeader(title = "Savings goals", action = "Open", onActionClick = onOpenGoals)

            SectionHeader(title = "Display")

            SectionCard(contentPadding = 20) {
                OutlinedTextField(
                    value = symbolInput,
                    onValueChange = { value ->
                        symbolInput = value.take(4)
                        viewModel.setCurrencySymbol(symbolInput)
                    },
                    label = { Text("Currency symbol") },
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                    shape = MaterialTheme.shapes.small,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "System"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    },
                                )
                            },
                        )
                    }
                }
            }

            SectionHeader(title = "Data")

            SectionCard(contentPadding = 20) {
                Text(
                    text = "Everything SafeSpend records stays on this phone. There is no account, no server and no network permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { confirmClear = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Delete all transactions")
                }
            }

            SectionCard(contentPadding = 20) {
                Text(
                    text = "SafeSpend 1.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Personal finance and expense tracking, built with Kotlin and Jetpack Compose for SE4041 Mobile Application Design and Development.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all transactions?") },
            text = {
                Text("Every transaction will be removed. Your categories, budgets and goals stay. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTransactions()
                        confirmClear = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}
