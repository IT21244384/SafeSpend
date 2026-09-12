package com.safespend.app.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safespend.app.data.local.SafeSpendDatabase
import com.safespend.app.data.model.TxSource
import com.safespend.app.data.model.TxType
import com.safespend.app.data.prefs.UserPreferences
import com.safespend.app.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Integration test across the full entry path: ViewModel → repository → Room.
 *
 * This is the flow most of the app's value passes through, and the one where a
 * mistake is invisible in unit tests — the parser can be perfect and the row can
 * still be written with the wrong category or type.
 */
@RunWith(AndroidJUnit4::class)
class AddEditTransactionViewModelTest {

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

    private suspend fun newViewModel(txId: String = "0") = withContext(Dispatchers.Main) {
        AddEditTransactionViewModel(
            repository,
            preferences,
            SavedStateHandle(mapOf("txId" to txId)),
        )
    }

    @Test
    fun cannotSaveUntilThereIsAnAmountAndACategory() = runBlocking {
        val viewModel = newViewModel()
        val ready = viewModel.state.first { it.categories.isNotEmpty() }
        assertFalse(ready.canSave)

        withContext(Dispatchers.Main) { viewModel.setAmount("500") }
        assertFalse("amount alone is not enough", viewModel.state.value.canSave)

        withContext(Dispatchers.Main) { viewModel.selectCategory(ready.categories.first().id) }
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun amountInputRejectsLettersAndKeepsTwoDecimalPlaces() = runBlocking {
        val viewModel = newViewModel()
        viewModel.state.first { it.categories.isNotEmpty() }

        withContext(Dispatchers.Main) { viewModel.setAmount("1a2b3.4567") }

        assertEquals("123.45", viewModel.state.value.amountInput)
    }

    @Test
    fun switchingTypeSwapsTheCategoryListAndDropsAnInvalidSelection() = runBlocking {
        val viewModel = newViewModel()
        val expenseState = viewModel.state.first { it.categories.isNotEmpty() }
        withContext(Dispatchers.Main) { viewModel.selectCategory(expenseState.categories.first().id) }

        withContext(Dispatchers.Main) { viewModel.setType(TxType.INCOME) }
        val incomeState = viewModel.state.first { it.type == TxType.INCOME && it.categories.isNotEmpty() }

        assertTrue(incomeState.categories.all { it.type == TxType.INCOME })
        assertEquals(
            "An expense category must not survive a switch to Income",
            null,
            incomeState.selectedCategoryId,
        )
    }

    @Test
    fun pastedBankMessageFillsTheFormAndSavesAsAnImport() = runBlocking {
        val viewModel = newViewModel()
        viewModel.state.first { it.categories.isNotEmpty() }

        withContext(Dispatchers.Main) {
            viewModel.setSmsInput(
                "Your A/C 1234 debited by LKR 2,500.00 on 12/09/2026 at KEELLS SUPER. Avl Bal LKR 45,000.00",
            )
            viewModel.applySms()
        }

        val filled = viewModel.state.first { it.smsApplied }
        assertEquals("2,500.00", filled.amountInput)
        assertEquals(TxType.EXPENSE, filled.type)
        assertEquals("Keells Super", filled.merchant)
        assertEquals(LocalDate.of(2026, 9, 12), filled.date)
        assertNotNull("The Groceries category should have been matched", filled.selectedCategoryId)
        assertTrue(filled.canSave)

        withContext(Dispatchers.Main) { viewModel.save() }
        viewModel.state.first { it.finished }

        val saved = repository.observeRecent(10).first().single()
        assertEquals(250_000L, saved.transaction.amountMinor)
        assertEquals("Groceries", saved.categoryName)
        assertEquals(TxSource.SMS_IMPORT, saved.transaction.source)
    }

    @Test
    fun unparseableMessageReportsAnErrorAndChangesNothing() = runBlocking {
        val viewModel = newViewModel()
        viewModel.state.first { it.categories.isNotEmpty() }

        withContext(Dispatchers.Main) {
            viewModel.openSmsSheet()
            viewModel.setSmsInput("Your statement is ready to view.")
            viewModel.applySms()
        }

        val state = viewModel.state.value
        assertNotNull(state.smsError)
        assertEquals("", state.amountInput)
        assertTrue("The sheet stays open so the user can correct it", state.smsSheetOpen)
    }

    @Test
    fun editingAnExistingTransactionLoadsItAndUpdatesInPlace() = runBlocking {
        val groceries = repository.observeCategories(TxType.EXPENSE).first().first { it.name == "Groceries" }
        val id = repository.addTransaction(
            com.safespend.app.data.entity.TransactionEntity(
                amountMinor = 100_000,
                type = TxType.EXPENSE,
                categoryId = groceries.id,
                date = LocalDate.of(2026, 9, 12),
                note = "original",
            ),
        )

        val viewModel = newViewModel(txId = id.toString())
        val loaded = viewModel.state.first { it.amountInput.isNotEmpty() }
        assertTrue(loaded.isEditing)
        assertEquals("1,000.00", loaded.amountInput)
        assertEquals("original", loaded.note)

        withContext(Dispatchers.Main) {
            viewModel.setAmount("1500")
            viewModel.setNote("corrected")
            viewModel.save()
        }
        viewModel.state.first { it.finished }

        val rows = repository.observeRecent(10).first()
        assertEquals("Editing must not create a second row", 1, rows.size)
        assertEquals(150_000L, rows[0].transaction.amountMinor)
        assertEquals("corrected", rows[0].transaction.note)
    }

    @Test
    fun deletingFromTheEditScreenRemovesTheRow() = runBlocking {
        val groceries = repository.observeCategories(TxType.EXPENSE).first().first { it.name == "Groceries" }
        val id = repository.addTransaction(
            com.safespend.app.data.entity.TransactionEntity(
                amountMinor = 100_000,
                type = TxType.EXPENSE,
                categoryId = groceries.id,
                date = LocalDate.of(2026, 9, 12),
            ),
        )

        val viewModel = newViewModel(txId = id.toString())
        viewModel.state.first { it.amountInput.isNotEmpty() }
        withContext(Dispatchers.Main) { viewModel.delete() }
        viewModel.state.first { it.finished }

        assertTrue(repository.observeRecent(10).first().isEmpty())
    }
}
