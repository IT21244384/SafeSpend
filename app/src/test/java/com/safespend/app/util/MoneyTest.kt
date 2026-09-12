package com.safespend.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Money is the one place a rounding mistake turns into a wrong balance, so these
 * tests pin down the boundaries rather than the happy path.
 */
class MoneyTest {

    @Test
    fun `parses a plain whole number into minor units`() {
        assertEquals(125000L, Money.parse("1250"))
    }

    @Test
    fun `parses decimals, thousands separators and a currency prefix`() {
        assertEquals(125050L, Money.parse("1,250.50"))
        assertEquals(125050L, Money.parse("Rs 1250.50"))
        assertEquals(125050L, Money.parse("Rs. 1,250.5"))
    }

    @Test
    fun `rounds a third decimal place half up instead of truncating`() {
        assertEquals(1235L, Money.parse("12.345"))
        assertEquals(1234L, Money.parse("12.344"))
    }

    @Test
    fun `rejects input that is not a usable amount`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("."))
    }

    @Test
    fun `rejects negative amounts because direction is the type, not the sign`() {
        assertNull(Money.parse("-500"))
    }

    @Test
    fun `formats with a grouped thousands separator and two decimals`() {
        assertEquals("Rs 1,250.50", Money.format(125050))
        assertEquals("Rs 0.00", Money.format(0))
    }

    @Test
    fun `keeps the minus sign in front of the currency symbol`() {
        assertEquals("-Rs 1,250.50", Money.format(-125050))
    }

    @Test
    fun `honours a caller supplied currency symbol`() {
        assertEquals("$ 99.00", Money.format(9900, "$"))
    }

    @Test
    fun `compact form abbreviates only once the number is long enough to need it`() {
        assertEquals("Rs 1,250", Money.formatCompact(125000))
        assertEquals("Rs 12.5K", Money.formatCompact(1250000))
        assertEquals("Rs 2M", Money.formatCompact(200000000))
    }

    @Test
    fun `round trips through formatPlain and parse without drift`() {
        val original = 987654L
        assertEquals(original, Money.parse(Money.formatPlain(original)))
    }
}
