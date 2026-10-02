package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Strict integer-based financial utility.
 * All monetary amounts are stored and calculated in minor units (paisa/cents: 1 unit = 100 paisa).
 * No floating-point (Float/Double) arithmetic is ever used for money calculations.
 */
object MoneyUtils {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = banglaDigits.indexOf(ch)
            if (idx >= 0) {
                sb.append(('0'.code + idx).toChar())
            } else if (ch != ',' && ch != ' ' && ch != '৳' && ch != '$') {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(number: Int, lang: String): String =
        if (lang == "bn") toBanglaDigits(number.toString()) else number.toString()

    fun formatNumber(number: Long, lang: String): String =
        if (lang == "bn") toBanglaDigits(number.toString()) else number.toString()

    /**
     * Parses a user-entered string into exact minor units (paisa).
     * Returns null if invalid or negative, or 0L if zero.
     */
    fun parseToPaisa(rawInput: String): Long? {
        val cleaned = normalizeDigits(rawInput)
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd < BigDecimal.ZERO) return null
            val scaled = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            scaled.longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts minor units (paisa) back into an editable plain number string (e.g. "500" or "500.50").
     */
    fun paisaToEditableString(amountPaisa: Long): String {
        val absPaisa = abs(amountPaisa)
        val whole = absPaisa / 100L
        val fraction = absPaisa % 100L
        return if (fraction == 0L) {
            whole.toString()
        } else {
            String.format(Locale.US, "%d.%02d", whole, fraction).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Formats minor units (paisa) with currency symbol and grouping commas.
     */
    fun formatPaisa(
        amountPaisa: Long,
        currencySymbol: String = "৳",
        useBanglaDigits: Boolean = false,
        showPlusSign: Boolean = false
    ): String {
        val isNegative = amountPaisa < 0L
        val absPaisa = abs(amountPaisa)
        val whole = absPaisa / 100L
        val fraction = absPaisa % 100L

        val formattedWhole = formatWithCommas(whole)
        val numberPart = if (fraction == 0L) {
            formattedWhole
        } else {
            String.format(Locale.US, "%s.%02d", formattedWhole, fraction)
        }

        val localizedNumber = if (useBanglaDigits) toBanglaDigits(numberPart) else numberPart
        val signPrefix = when {
            isNegative -> "−"
            showPlusSign && amountPaisa > 0L -> "+"
            else -> ""
        }
        return "$signPrefix$currencySymbol$localizedNumber"
    }

    private fun formatWithCommas(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        // Standard grouping (e.g., 12,500 or 1,25,000)
        val sb = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            sb.append(str[i])
            count++
            if (i > 0) {
                if (count == 3 || (count > 3 && (count - 3) % 2 == 0)) {
                    sb.append(',')
                }
            }
        }
        return sb.reverse().toString()
    }

    fun formatDateShort(timestampMillis: Long, languageCode: String = "en"): String {
        val locale = if (languageCode == "bn") Locale.forLanguageTag("bn-BD") else Locale.ENGLISH
        val sdf = SimpleDateFormat("dd MMM", locale)
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateFull(timestampMillis: Long, languageCode: String = "en"): String {
        val locale = if (languageCode == "bn") Locale.forLanguageTag("bn-BD") else Locale.ENGLISH
        val sdf = SimpleDateFormat("dd MMM yyyy", locale)
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateTime(timestampMillis: Long, languageCode: String = "en"): String {
        val locale = if (languageCode == "bn") Locale.forLanguageTag("bn-BD") else Locale.ENGLISH
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
        return sdf.format(Date(timestampMillis))
    }

    fun startOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfYesterdayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfTodayMillis(nowMillis)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    fun startOfWeekMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfTodayMillis(nowMillis)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        return cal.timeInMillis
    }

    fun startOfMonthMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfTodayMillis(nowMillis)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun startOfLastMonthMillis(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val startThisMonth = startOfMonthMillis(nowMillis)
        val cal = Calendar.getInstance().apply {
            timeInMillis = startThisMonth
            add(Calendar.MONTH, -1)
        }
        return Pair(cal.timeInMillis, startThisMonth - 1L)
    }

    fun startOfYearMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfTodayMillis(nowMillis)
            set(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
