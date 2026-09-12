package com.safespend.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val INSIGHTS = "insights"
    const val GOALS = "goals"
    const val SETTINGS = "settings"

    /** 0 means "new transaction"; any other id opens that transaction for editing. */
    const val ENTRY = "entry/{txId}"

    fun entry(txId: Long = 0L) = "entry/$txId"
}

/**
 * The four destinations in the bottom bar. Goals and Settings deliberately stay
 * out of it: five tabs is where a bottom bar starts to read as a menu, and both
 * are reached often enough from Home.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS(Routes.TRANSACTIONS, "Ledger", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BUDGETS(Routes.BUDGETS, "Budgets", Icons.Filled.Savings, Icons.Outlined.Savings),
    INSIGHTS(Routes.INSIGHTS, "Insights", Icons.Filled.PieChart, Icons.Outlined.PieChart),
}
