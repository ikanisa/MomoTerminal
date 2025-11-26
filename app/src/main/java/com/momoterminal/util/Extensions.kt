package com.momoterminal.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Extension functions for common operations.
 */

// Context Extensions

/**
 * Check if the device has an active internet connection.
 */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// String Extensions

/**
 * Safely parse string to Double, returning null if invalid.
 */
fun String.toDoubleOrNullSafe(): Double? {
    return try {
        this.replace(",", "").toDoubleOrNull()
    } catch (e: Exception) {
        null
    }
}

/**
 * Format phone number for display.
 */
fun String.formatPhoneNumber(): String {
    return if (this.startsWith("+")) {
        this
    } else if (this.startsWith("0")) {
        "+233${this.substring(1)}"
    } else {
        this
    }
}

/**
 * Truncate string with ellipsis.
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        "${this.take(maxLength)}..."
    } else {
        this
    }
}

// Long Extensions (Timestamps)

/**
 * Format timestamp to relative time string.
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes min ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hr ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days d ago"
        }
        else -> {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
        }
    }
}

/**
 * Format timestamp to date string.
 */
fun Long.toDateString(pattern: String = "dd MMM yyyy, HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

// Double Extensions (Currency)

/**
 * Format amount as currency.
 */
fun Double.toCurrency(currencyCode: String = "GHS"): String {
    val locale = when (currencyCode) {
        "GHS" -> Locale("en", "GH")
        "RWF" -> Locale("rw", "RW")
        else -> Locale.getDefault()
    }
    
    return try {
        NumberFormat.getCurrencyInstance(locale).format(this)
    } catch (e: Exception) {
        "$currencyCode ${String.format(Locale.getDefault(), "%.2f", this)}"
    }
}

/**
 * Format amount with two decimal places.
 */
fun Double.formatAmount(): String {
    return String.format(Locale.getDefault(), "%.2f", this)
}

// SharedPreferences Extensions

/**
 * Edit shared preferences with apply.
 */
inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    block(editor)
    editor.apply()
}

/**
 * Get string or throw exception if not found.
 */
fun SharedPreferences.getStringOrThrow(key: String): String {
    return getString(key, null)
        ?: throw IllegalStateException("Required preference '$key' not found")
}
