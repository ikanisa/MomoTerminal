package com.momoterminal.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Locale-aware currency formatting for African and global currencies.
 */
object CurrencyFormatter {
    
    /**
     * Currency configuration with locale-specific formatting rules.
     */
    data class CurrencyConfig(
        val code: String,
        val symbol: String,
        val decimals: Int,
        val symbolPosition: SymbolPosition = SymbolPosition.AFTER,
        val groupingSeparator: Char = ' ',
        val decimalSeparator: Char = ','
    )
    
    enum class SymbolPosition { BEFORE, AFTER }
    
    private val CURRENCIES = mapOf(
        // Central/West African CFA Franc
        "XOF" to CurrencyConfig("XOF", "FCFA", 0, SymbolPosition.AFTER, ' ', ','),
        "XAF" to CurrencyConfig("XAF", "FCFA", 0, SymbolPosition.AFTER, ' ', ','),
        // East African
        "RWF" to CurrencyConfig("RWF", "FRw", 0, SymbolPosition.AFTER, ' ', ','),
        "KES" to CurrencyConfig("KES", "KSh", 2, SymbolPosition.BEFORE, ',', '.'),
        "TZS" to CurrencyConfig("TZS", "TSh", 0, SymbolPosition.BEFORE, ',', '.'),
        "UGX" to CurrencyConfig("UGX", "USh", 0, SymbolPosition.BEFORE, ',', '.'),
        // West African
        "GHS" to CurrencyConfig("GHS", "₵", 2, SymbolPosition.BEFORE, ',', '.'),
        "NGN" to CurrencyConfig("NGN", "₦", 2, SymbolPosition.BEFORE, ',', '.'),
        // Southern African
        "ZAR" to CurrencyConfig("ZAR", "R", 2, SymbolPosition.BEFORE, ' ', ','),
        "ZMW" to CurrencyConfig("ZMW", "K", 2, SymbolPosition.BEFORE, ',', '.'),
        "MZN" to CurrencyConfig("MZN", "MT", 2, SymbolPosition.AFTER, ' ', ','),
        // Other
        "USD" to CurrencyConfig("USD", "$", 2, SymbolPosition.BEFORE, ',', '.'),
        "EUR" to CurrencyConfig("EUR", "€", 2, SymbolPosition.AFTER, ' ', ',')
    )
    
    private val COUNTRY_CURRENCY = mapOf(
        // CFA Franc (XOF) - West Africa
        "BJ" to "XOF", "BF" to "XOF", "CI" to "XOF", "GW" to "XOF",
        "ML" to "XOF", "NE" to "XOF", "SN" to "XOF", "TG" to "XOF",
        // CFA Franc (XAF) - Central Africa
        "CM" to "XAF", "CF" to "XAF", "TD" to "XAF", "CG" to "XAF",
        "GQ" to "XAF", "GA" to "XAF",
        // East Africa
        "RW" to "RWF", "KE" to "KES", "TZ" to "TZS", "UG" to "UGX",
        // West Africa
        "GH" to "GHS", "NG" to "NGN",
        // Southern Africa
        "ZA" to "ZAR", "ZM" to "ZMW", "MZ" to "MZN"
    )
    
    fun getCurrencyForCountry(countryCode: String): String {
        return COUNTRY_CURRENCY[countryCode.uppercase()] ?: "USD"
    }
    
    fun getConfig(currencyCode: String): CurrencyConfig {
        return CURRENCIES[currencyCode.uppercase()] 
            ?: CurrencyConfig(currencyCode, currencyCode, 2)
    }
    
    /**
     * Format amount with currency symbol.
     * @param amountInSmallestUnit Amount in smallest currency unit (e.g., pesewas, cents)
     * @param currencyCode ISO currency code
     * @return Formatted string like "1 500 RWF" or "₵50.00"
     */
    fun format(amountInSmallestUnit: Long, currencyCode: String): String {
        val config = getConfig(currencyCode)
        val divisor = BigDecimal.TEN.pow(config.decimals)
        val amount = BigDecimal(amountInSmallestUnit).divide(divisor, config.decimals, RoundingMode.HALF_EVEN)
        
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = config.groupingSeparator
            decimalSeparator = config.decimalSeparator
        }
        
        val pattern = if (config.decimals > 0) "#,##0.${"0".repeat(config.decimals)}" else "#,##0"
        val formatter = DecimalFormat(pattern, symbols)
        val formatted = formatter.format(amount)
        
        return when (config.symbolPosition) {
            SymbolPosition.BEFORE -> "${config.symbol}$formatted"
            SymbolPosition.AFTER -> "$formatted ${config.symbol}"
        }
    }
    
    /**
     * Format from BigDecimal (major units).
     */
    fun format(amount: BigDecimal, currencyCode: String): String {
        val config = getConfig(currencyCode)
        val smallestUnit = amount.multiply(BigDecimal.TEN.pow(config.decimals))
            .setScale(0, RoundingMode.HALF_EVEN).toLong()
        return format(smallestUnit, currencyCode)
    }
    
    /**
     * Format with explicit locale for number formatting.
     */
    fun format(amountInSmallestUnit: Long, currencyCode: String, locale: Locale): String {
        val config = getConfig(currencyCode)
        val divisor = BigDecimal.TEN.pow(config.decimals)
        val amount = BigDecimal(amountInSmallestUnit).divide(divisor, config.decimals, RoundingMode.HALF_EVEN)
        
        val formatter = DecimalFormat.getInstance(locale) as DecimalFormat
        formatter.minimumFractionDigits = config.decimals
        formatter.maximumFractionDigits = config.decimals
        val formatted = formatter.format(amount)
        
        return when (config.symbolPosition) {
            SymbolPosition.BEFORE -> "${config.symbol}$formatted"
            SymbolPosition.AFTER -> "$formatted ${config.symbol}"
        }
    }
    
    /**
     * Format without symbol (plain number).
     */
    fun formatPlain(amountInSmallestUnit: Long, currencyCode: String): String {
        val config = getConfig(currencyCode)
        val divisor = BigDecimal.TEN.pow(config.decimals)
        val amount = BigDecimal(amountInSmallestUnit).divide(divisor, config.decimals, RoundingMode.HALF_EVEN)
        
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = config.groupingSeparator
            decimalSeparator = config.decimalSeparator
        }
        
        val pattern = if (config.decimals > 0) "#,##0.${"0".repeat(config.decimals)}" else "#,##0"
        return DecimalFormat(pattern, symbols).format(amount)
    }
}

/**
 * Composable helper for currency formatting.
 */
@Composable
fun rememberCurrencyFormatter(currencyCode: String = LocalCurrency.current): (Long) -> String {
    return remember(currencyCode) { { amount: Long -> CurrencyFormatter.format(amount, currencyCode) } }
}
