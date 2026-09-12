package com.safespend.app.domain

import java.time.LocalDate

/**
 * The number the whole app is built around.
 *
 * A conventional expense tracker tells you what you already spent, which is true
 * and useless — the money is gone. SafeSpend answers the question people actually
 * ask standing at a till: *can I afford this right now?*
 *
 * It does that by spreading what's left of the month's spendable pool across the
 * days that are left, so the allowance self-corrects: overspend on Monday and
 * Tuesday through Sunday each quietly shrink, rather than the user discovering
 * on the 28th that the month is gone.
 */
data class SafeToSpend(
    /** What the user has to spend this month in total. */
    val poolMinor: Long,
    /** Spent so far this month. */
    val spentMinor: Long,
    /** Pool minus spend. Can be negative. */
    val remainingMinor: Long,
    /** Days left in the month, counting today. */
    val daysRemaining: Int,
    /** Remaining spread evenly across the days left. */
    val dailyAllowanceMinor: Long,
    /** Spent today only. */
    val spentTodayMinor: Long,
    /** Today's allowance minus today's spend — the headline figure. */
    val todayRemainingMinor: Long,
    /**
     * Where the month ends up if the current daily pace continues. This is what
     * turns the card from a scoreboard into a warning.
     */
    val projectedSpendMinor: Long,
    val status: Status,
) {
    enum class Status {
        /** On pace, with today's allowance intact. */
        HEALTHY,

        /** Today's allowance is used up, or the current pace overshoots the month. */
        TIGHT,

        /** The month's pool is already spent. */
        OVER,

        /** No income and no budgets recorded yet — there is nothing to divide. */
        UNSET,
    }

    val hasPool: Boolean get() = status != Status.UNSET
}

object SafeToSpendCalculator {

    /**
     * @param plannedIncomeMinor what the user told Settings they expect to earn each
     *   month. Zero means "not set".
     * @param actualIncomeMinor income actually recorded this month. Used when no
     *   planned figure exists, so the card works for irregular earners too.
     * @param budgetedMinor the sum of the month's envelopes. When the user has set
     *   envelopes these take priority — they are a deliberate statement about
     *   spending, where income is only a ceiling.
     * @param savingsTargetMinor money set aside before anything is spendable.
     */
    fun calculate(
        plannedIncomeMinor: Long,
        actualIncomeMinor: Long,
        budgetedMinor: Long,
        savingsTargetMinor: Long,
        spentMinor: Long,
        spentTodayMinor: Long,
        monthLengthDays: Int,
        daysRemaining: Int,
        dayOfMonth: Int,
    ): SafeToSpend {
        val incomeBasis = if (plannedIncomeMinor > 0) plannedIncomeMinor else actualIncomeMinor
        val fromIncome = (incomeBasis - savingsTargetMinor).coerceAtLeast(0)

        // Envelopes win when they exist: they are what the user *decided* to spend.
        val pool = if (budgetedMinor > 0) budgetedMinor else fromIncome

        if (pool <= 0) {
            return SafeToSpend(
                poolMinor = 0,
                spentMinor = spentMinor,
                remainingMinor = -spentMinor,
                daysRemaining = daysRemaining,
                dailyAllowanceMinor = 0,
                spentTodayMinor = spentTodayMinor,
                todayRemainingMinor = 0,
                projectedSpendMinor = projectPace(spentMinor, dayOfMonth, monthLengthDays),
                status = SafeToSpend.Status.UNSET,
            )
        }

        val remaining = pool - spentMinor
        // A finished month has no days left; showing "divide by 0" or a full
        // allowance would both be lies, so the allowance collapses to what's left.
        val safeDays = daysRemaining.coerceAtLeast(1)
        val dailyAllowance = if (remaining > 0) remaining / safeDays else 0L
        val todayRemaining = dailyAllowance - spentTodayMinor
        val projected = projectPace(spentMinor, dayOfMonth, monthLengthDays)

        val status = when {
            remaining <= 0 -> SafeToSpend.Status.OVER
            todayRemaining <= 0 || projected > pool -> SafeToSpend.Status.TIGHT
            else -> SafeToSpend.Status.HEALTHY
        }

        return SafeToSpend(
            poolMinor = pool,
            spentMinor = spentMinor,
            remainingMinor = remaining,
            daysRemaining = daysRemaining,
            dailyAllowanceMinor = dailyAllowance,
            spentTodayMinor = spentTodayMinor,
            todayRemainingMinor = todayRemaining,
            projectedSpendMinor = projected,
            status = status,
        )
    }

    /** Straight-line projection from the average daily spend so far. */
    private fun projectPace(spentMinor: Long, dayOfMonth: Int, monthLengthDays: Int): Long {
        if (dayOfMonth <= 0 || spentMinor <= 0) return 0
        val elapsed = dayOfMonth.coerceAtMost(monthLengthDays)
        return spentMinor * monthLengthDays / elapsed
    }

    /**
     * Convenience overload for the common "viewing the current month" case.
     * Kept separate so the pure function above stays free of `LocalDate.now()`
     * and can be unit tested without a clock.
     */
    fun forMonth(
        today: LocalDate,
        monthLengthDays: Int,
        daysRemaining: Int,
        plannedIncomeMinor: Long,
        actualIncomeMinor: Long,
        budgetedMinor: Long,
        savingsTargetMinor: Long,
        spentMinor: Long,
        spentTodayMinor: Long,
    ): SafeToSpend = calculate(
        plannedIncomeMinor = plannedIncomeMinor,
        actualIncomeMinor = actualIncomeMinor,
        budgetedMinor = budgetedMinor,
        savingsTargetMinor = savingsTargetMinor,
        spentMinor = spentMinor,
        spentTodayMinor = spentTodayMinor,
        monthLengthDays = monthLengthDays,
        daysRemaining = daysRemaining,
        dayOfMonth = today.dayOfMonth,
    )
}
