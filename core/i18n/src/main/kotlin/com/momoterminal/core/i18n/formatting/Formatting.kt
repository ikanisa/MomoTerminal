package com.momoterminal.core.i18n.formatting

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.momoterminal.core.i18n.locale.rememberAppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// 1. DATE/TIME FORMATTER

@Singleton
class DateFormatter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun formatDate(
        timestamp: Long,
        locale: Locale = Locale.getDefault(),
        style: FormatStyle = FormatStyle.MEDIUM
    ): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(locale)
            .format(date)
    }
    
    fun formatTime(
        timestamp: Long,
        locale: Locale = Locale.getDefault(),
        style: FormatStyle = FormatStyle.SHORT
    ): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val time = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return DateTimeFormatter.ofLocalizedTime(style)
            .withLocale(locale)
            .format(time)
    }
    
    fun formatDateTime(
        timestamp: Long,
        locale: Locale = Locale.getDefault(),
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.SHORT
    ): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = instant.atZone(ZoneId.systemDefault())
        return DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle)
            .withLocale(locale)
            .format(dateTime)
    }
    
    fun formatRelative(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> formatDate(timestamp, locale)
        }
    }
    
    fun formatCustom(timestamp: Long, pattern: String, locale: Locale = Locale.getDefault()): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = instant.atZone(ZoneId.systemDefault())
        return DateTimeFormatter.ofPattern(pattern, locale).format(dateTime)
    }
}

// 2. NUMBER FORMATTER

@Singleton
class NumberFormatter @Inject constructor() {
    
    fun formatNumber(
        number: Number,
        locale: Locale = Locale.getDefault(),
        minFractionDigits: Int = 0,
        maxFractionDigits: Int = 2
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = minFractionDigits
        formatter.maximumFractionDigits = maxFractionDigits
        return formatter.format(number)
    }
    
    fun formatPercent(
        value: Double,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getPercentInstance(locale)
        return formatter.format(value)
    }
    
    fun formatCompact(
        number: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        return when {
            number < 1_000 -> number.toString()
            number < 1_000_000 -> String.format(locale, "%.1fK", number / 1_000.0)
            number < 1_000_000_000 -> String.format(locale, "%.1fM", number / 1_000_000.0)
            else -> String.format(locale, "%.1fB", number / 1_000_000_000.0)
        }
    }
    
    fun formatOrdinal(number: Int, locale: Locale = Locale.getDefault()): String {
        // English-specific, extend for other locales
        if (locale.language == "en") {
            val suffix = when {
                number % 100 in 11..13 -> "th"
                number % 10 == 1 -> "st"
                number % 10 == 2 -> "nd"
                number % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$number$suffix"
        }
        return number.toString()
    }
}

// 3. CURRENCY FORMATTER (Generic - not tied to real currency)

@Singleton
class CurrencyFormatter @Inject constructor() {
    
    // Generic "credits" or "points" system
    fun formatCredits(
        amount: Double,
        locale: Locale = Locale.getDefault(),
        symbol: String = "¤" // Generic currency symbol
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        
        val formatted = formatter.format(amount)
        
        // Symbol placement depends on locale
        return when (locale.language) {
            "en" -> "$symbol$formatted"
            "fr", "es", "de" -> "$formatted $symbol"
            else -> "$symbol$formatted"
        }
    }
    
    // Real currency formatting (if needed)
    fun formatCurrency(
        amount: Double,
        currencyCode: String, // ISO 4217 (USD, EUR, GBP)
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        return formatter.format(amount)
    }
}

// 4. UNIT FORMATTER (Generic distance, weight, etc.)

enum class DistanceUnit {
    METRIC,    // km, m
    IMPERIAL   // mi, ft
}

enum class WeightUnit {
    METRIC,    // kg, g
    IMPERIAL   // lb, oz
}

@Singleton
class UnitFormatter @Inject constructor() {
    
