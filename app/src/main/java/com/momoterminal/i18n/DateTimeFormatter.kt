package com.momoterminal.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.momoterminal.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Locale-aware date/time formatting for transaction history and UI.
 */
object MomoDateTimeFormatter {
    
    /**
     * Format timestamp as relative time ("2 min ago") or absolute date.
     */
    fun formatRelative(timestamp: Long, context: Context, locale: Locale = Locale.getDefault()): String {
        val now = Instant.now()
        val then = Instant.ofEpochMilli(timestamp)
        val minutes = ChronoUnit.MINUTES.between(then, now)
        val hours = ChronoUnit.HOURS.between(then, now)
        val days = ChronoUnit.DAYS.between(then, now)
        
        return when {
            minutes < 1 -> context.getString(R.string.just_now)
            minutes < 60 -> context.getString(R.string.minutes_ago, minutes.toInt())
            hours < 24 -> context.getString(R.string.hours_ago, hours.toInt())
            days < 7 -> context.getString(R.string.days_ago, days.toInt())
            else -> formatDate(timestamp, locale)
        }
    }
    
    /**
     * Format as localized date (e.g., "Dec 3, 2025" or "3 déc. 2025").
     */
    fun formatDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return dateTime.format(formatter)
    }
    
    /**
     * Format as localized date and time.
     */
    fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        return dateTime.format(formatter)
    }
    
    /**
     * Format as time only (e.g., "2:30 PM" or "14:30").
     */
    fun formatTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return dateTime.format(formatter)
    }
    
    /**
     * Format for transaction list grouping headers.
     */
    fun formatGroupHeader(timestamp: Long, context: Context, locale: Locale = Locale.getDefault()): String {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        
        return when {
            date == today -> context.getString(R.string.today)
            date == today.minusDays(1) -> context.getString(R.string.filter_today) // "Yesterday" if you add it
            date.isAfter(today.minusDays(7)) -> {
                // Day name for this week
                date.format(DateTimeFormatter.ofPattern("EEEE", locale))
            }
            else -> formatDate(timestamp, locale)
        }
    }
    
    /**
     * Format for transaction detail screen.
     */
    fun formatFull(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(locale)
        return dateTime.format(formatter)
    }
    
    /**
     * Format for export/receipt (ISO-like but readable).
     */
    fun formatForExport(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}

/**
 * Composable helper for relative time formatting.
 */
@Composable
fun rememberRelativeTimeFormatter(): (Long) -> String {
    val context = LocalContext.current
    val locale = LocalAppLocale.current
    return remember(locale) { { timestamp: Long -> MomoDateTimeFormatter.formatRelative(timestamp, context, locale) } }
}

/**
 * Composable helper for date formatting.
 */
@Composable
fun rememberDateFormatter(): (Long) -> String {
    val locale = LocalAppLocale.current
    return remember(locale) { { timestamp: Long -> MomoDateTimeFormatter.formatDate(timestamp, locale) } }
}
