package com.momoterminal.core.common

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
 * Currency locale mappings.
 * Add new currencies here as the app expands to new regions.
 */
private val CURRENCY_LOCALES = mapOf(
    "GHS" to Locale("en", "GH"),  // Ghana Cedi
    "KES" to Locale("en", "KE"),  // Kenyan Shilling
    "UGX" to Locale("en", "UG"),  // Ugandan Shilling
    "TZS" to Locale("en", "TZ"),  // Tanzanian Shilling
    "NGN" to Locale("en", "NG"),  // Nigerian Naira
    "XOF" to Locale("fr", "SN"),  // West African CFA Franc
    "XAF" to Locale("fr", "CM")   // Central African CFA Franc
)

/**
 * Format amount as currency.
 */
fun Double.toCurrency(currencyCode: String = "GHS"): String {
    val locale = CURRENCY_LOCALES[currencyCode] ?: Locale.getDefault()
    
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

// Long Extensions (Currency in Pesewas)

/**
 * Format amount in pesewas as currency.
 * Converts from pesewas (smallest unit) to main currency unit.
 * 1 GHS = 100 pesewas.
 */
fun Long.pesewaToCurrency(currencyCode: String = "GHS"): String {
    val amountInMainUnit = this / 100.0
    val locale = CURRENCY_LOCALES[currencyCode] ?: Locale.getDefault()
    
    return try {
        NumberFormat.getCurrencyInstance(locale).format(amountInMainUnit)
    } catch (e: Exception) {
        "$currencyCode ${String.format(Locale.getDefault(), "%.2f", amountInMainUnit)}"
    }
}

/**
 * Format amount in pesewas with two decimal places.
 * Converts from pesewas (smallest unit) to main currency unit.
 */
fun Long.pesewaToFormatAmount(): String {
    val amountInMainUnit = this / 100.0
    return String.format(Locale.getDefault(), "%.2f", amountInMainUnit)
}

/**
 * Convert pesewas to main currency unit (Double).
 */
fun Long.pesewaToMain(): Double = this / 100.0

/**
 * Convert main currency unit (Double) to pesewas.
 */
fun Double.toPesewas(): Long = (this * 100).toLong()

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
