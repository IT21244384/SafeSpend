package com.safespend.app.ui.entry

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safespend.app.data.local.SafeSpendDatabase
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.ui.theme.SafeSpendTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The same entry flow again, this time driven through the actual Compose tree:
 * typing into the field, tapping a category chip, pressing the button. It catches
 * the class of bug a ViewModel test cannot — a control wired to nothing.
 */
@RunWith(AndroidJUnit4::class)
class AddTransactionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: SafeSpendDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var preferences: UserPreferences

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = SafeSpendDatabase.inMemory(context)
        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            goalDao = database.goalDao(),
        )
        preferences = UserPreferences(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun launchScreen(onDone: () -> Unit = {}) {
        composeRule.setContent {
            val viewModel = remember {
                AddEditTransactionViewModel(
                    repository,
                    preferences,
                    SavedStateHandle(mapOf("txId" to "0")),
                )
            }
            SafeSpendTheme {
                AddEditTransactionScreen(viewModel = viewModel, onDone = onDone)
            }
        }
    }

    @Test
    fun theScreenOpensOnExpenseWithSaveDisabled() {
        launchScreen()

        composeRule.onNodeWithText("New transaction").assertExists()
        composeRule.onNodeWithText("Expense").assertExists()
        composeRule.onNodeWithTag(EntryTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun typingAnAmountAndPickingACategoryWritesTheTransaction() {
        var done = false
        launchScreen(onDone = { done = true })

        composeRule.onNodeWithTag(EntryTestTags.AMOUNT_FIELD).performTextInput("450")
        composeRule.onNodeWithText("Groceries").performScrollTo().performClick()

        composeRule.onNodeWithTag(EntryTestTags.SAVE_BUTTON).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag(EntryTestTags.SAVE_BUTTON).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { done }

        val saved = runBlocking { repository.observeRecent(10).first() }
        assertEquals(1, saved.size)
        assertEquals(45_000L, saved[0].transaction.amountMinor)
        assertEquals("Groceries", saved[0].categoryName)
    }

    @Test
    fun switchingToIncomeShowsIncomeCategories() {
        launchScreen()

        composeRule.onNodeWithText("Income").performClick()

        // The category list reloads from the database, so the assertion has to wait
        // for the query rather than for the next frame.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Salary").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Salary").assertExists()
    }
}
