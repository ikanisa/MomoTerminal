package com.momoterminal.core.i18n.formatting

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object LocalizedFormatters {
    fun formatCurrency(amount: Double, currencyCode: String = "USD", locale: Locale = Locale.getDefault()): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        return formatter.format(amount)
    }
    
    fun formatNumber(number: Number, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).format(number)
    
    fun formatDate(date: Date, pattern: String = "dd MMM yyyy", locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(pattern, locale).format(date)
    
    fun formatTime(date: Date, use24Hour: Boolean = true, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return SimpleDateFormat(pattern, locale).format(date)
    }
}
