package com.safespend.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.entity.TransactionEntity
import com.safespend.app.data.local.SafeSpendDatabase
import com.safespend.app.data.model.TxType
import com.safespend.app.data.repository.FinanceRepository
import com.safespend.app.util.MonthPeriod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * Integration tests: real Room, real SQLite, real SQL.
 *
 * The unit tests cover arithmetic in isolation; these cover the part that only
 * breaks once the pieces are wired together — the joins, the aggregate queries, the
 * seed callback and the foreign keys.
 */
@RunWith(AndroidJUnit4::class)
class FinanceRepositoryIntegrationTest {

    private lateinit var database: SafeSpendDatabase
    private lateinit var repository: FinanceRepository

    private val september = MonthPeriod(YearMonth.of(2026, 9))
    private val august = MonthPeriod(YearMonth.of(2026, 8))

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun categoriesAreSeededWhenTheDatabaseIsCreated() = runBlocking {
        val categories = repository.observeCategories().first()

        assertEquals(16, categories.size)
        assertTrue(categories.any { it.name == "Groceries" && it.type == TxType.EXPENSE })
        assertTrue(categories.any { it.name == "Salary" && it.type == TxType.INCOME })
    }

    @Test
    fun categoryPickerShowsOnlyTheTypeBeingEntered() = runBlocking {
        val expense = repository.observeCategories(TxType.EXPENSE).first()
        val income = repository.observeCategories(TxType.INCOME).first()

        assertTrue(expense.all { it.type == TxType.EXPENSE })
        assertTrue(income.all { it.type == TxType.INCOME })
        assertEquals(16, expense.size + income.size)
    }

    @Test
    fun savedTransactionComesBackJoinedToItsCategory() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.addTransaction(
            transaction(amount = 250_000, categoryId = groceries.id, date = LocalDate.of(2026, 9, 12)),
        )

        val rows = repository.observeTransactions(september).first()

