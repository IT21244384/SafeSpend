package com.safespend.app.domain

import com.safespend.app.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Bank alerts have no standard format, so the parser is tested against the shapes
 * real Sri Lankan banks actually send: symbol-first and amount-first, debit and
 * credit wording, and dates in three different orders.
 */
class SmsTransactionParserTest {

    @Test
    fun `reads a standard debit alert end to end`() {
        val parsed = SmsTransactionParser.parse(
            "Your A/C 1234 debited by LKR 2,500.00 on 12/09/2026 at KEELLS SUPER. Avl Bal LKR 45,000.00",
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(250_000L, parsed.amountMinor)
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals("Keells Super", parsed.merchant)
        assertEquals(LocalDate.of(2026, 9, 12), parsed.date)
        assertEquals("Groceries", parsed.suggestedCategory)
    }

    @Test
    fun `takes the transaction amount, not the balance that follows it`() {
        val parsed = SmsTransactionParser.parse(
            "Debited LKR 300.00 at CAFE. Avl Bal LKR 99,999.00",
        )

        assertEquals(30_000L, parsed?.amountMinor)
    }

    @Test
    fun `recognises a credit as income`() {
        val parsed = SmsTransactionParser.parse(
            "Credited LKR 85,000.00 to your account on 01-09-2026. SALARY for August.",
        )

        requireNotNull(parsed)
        assertEquals(TxType.INCOME, parsed.type)
        assertEquals(8_500_000L, parsed.amountMinor)
        assertEquals(LocalDate.of(2026, 9, 1), parsed.date)
        assertEquals("Salary", parsed.suggestedCategory)
    }

    @Test
    fun `handles the Rs dot prefix with no space`() {
        val parsed = SmsTransactionParser.parse(
            "Purchase of Rs.3,450.00 at PIZZA HUT on card ending 4412.",
        )

        requireNotNull(parsed)
        assertEquals(345_000L, parsed.amountMinor)
        assertEquals("Pizza Hut", parsed.merchant)
        assertEquals("Food & Dining", parsed.suggestedCategory)
    }

    @Test
    fun `handles an amount that comes before the currency code`() {
        val parsed = SmsTransactionParser.parse("2500 LKR withdrawn from ATM Colombo 07")

        requireNotNull(parsed)
        assertEquals(250_000L, parsed.amountMinor)
        assertEquals(TxType.EXPENSE, parsed.type)
    }

    @Test
    fun `returns null when there is no amount to work with`() {
        assertNull(SmsTransactionParser.parse("Your statement is ready to view."))
        assertNull(SmsTransactionParser.parse(""))
    }

    @Test
    fun `ignores a date-shaped string that is not a real date`() {
        val parsed = SmsTransactionParser.parse("debited LKR 100.00 on 32/13/2026")

        requireNotNull(parsed)
        assertEquals(10_000L, parsed.amountMinor)
        assertNull("An impossible date must not be guessed at", parsed.date)
    }

    @Test
    fun `defaults an unrecognised merchant to no category rather than a wrong one`() {
        val parsed = SmsTransactionParser.parse("Debited LKR 1,000.00 at ZZQQ TRADERS.")

        requireNotNull(parsed)
        assertEquals("Zzqq Traders", parsed.merchant)
        assertNull(parsed.suggestedCategory)
    }

    @Test
    fun `reads an ISO date`() {
        val parsed = SmsTransactionParser.parse("Debited LKR 500.00 on 2026-09-30 at SHOP.")

        assertEquals(LocalDate.of(2026, 9, 30), parsed?.date)
    }
}
