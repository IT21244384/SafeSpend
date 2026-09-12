package com.safespend.app.data.repository

import com.safespend.app.data.dao.BudgetDao
import com.safespend.app.data.dao.CategoryDao
import com.safespend.app.data.dao.GoalDao
import com.safespend.app.data.dao.TransactionDao
import com.safespend.app.data.entity.BudgetEntity
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.data.entity.TransactionEntity
import com.safespend.app.data.model.BudgetWithSpend
import com.safespend.app.data.model.CategorySpend
import com.safespend.app.data.model.DailyTotal
import com.safespend.app.data.model.PeriodTotals
import com.safespend.app.data.model.TransactionWithCategory
import com.safespend.app.data.model.TxType
import com.safespend.app.util.MonthPeriod
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The single door between the UI and storage. ViewModels never touch a DAO, so
 * swapping SQLite for anything else (or adding a sync layer later) is one file's
 * worth of change.
 */
class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
) {

    // ---- Categories -------------------------------------------------------

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeCategories(type: TxType): Flow<List<CategoryEntity>> = categoryDao.observeByType(type)

    suspend fun category(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    /**
     * Archives instead of deleting when history would break. Returns false if the
     * caller asked for a hard delete that the data won't allow.
     */
    suspend fun archiveCategory(id: Long) = categoryDao.archive(id)

    suspend fun categoryUsageCount(id: Long): Int = transactionDao.countForCategory(id)

    // ---- Transactions -----------------------------------------------------

    fun observeTransactions(period: MonthPeriod): Flow<List<TransactionWithCategory>> =
        transactionDao.observeBetween(period.start, period.end)

    fun observeRecent(limit: Int = 5): Flow<List<TransactionWithCategory>> =
        transactionDao.observeRecent(limit)

    fun observeFiltered(
        period: MonthPeriod,
        type: TxType?,
        categoryId: Long?,
        query: String,
    ): Flow<List<TransactionWithCategory>> =
        transactionDao.observeFiltered(period.start, period.end, type, categoryId, query)

    fun observeTotals(period: MonthPeriod): Flow<PeriodTotals> =
        transactionDao.observeTotals(period.start, period.end)

    fun observeDayTotals(date: LocalDate): Flow<PeriodTotals> =
        transactionDao.observeTotals(date, date)

    fun observeCategoryTotals(type: TxType, period: MonthPeriod): Flow<List<CategorySpend>> =
        transactionDao.observeCategoryTotals(type, period.start, period.end)

    fun observeDailyTotals(type: TxType, period: MonthPeriod): Flow<List<DailyTotal>> =
        transactionDao.observeDailyTotals(type, period.start, period.end)

    suspend fun transaction(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun addTransaction(transaction: TransactionEntity): Long = transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.delete(transaction)

    suspend fun deleteAllTransactions() = transactionDao.deleteAll()

    // ---- Budgets ----------------------------------------------------------

    fun observeBudgets(period: MonthPeriod): Flow<List<BudgetWithSpend>> =
        budgetDao.observeForMonth(period.key, period.start, period.end)

    fun observeTotalBudget(period: MonthPeriod): Flow<Long> = budgetDao.observeTotalBudget(period.key)

    suspend fun setBudget(categoryId: Long, period: MonthPeriod, limitMinor: Long) {
        val existing = budgetDao.find(categoryId, period.key)
        budgetDao.upsert(
            BudgetEntity(
                id = existing?.id ?: 0,
                categoryId = categoryId,
                periodMonth = period.key,
                limitMinor = limitMinor,
            ),
        )
    }

    suspend fun removeBudget(budgetId: Long) = budgetDao.delete(budgetId)

    suspend fun copyBudgets(from: MonthPeriod, to: MonthPeriod) =
        budgetDao.copyMonth(from.key, to.key)

    // ---- Goals ------------------------------------------------------------

    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun addGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    suspend fun contributeToGoal(id: Long, deltaMinor: Long) = goalDao.addToSaved(id, deltaMinor)
}