        assertEquals(1, rows.size)
        assertEquals("Groceries", rows[0].categoryName)
        assertEquals("groceries", rows[0].categoryIcon)
        assertEquals(250_000L, rows[0].transaction.amountMinor)
    }

    @Test
    fun totalsSeparateIncomeFromExpenseWithinTheMonth() = runBlocking {
        val groceries = expenseCategory("Groceries")
        val salary = incomeCategory("Salary")

        repository.addTransaction(transaction(120_000, groceries.id, LocalDate.of(2026, 9, 3)))
        repository.addTransaction(transaction(80_000, groceries.id, LocalDate.of(2026, 9, 20)))
        repository.addTransaction(
            transaction(5_000_000, salary.id, LocalDate.of(2026, 9, 1), TxType.INCOME),
        )
        // Outside the month — must not be counted.
        repository.addTransaction(transaction(999_000, groceries.id, LocalDate.of(2026, 8, 31)))

        val totals = repository.observeTotals(september).first()

        assertEquals(5_000_000L, totals.incomeMinor)
        assertEquals(200_000L, totals.expenseMinor)
        assertEquals(4_800_000L, totals.netMinor)
    }

    @Test
    fun totalsAreZeroRatherThanNullForAnEmptyMonth() = runBlocking {
        val totals = repository.observeTotals(september).first()

        assertEquals(0L, totals.incomeMinor)
        assertEquals(0L, totals.expenseMinor)
    }

    @Test
    fun categoryTotalsAggregateAndSortByHeaviestSpend() = runBlocking {
        val groceries = expenseCategory("Groceries")
        val transport = expenseCategory("Transport")

        repository.addTransaction(transaction(50_000, groceries.id, LocalDate.of(2026, 9, 5)))
        repository.addTransaction(transaction(50_000, groceries.id, LocalDate.of(2026, 9, 6)))
        repository.addTransaction(transaction(70_000, transport.id, LocalDate.of(2026, 9, 7)))

        val totals = repository.observeCategoryTotals(TxType.EXPENSE, september).first()

        assertEquals(2, totals.size)
        assertEquals("Groceries", totals[0].categoryName)
        assertEquals(100_000L, totals[0].totalMinor)
        assertEquals(70_000L, totals[1].totalMinor)
    }

    @Test
    fun dailyTotalsGroupEveryTransactionOnADayIntoOneBar() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.addTransaction(transaction(10_000, groceries.id, LocalDate.of(2026, 9, 4)))
        repository.addTransaction(transaction(15_000, groceries.id, LocalDate.of(2026, 9, 4)))
        repository.addTransaction(transaction(20_000, groceries.id, LocalDate.of(2026, 9, 9)))

        val daily = repository.observeDailyTotals(TxType.EXPENSE, september).first()

        assertEquals(2, daily.size)
        assertEquals(LocalDate.of(2026, 9, 4), daily[0].date)
        assertEquals(25_000L, daily[0].totalMinor)
        assertEquals(20_000L, daily[1].totalMinor)
    }

    @Test
    fun searchMatchesNoteMerchantAndCategoryName() = runBlocking {
        val groceries = expenseCategory("Groceries")
        val transport = expenseCategory("Transport")
        repository.addTransaction(
            transaction(10_000, groceries.id, LocalDate.of(2026, 9, 4)).copy(merchant = "Keells Super"),
        )
        repository.addTransaction(
            transaction(20_000, transport.id, LocalDate.of(2026, 9, 5)).copy(note = "Trip to Kandy"),
        )

        val byMerchant = repository.observeFiltered(september, null, null, "keells").first()
        val byNote = repository.observeFiltered(september, null, null, "kandy").first()
        val byCategory = repository.observeFiltered(september, null, null, "Transport").first()
        val everything = repository.observeFiltered(september, null, null, "").first()

        assertEquals(1, byMerchant.size)
        assertEquals(1, byNote.size)
        assertEquals(1, byCategory.size)
        assertEquals(2, everything.size)
    }

    @Test
    fun typeAndCategoryFiltersNarrowTheLedger() = runBlocking {
        val groceries = expenseCategory("Groceries")
        val salary = incomeCategory("Salary")
        repository.addTransaction(transaction(10_000, groceries.id, LocalDate.of(2026, 9, 4)))
        repository.addTransaction(
            transaction(500_000, salary.id, LocalDate.of(2026, 9, 1), TxType.INCOME),
        )

        val expensesOnly = repository.observeFiltered(september, TxType.EXPENSE, null, "").first()
        val groceriesOnly = repository.observeFiltered(september, null, groceries.id, "").first()

        assertEquals(1, expensesOnly.size)
        assertEquals(TxType.EXPENSE, expensesOnly[0].transaction.type)
        assertEquals(1, groceriesOnly.size)
    }

    @Test
    fun budgetCountsSpendInItsOwnMonthOnly() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.setBudget(groceries.id, september, 300_000)
        repository.addTransaction(transaction(120_000, groceries.id, LocalDate.of(2026, 9, 10)))
        repository.addTransaction(transaction(999_000, groceries.id, LocalDate.of(2026, 8, 10)))

        val envelopes = repository.observeBudgets(september).first()

        assertEquals(1, envelopes.size)
        assertEquals(120_000L, envelopes[0].spentMinor)
        assertEquals(180_000L, envelopes[0].remainingMinor)
        assertFalse(envelopes[0].isOverBudget)
    }

    @Test
    fun anEnvelopeWithNoSpendStillAppearsAtZero() = runBlocking {
        val transport = expenseCategory("Transport")
        repository.setBudget(transport.id, september, 100_000)

        val envelopes = repository.observeBudgets(september).first()

        assertEquals(1, envelopes.size)
        assertEquals(0L, envelopes[0].spentMinor)
        assertEquals(0f, envelopes[0].progress, 0.0001f)
    }

    @Test
    fun incomeInABudgetedCategoryIsNotCountedAsSpending() = runBlocking {
        val gift = incomeCategory("Gift")
        repository.setBudget(gift.id, september, 100_000)
        repository.addTransaction(
            transaction(50_000, gift.id, LocalDate.of(2026, 9, 10), TxType.INCOME),
        )

        val envelopes = repository.observeBudgets(september).first()

        assertEquals(0L, envelopes[0].spentMinor)
    }

    @Test
    fun settingTheSameCategoryTwiceUpdatesRatherThanDuplicates() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.setBudget(groceries.id, september, 300_000)
        repository.setBudget(groceries.id, september, 450_000)

        val envelopes = repository.observeBudgets(september).first()

        assertEquals(1, envelopes.size)
        assertEquals(450_000L, envelopes[0].limitMinor)
    }

    @Test
    fun overspendingIsFlaggedAndTheBarStaysClamped() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.setBudget(groceries.id, september, 100_000)
        repository.addTransaction(transaction(350_000, groceries.id, LocalDate.of(2026, 9, 10)))

        val envelope = repository.observeBudgets(september).first().single()

        assertTrue(envelope.isOverBudget)
        assertEquals(-250_000L, envelope.remainingMinor)
        assertEquals(1f, envelope.progress, 0.0001f)
    }

    @Test
    fun budgetsCanBeCarriedForwardWithoutOverwritingWhatIsAlreadyThere() = runBlocking {
        val groceries = expenseCategory("Groceries")
        val transport = expenseCategory("Transport")
        repository.setBudget(groceries.id, august, 300_000)
        repository.setBudget(transport.id, august, 100_000)
        repository.setBudget(groceries.id, september, 500_000)

        repository.copyBudgets(from = august, to = september)

        val envelopes = repository.observeBudgets(september).first().associateBy { it.categoryName }

        assertEquals(2, envelopes.size)
        assertEquals("Existing September budget must survive the copy", 500_000L, envelopes["Groceries"]?.limitMinor)
        assertEquals(100_000L, envelopes["Transport"]?.limitMinor)
    }

    @Test
    fun archivedCategoryLeavesItsHistoryReadable() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.addTransaction(transaction(10_000, groceries.id, LocalDate.of(2026, 9, 4)))

        repository.archiveCategory(groceries.id)

        val pickable = repository.observeCategories().first()
        val ledger = repository.observeTransactions(september).first()

        assertFalse("Archived categories disappear from the picker", pickable.any { it.id == groceries.id })
        assertEquals("but the transaction still reads back", "Groceries", ledger.single().categoryName)
    }

    @Test
    fun goalContributionsAccumulateAndCannotGoNegative() = runBlocking {
        val id = repository.addGoal(
            com.safespend.app.data.entity.GoalEntity(name = "Laptop", targetMinor = 2_000_000),
        )

        repository.contributeToGoal(id, 500_000)
        repository.contributeToGoal(id, 250_000)
        repository.contributeToGoal(id, -10_000_000)

        val goal = repository.observeGoals().first().single()

        assertEquals(0L, goal.savedMinor)
    }

    @Test
    fun deletingAllTransactionsLeavesCategoriesAndBudgetsIntact() = runBlocking {
        val groceries = expenseCategory("Groceries")
        repository.setBudget(groceries.id, september, 300_000)
        repository.addTransaction(transaction(10_000, groceries.id, LocalDate.of(2026, 9, 4)))

        repository.deleteAllTransactions()

        assertTrue(repository.observeTransactions(september).first().isEmpty())
        assertEquals(16, repository.observeCategories().first().size)
        assertEquals(1, repository.observeBudgets(september).first().size)
    }

    // ---- helpers ----------------------------------------------------------

    private suspend fun expenseCategory(name: String): CategoryEntity =
        repository.observeCategories(TxType.EXPENSE).first().first { it.name == name }

    private suspend fun incomeCategory(name: String): CategoryEntity =
        repository.observeCategories(TxType.INCOME).first().first { it.name == name }

    private fun transaction(
        amount: Long,
        categoryId: Long,
        date: LocalDate,
        type: TxType = TxType.EXPENSE,
    ) = TransactionEntity(
        amountMinor = amount,
        type = type,
        categoryId = categoryId,
        date = date,
    )
}
