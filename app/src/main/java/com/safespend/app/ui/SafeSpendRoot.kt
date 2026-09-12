package com.safespend.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safespend.app.ui.budgets.BudgetsScreen
import com.safespend.app.ui.budgets.BudgetsViewModel
import com.safespend.app.ui.entry.AddEditTransactionScreen
import com.safespend.app.ui.entry.AddEditTransactionViewModel
import com.safespend.app.ui.goals.GoalsScreen
import com.safespend.app.ui.goals.GoalsViewModel
import com.safespend.app.ui.home.HomeScreen
import com.safespend.app.ui.home.HomeViewModel
import com.safespend.app.ui.insights.InsightsScreen
import com.safespend.app.ui.insights.InsightsViewModel
import com.safespend.app.ui.navigation.Routes
import com.safespend.app.ui.navigation.TopLevelDestination
import com.safespend.app.ui.settings.SettingsScreen
import com.safespend.app.ui.settings.SettingsViewModel
import com.safespend.app.ui.transactions.TransactionsScreen
import com.safespend.app.ui.transactions.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeSpendRoot() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val onTopLevel = TopLevelDestination.entries.any { destination ->
        currentRoute?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // The bar slides away on full-screen destinations rather than vanishing,
            // so returning to a tab doesn't feel like a different app.
            AnimatedVisibility(
                visible = onTopLevel,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Tabs are peers: switching between them must not
                                    // stack up a back history five screens deep.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = onTopLevel,
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 },
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.entry()) },
                    // The 10% accent, on the one control that starts every task.
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(factory = SafeSpendViewModelFactory)
                HomeScreen(
                    viewModel = viewModel,
                    onAddTransaction = { navController.navigate(Routes.entry()) },
                    onOpenTransaction = { id -> navController.navigate(Routes.entry(id)) },
                    onSeeAllTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onSeeInsights = { navController.navigate(Routes.INSIGHTS) },
                    onSeeGoals = { navController.navigate(Routes.GOALS) },
                    onSeeSettings = { navController.navigate(Routes.SETTINGS) },
                    contentPadding = padding,
                )
            }

            composable(Routes.TRANSACTIONS) {
                val viewModel: TransactionsViewModel = viewModel(factory = SafeSpendViewModelFactory)
                TransactionsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onOpenTransaction = { id -> navController.navigate(Routes.entry(id)) },
                    contentPadding = padding,
                )
            }

            composable(Routes.BUDGETS) {
                val viewModel: BudgetsViewModel = viewModel(factory = SafeSpendViewModelFactory)
                BudgetsScreen(viewModel = viewModel, contentPadding = padding)
            }

            composable(Routes.INSIGHTS) {
                val viewModel: InsightsViewModel = viewModel(factory = SafeSpendViewModelFactory)
                InsightsScreen(viewModel = viewModel, contentPadding = padding)
            }

            composable(
                route = Routes.ENTRY,
                arguments = listOf(
                    navArgument("txId") {
                        type = NavType.StringType
                        defaultValue = "0"
                    },
                ),
            ) {
                val viewModel: AddEditTransactionViewModel = viewModel(factory = SafeSpendViewModelFactory)
                AddEditTransactionScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                )
            }

            composable(Routes.GOALS) {
                val viewModel: GoalsViewModel = viewModel(factory = SafeSpendViewModelFactory)
                GoalsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(factory = SafeSpendViewModelFactory)
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                )
            }
        }
    }
}
