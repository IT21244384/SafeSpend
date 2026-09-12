package com.safespend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The safe-to-spend figure is the app's headline claim, so its arithmetic is tested
 * as a pure function with no clock, no database and no Android framework involved.
 */
class SafeToSpendCalculatorTest {

    private fun calculate(
        plannedIncome: Long = 0,
        actualIncome: Long = 0,
        budgeted: Long = 0,
        savings: Long = 0,
        spent: Long = 0,
        spentToday: Long = 0,
        monthLength: Int = 30,
        daysRemaining: Int = 20,
        dayOfMonth: Int = 11,
    ) = SafeToSpendCalculator.calculate(
        plannedIncomeMinor = plannedIncome,
        actualIncomeMinor = actualIncome,
        budgetedMinor = budgeted,
        savingsTargetMinor = savings,
        spentMinor = spent,
        spentTodayMinor = spentToday,
        monthLengthDays = monthLength,
        daysRemaining = daysRemaining,
        dayOfMonth = dayOfMonth,
    )

    @Test
    fun `reports UNSET when there is no income and no budget to divide`() {
        val result = calculate(spent = 5_000)

        assertEquals(SafeToSpend.Status.UNSET, result.status)
        assertEquals(0L, result.poolMinor)
        assertEquals(0L, result.dailyAllowanceMinor)
    }

    @Test
    fun `uses planned income minus savings when no budgets are set`() {
        val result = calculate(plannedIncome = 100_000, savings = 20_000)

        assertEquals(80_000L, result.poolMinor)
    }

    @Test
    fun `falls back to actual recorded income when no planned income exists`() {
        val result = calculate(actualIncome = 60_000)

        assertEquals(60_000L, result.poolMinor)
    }

    @Test
    fun `budget envelopes take priority over income`() {
        val result = calculate(plannedIncome = 100_000, budgeted = 45_000)

        assertEquals(
            "Envelopes are a deliberate spending decision; income is only a ceiling",
            45_000L,
            result.poolMinor,
        )
    }

    @Test
    fun `savings target cannot push the pool below zero`() {
        val result = calculate(plannedIncome = 10_000, savings = 50_000)

        assertEquals(SafeToSpend.Status.UNSET, result.status)
        assertEquals(0L, result.poolMinor)
    }

    @Test
    fun `spreads what is left across the days that are left`() {
        val result = calculate(budgeted = 30_000, spent = 10_000, daysRemaining = 20)

        assertEquals(20_000L, result.remainingMinor)
        assertEquals(1_000L, result.dailyAllowanceMinor)
    }

    @Test
    fun `allowance shrinks after overspending rather than staying flat`() {
        val steady = calculate(budgeted = 30_000, spent = 10_000, daysRemaining = 20)
        val overspent = calculate(budgeted = 30_000, spent = 18_000, daysRemaining = 20)

        assertTrue(
            "Spending more must reduce the remaining daily allowance",
            overspent.dailyAllowanceMinor < steady.dailyAllowanceMinor,
        )
    }

    @Test
    fun `is HEALTHY when on pace with today's allowance intact`() {
        val result = calculate(
            budgeted = 30_000,
            spent = 5_000,
            spentToday = 200,
            daysRemaining = 21,
            dayOfMonth = 10,
        )

        assertEquals(SafeToSpend.Status.HEALTHY, result.status)
        assertTrue(result.todayRemainingMinor > 0)
    }

    @Test
    fun `is TIGHT once today's allowance is used up, even with the month on track`() {
        val result = calculate(
            budgeted = 30_000,
            spent = 1_000,
            spentToday = 1_000,
            daysRemaining = 30,
            dayOfMonth = 1,
        )

        assertEquals(SafeToSpend.Status.TIGHT, result.status)
        assertTrue(result.remainingMinor > 0)
        assertTrue(result.todayRemainingMinor <= 0)
    }

    @Test
    fun `is TIGHT when the current pace would overshoot the month`() {
        val result = calculate(
            budgeted = 30_000,
            spent = 15_000,
            spentToday = 0,
            monthLength = 30,
            daysRemaining = 26,
            dayOfMonth = 5,
        )

        assertEquals(SafeToSpend.Status.TIGHT, result.status)
        assertEquals(90_000L, result.projectedSpendMinor)
    }

    @Test
    fun `is OVER once the pool is spent`() {
        val result = calculate(budgeted = 30_000, spent = 31_000)

        assertEquals(SafeToSpend.Status.OVER, result.status)
        assertEquals(0L, result.dailyAllowanceMinor)
        assertTrue(result.remainingMinor < 0)
    }

    @Test
    fun `does not divide by zero on the last day of the month`() {
        val result = calculate(budgeted = 30_000, spent = 10_000, daysRemaining = 0, dayOfMonth = 30)

        assertEquals(20_000L, result.dailyAllowanceMinor)
    }

    @Test
    fun `projection is zero before anything has been spent`() {
        val result = calculate(budgeted = 30_000, spent = 0, dayOfMonth = 15)

        assertEquals(0L, result.projectedSpendMinor)
    }
}
