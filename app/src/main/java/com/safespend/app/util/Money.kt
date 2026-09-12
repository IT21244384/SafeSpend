package com.safespend.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * All money in SafeSpend is a [Long] count of minor units (cents). This object is
 * the only place that converts to and from human-readable text, so rounding
 * happens exactly once per value instead of accumulating through the app.
 */
object Money {

    private const val MINOR_PER_MAJOR = 100L

    private val grouped: DecimalFormat = DecimalFormat(
        "#,##0.00",
        DecimalFormatSymbols(Locale.US),
    )

    private val groupedNoDecimals: DecimalFormat = DecimalFormat(
        "#,##0",
        DecimalFormatSymbols(Locale.US),
    )

    /** "Rs 12,450.00". Negative values keep the sign in front of the symbol. */
    fun format(minor: Long, symbol: String = "Rs"): String {
        val sign = if (minor < 0) "-" else ""
        val abs = Math.abs(minor)
        return "$sign$symbol ${grouped.format(abs / MINOR_PER_MAJOR.toDouble())}"
    }

    /** "Rs 12,450" — for chart axes and tight tiles where cents are noise. */
    fun formatCompact(minor: Long, symbol: String = "Rs"): String {
        val sign = if (minor < 0) "-" else ""
        val abs = Math.abs(minor)
        val major = abs / MINOR_PER_MAJOR
        return when {
            major >= 1_000_000 -> "$sign$symbol ${trim(major / 1_000_000.0)}M"
            major >= 10_000 -> "$sign$symbol ${trim(major / 1_000.0)}K"
            else -> "$sign$symbol ${groupedNoDecimals.format(major)}"
        }
    }

    /** Bare "12,450.00" with no symbol, for input fields and CSV export. */
    fun formatPlain(minor: Long): String = grouped.format(minor / MINOR_PER_MAJOR.toDouble())

    /**
     * Parses user input into minor units. Accepts "1250", "1,250.5", "Rs 1250.50",
     * "1 250.50". Returns null for anything that isn't a non-negative amount —
     * callers treat null as "don't save yet", not as zero.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .replace(Regex("[^0-9.,-]"), "")
            .replace(",", "")
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value
            .multiply(BigDecimal(MINOR_PER_MAJOR))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