    fun formatDistance(
        meters: Double,
        unit: DistanceUnit = DistanceUnit.METRIC,
        locale: Locale = Locale.getDefault()
    ): String {
        return when (unit) {
            DistanceUnit.METRIC -> {
                if (meters < 1000) {
                    "${meters.toInt()} m"
                } else {
                    String.format(locale, "%.1f km", meters / 1000)
                }
            }
            DistanceUnit.IMPERIAL -> {
                val miles = meters * 0.000621371
                if (miles < 0.1) {
                    val feet = meters * 3.28084
                    "${feet.toInt()} ft"
                } else {
                    String.format(locale, "%.1f mi", miles)
                }
            }
        }
    }
    
    fun formatWeight(
        grams: Double,
        unit: WeightUnit = WeightUnit.METRIC,
        locale: Locale = Locale.getDefault()
    ): String {
        return when (unit) {
            WeightUnit.METRIC -> {
                if (grams < 1000) {
                    "${grams.toInt()} g"
                } else {
                    String.format(locale, "%.1f kg", grams / 1000)
                }
            }
            WeightUnit.IMPERIAL -> {
                val pounds = grams * 0.00220462
                String.format(locale, "%.1f lb", pounds)
            }
        }
    }
    
    fun getPreferredDistanceUnit(locale: Locale): DistanceUnit {
        return when (locale.country) {
            "US", "GB", "MM" -> DistanceUnit.IMPERIAL
            else -> DistanceUnit.METRIC
        }
    }
}

// 5. COMPOSE HELPERS

@Composable
fun rememberDateFormatter(): DateFormatter {
    val context = LocalContext.current
    return remember { DateFormatter(context) }
}

@Composable
fun rememberNumberFormatter(): NumberFormatter {
    return remember { NumberFormatter() }
}

@Composable
fun rememberCurrencyFormatter(): CurrencyFormatter {
    return remember { CurrencyFormatter() }
}

@Composable
fun rememberUnitFormatter(): UnitFormatter {
    return remember { UnitFormatter() }
}

// 6. FORMATTING EXTENSIONS

@Composable
fun Long.formatAsDate(style: FormatStyle = FormatStyle.MEDIUM): String {
    val formatter = rememberDateFormatter()
    val locale = rememberAppLocale().toLocale()
    return remember(this, style, locale) {
        formatter.formatDate(this, locale, style)
    }
}

@Composable
fun Long.formatAsDateTime(): String {
    val formatter = rememberDateFormatter()
    val locale = rememberAppLocale().toLocale()
    return remember(this, locale) {
        formatter.formatDateTime(this, locale)
    }
}

@Composable
fun Number.formatAsNumber(maxDecimals: Int = 2): String {
    val formatter = rememberNumberFormatter()
    val locale = rememberAppLocale().toLocale()
    return remember(this, maxDecimals, locale) {
        formatter.formatNumber(this, locale, maxFractionDigits = maxDecimals)
    }
}

@Composable
fun Double.formatAsCredits(symbol: String = "¤"): String {
    val formatter = rememberCurrencyFormatter()
    val locale = rememberAppLocale().toLocale()
    return remember(this, symbol, locale) {
        formatter.formatCredits(this, locale, symbol)
    }
}

@Composable
fun Double.formatAsDistance(): String {
    val formatter = rememberUnitFormatter()
    val locale = rememberAppLocale().toLocale()
    val unit = remember(locale) { formatter.getPreferredDistanceUnit(locale) }
    return remember(this, unit, locale) {
        formatter.formatDistance(this, unit, locale)
    }
}

// 7. USAGE EXAMPLES

@Composable
fun FormattingExamples() {
    val timestamp = System.currentTimeMillis()
    val number = 1234.56
    val distance = 1500.0 // meters
    
    Column {
        // Date formatting
        Text("Date: ${timestamp.formatAsDate()}")
        Text("DateTime: ${timestamp.formatAsDateTime()}")
        
        // Number formatting
        Text("Number: ${number.formatAsNumber()}")
        
        // Currency formatting
        Text("Credits: ${number.formatAsCredits("★")}")
        
        // Distance formatting
        Text("Distance: ${distance.formatAsDistance()}")
    }
}
