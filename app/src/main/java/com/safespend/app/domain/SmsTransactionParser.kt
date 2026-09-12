package com.safespend.app.domain

import com.safespend.app.data.model.TxType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Turns a pasted bank alert into a draft transaction.
 *
 * Manual entry is where expense trackers die: it takes twenty seconds per purchase
 * and people stop after a week. Every Sri Lankan bank already sends an SMS for every
 * card transaction, so the fastest honest path is to let the user long-press that
 * message, paste it, and have the amount, merchant, direction and date filled in.
 *
 * This reads a string the user hands over. It does **not** ask for the READ_SMS
 * permission: that permission is restricted on Google Play, gives the app the user's
 * entire message history for a convenience feature, and is not worth it. Paste keeps
 * the user in control of exactly which message the app ever sees.
 */
object SmsTransactionParser {

    data class Parsed(
        val amountMinor: Long,
        val type: TxType,
        val merchant: String?,
        val date: LocalDate?,
        /** Category *name* to preselect, matched from the merchant. Null when unsure. */
        val suggestedCategory: String?,
    )

    // "LKR 2,500.00", "Rs.2500", "USD 19.99" — symbol first.
    private val CURRENCY_FIRST = Regex(
        """(?:LKR|Rs\.?|INR|USD|EUR|GBP)\s*([0-9][0-9,\s]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    // "2,500.00 LKR" — amount first.
    private val AMOUNT_FIRST = Regex(
        """([0-9][0-9,\s]*(?:\.[0-9]{1,2})?)\s*(?:LKR|Rs\.?|INR|USD|EUR|GBP)""",
        RegexOption.IGNORE_CASE,
    )

    private val CREDIT_WORDS = listOf(
        "credited", "credit", "received", "deposited", "deposit",
        "salary", "refund", "reversal", "cashback", "transferred to your",
    )

    private val DEBIT_WORDS = listOf(
        "debited", "debit", "purchase", "spent", "withdrawn", "withdrawal",
        "paid", "payment", "charged", "atm",
    )

    /** " at KEELLS SUPER on", " to JOHN PERERA." — merchant sits between a preposition and a stop. */
    private val MERCHANT = Regex(
        """\b(?:at|to|from)\s+([A-Za-z0-9&'./\- ]{2,40}?)(?=\s+(?:on|via|using|dated|ref|for)\b|[.,;\n]|$)""",
        RegexOption.IGNORE_CASE,
    )

    private val DATE_PATTERNS = listOf(
        Regex("""\b(\d{4}-\d{2}-\d{2})\b""") to "yyyy-MM-dd",
        Regex("""\b(\d{2}/\d{2}/\d{4})\b""") to "dd/MM/yyyy",
        Regex("""\b(\d{2}-\d{2}-\d{4})\b""") to "dd-MM-yyyy",
        Regex("""\b(\d{2}/\d{2}/\d{2})\b""") to "dd/MM/yy",
        Regex("""\b(\d{1,2} [A-Za-z]{3} \d{4})\b""") to "d MMM yyyy",
    )

    /**
     * Merchant keyword to category name. Deliberately small and obvious: a wrong
     * guess costs the user a tap to correct, so breadth matters more than cleverness.
     */
    private val CATEGORY_HINTS: List<Pair<List<String>, String>> = listOf(
        listOf("keells", "cargills", "arpico", "laugfs super", "supermarket", "glomark", "spar") to "Groceries",
        listOf("pizza", "kfc", "mcdonald", "burger", "restaurant", "cafe", "coffee", "bakery", "uber eats", "pickme food", "food city") to "Food & Dining",
        listOf("uber", "pickme", "taxi", "ceypetco", "ioc", "fuel", "petrol", "diesel", "bus", "railway", "parking") to "Transport",
        listOf("dialog", "mobitel", "hutch", "airtel", "slt", "ceb", "leco", "water board", "electricity", "broadband") to "Bills & Utilities",
        listOf("pharmacy", "hospital", "medical", "channel", "lab", "dental", "osu sala") to "Health",
        listOf("netflix", "spotify", "cinema", "scope", "savoy", "youtube premium", "disney") to "Entertainment",
        listOf("odel", "fashion", "house of", "nolimit", "mothercare", "daraz", "amazon", "aliexpress") to "Shopping",
        listOf("campus", "university", "institute", "tuition", "academy", "school") to "Education",
        listOf("rent", "landlord", "lease") to "Rent",
        listOf("salary", "payroll", "wages") to "Salary",
    )

    /**
     * Returns null when the text has no recognisable amount — the UI then keeps the
     * user in manual entry rather than silently filling in a wrong number.
     */
    fun parse(text: String): Parsed? {
        if (text.isBlank()) return null
        val amountMinor = extractAmount(text) ?: return null
        val lower = text.lowercase(Locale.ROOT)

        val type = when {
            CREDIT_WORDS.any { it in lower } && DEBIT_WORDS.none { it in lower } -> TxType.INCOME
            CREDIT_WORDS.any { it in lower } && firstIndexOf(lower, CREDIT_WORDS) < firstIndexOf(lower, DEBIT_WORDS) -> TxType.INCOME
            else -> TxType.EXPENSE
        }

        val merchant = extractMerchant(text)
        val date = extractDate(text)
        val category = suggestCategory(lower, merchant, type)

        return Parsed(
            amountMinor = amountMinor,
            type = type,
            merchant = merchant,
            date = date,
            suggestedCategory = category,
        )
    }

    private fun extractAmount(text: String): Long? {
        val raw = CURRENCY_FIRST.find(text)?.groupValues?.get(1)
            ?: AMOUNT_FIRST.find(text)?.groupValues?.get(1)
            ?: return null
        val normalised = raw.replace(",", "").replace(" ", "")
        val value = normalised.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    private fun extractMerchant(text: String): String? {
        val candidate = MERCHANT.find(text)?.groupValues?.get(1)?.trim() ?: return null
        // "at 14:32" and "to A/C 1234" are timestamps and account numbers, not shops.
        if (candidate.none { it.isLetter() }) return null
        if (candidate.length < 2) return null
        return candidate
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isUpperCase() }) word
                else word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
    }

    private fun extractDate(text: String): LocalDate? {
        for ((regex, pattern) in DATE_PATTERNS) {
            val match = regex.find(text)?.groupValues?.get(1) ?: continue
            try {
                return LocalDate.parse(match, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
            } catch (_: DateTimeParseException) {
                // Pattern matched the shape but not a real date (32/13/2026). Try the next.
            }
        }
        return null
    }

    private fun suggestCategory(lowerText: String, merchant: String?, type: TxType): String? {
        val haystack = (merchant?.lowercase(Locale.ROOT).orEmpty() + " " + lowerText)
        val hit = CATEGORY_HINTS.firstOrNull { (keywords, _) -> keywords.any { it in haystack } }?.second
        return when {
            hit != null -> hit
            type == TxType.INCOME -> "Other Income"
            else -> null
        }
    }

    private fun firstIndexOf(text: String, words: List<String>): Int =
        words.mapNotNull { word -> text.indexOf(word).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
}
