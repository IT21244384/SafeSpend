package com.safespend.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthPeriodTest {

    private val september = MonthPeriod(YearMonth.of(2026, 9))

    @Test
    fun `spans the whole month inclusive`() {
        assertEquals(LocalDate.of(2026, 9, 1), september.start)
        assertEquals(LocalDate.of(2026, 9, 30), september.end)
        assertEquals(30, september.lengthInDays)
    }

    @Test
    fun `key matches the format budgets are stored under`() {
        assertEquals("2026-09", september.key)
        assertEquals(september, MonthPeriod.of("2026-09"))
    }

    @Test
    fun `counts today as a day still available to spend`() {
        assertEquals(20, september.daysRemaining(today = LocalDate.of(2026, 9, 11)))
        assertEquals(1, september.daysRemaining(today = LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun `a finished month has no days remaining`() {
        assertEquals(0, september.daysRemaining(today = LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `a future month has all of its days remaining`() {
        assertEquals(30, september.daysRemaining(today = LocalDate.of(2026, 8, 15)))
    }

    @Test
    fun `knows which dates belong to it`() {
        assertTrue(september.contains(LocalDate.of(2026, 9, 30)))
        assertFalse(september.contains(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `steps across a year boundary correctly`() {
        val january = MonthPeriod(YearMonth.of(2026, 1))

        assertEquals("2025-12", january.previous().key)
        assertEquals("2026-02", january.next().key)
    }

    @Test
    fun `labels dates relative to today where that reads better`() {
        val today = LocalDate.of(2026, 9, 12)

        assertEquals("Today", today.relativeLabel(today))
        assertEquals("Yesterday", today.minusDays(1).relativeLabel(today))
        assertEquals("5 Sep", today.minusDays(7).relativeLabel(today))
    }

    @Test
    fun `includes the year once a date is outside the current one`() {
        val today = LocalDate.of(2026, 9, 12)

        assertEquals("12 Sep 2025", LocalDate.of(2025, 9, 12).displayLabel(today))
    }
}
