package com.momoterminal.util.money

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a monetary value with proper precision handling.
 * Stores amounts in the smallest currency unit (e.g., pesewas for GHS)
 * to avoid floating-point precision errors.
 *
 * This class provides:
 * - Safe arithmetic operations for financial calculations
 * - Proper rounding for display purposes
 * - Serialization support for API communication
 * - Conversion utilities between display and storage formats
 */
data class Money(
    @SerializedName("amount_in_smallest_unit")
    val amountInSmallestUnit: Long,
    @SerializedName("currency")
    val currency: Currency = Currency.GHS
) : Serializable, Comparable<Money> {

    /**
     * Supported currencies with their properties.
     *
     * @property code ISO 4217 currency code
     * @property symbol Currency display symbol
     * @property decimals Number of decimal places
     */
    enum class Currency(
        val code: String,
        val symbol: String,
        val decimals: Int,
        val smallestUnitName: String
    ) {
        /** Ghanaian Cedi - 100 pesewas = 1 cedi */
        GHS("GHS", "₵", 2, "pesewas"),
        
        /** Rwandan Franc - no decimal subdivision */
        RWF("RWF", "FRw", 0, "francs"),
        
        /** US Dollar - 100 cents = 1 dollar */
        USD("USD", "$", 2, "cents")
    }

    /**
     * Returns the divisor for converting smallest unit to major unit.
     */
    private val divisor: Long
        get() = BigDecimal.TEN.pow(currency.decimals).toLong()

    /**
     * Returns the amount as a BigDecimal for precise calculations.
     */
    fun toBigDecimal(): BigDecimal {
        return BigDecimal(amountInSmallestUnit)
            .divide(BigDecimal(divisor), currency.decimals, RoundingMode.HALF_EVEN)
    }

    /**
     * Returns the amount as a Double for display purposes.
     * Note: Use [toBigDecimal] for calculations to maintain precision.
     */
    fun toDouble(): Double {
        return amountInSmallestUnit.toDouble() / divisor
    }

    /**
     * Returns a formatted string for display (e.g., "₵50.00").
     */
    fun toDisplayString(): String {
        val amount = toBigDecimal()
        return "${currency.symbol}${String.format("%.${currency.decimals}f", amount)}"
    }

    /**
     * Returns a plain formatted string without currency symbol.
     */
    fun toPlainString(): String {
        return String.format("%.${currency.decimals}f", toBigDecimal())
    }

    /**
     * Returns full display with currency code (e.g., "GHS 50.00").
     */
    fun toFullDisplayString(): String {
        return "${currency.code} ${toPlainString()}"
    }

    // Arithmetic operations

    /**
     * Adds another Money amount. Both must have the same currency.
     */
    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot add different currencies: ${currency.code} and ${other.currency.code}"
        }
        return Money(amountInSmallestUnit + other.amountInSmallestUnit, currency)
    }

    /**
     * Subtracts another Money amount. Both must have the same currency.
     */
    operator fun minus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot subtract different currencies: ${currency.code} and ${other.currency.code}"
        }
        return Money(amountInSmallestUnit - other.amountInSmallestUnit, currency)
    }

    /**
     * Multiplies by a scalar value.
     */
    operator fun times(multiplier: Int): Money {
        return Money(amountInSmallestUnit * multiplier, currency)
    }

    /**
     * Multiplies by a decimal multiplier (e.g., for percentage calculations).
     */
    fun multiply(multiplier: BigDecimal): Money {
        val result = BigDecimal(amountInSmallestUnit)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()
        return Money(result, currency)
    }

    /**
     * Divides by a divisor.
     */
    operator fun div(divisor: Int): Money {
        require(divisor != 0) { "Cannot divide by zero" }
        return Money(amountInSmallestUnit / divisor, currency)
    }

    /**
     * Negates the amount (e.g., for refunds).
     */
    operator fun unaryMinus(): Money {
        return Money(-amountInSmallestUnit, currency)
    }

    // Comparison operations

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) {
            "Cannot compare different currencies: ${currency.code} and ${other.currency.code}"
        }
        return amountInSmallestUnit.compareTo(other.amountInSmallestUnit)
    }

    /**
     * Returns true if this amount is zero.
     */
    fun isZero(): Boolean = amountInSmallestUnit == 0L

    /**
     * Returns true if this amount is positive.
     */
    fun isPositive(): Boolean = amountInSmallestUnit > 0

    /**
     * Returns true if this amount is negative.
     */
    fun isNegative(): Boolean = amountInSmallestUnit < 0

    /**
     * Returns the absolute value of this amount.
     */
    fun abs(): Money = Money(kotlin.math.abs(amountInSmallestUnit), currency)

    companion object {
        /**
         * Creates a Money instance from a Double value in major units.
         * For example, fromMajorUnits(50.00, GHS) = 5000 pesewas.
         *
         * @param amount The amount in major units (e.g., cedis)
         * @param currency The currency (defaults to GHS)
         * @return Money instance with amount converted to smallest unit
         */
        fun fromMajorUnits(amount: Double, currency: Currency = Currency.GHS): Money {
            val divisor = BigDecimal.TEN.pow(currency.decimals)
            val smallestUnit = BigDecimal.valueOf(amount)
                .multiply(divisor)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
            return Money(smallestUnit, currency)
        }

        /**
         * Creates a Money instance from a BigDecimal value in major units.
         * Provides better precision than fromMajorUnits(Double).
         */
        fun fromMajorUnits(amount: BigDecimal, currency: Currency = Currency.GHS): Money {
            val divisor = BigDecimal.TEN.pow(currency.decimals)
            val smallestUnit = amount.multiply(divisor)
                .setScale(0, RoundingMode.HALF_EVEN)
                .toLong()
            return Money(smallestUnit, currency)
        }

        /**
         * Creates a Money instance from smallest currency units.
         * For example, fromSmallestUnit(5000, GHS) = 50.00 cedis.
         */
        fun fromSmallestUnit(amount: Long, currency: Currency = Currency.GHS): Money {
            return Money(amount, currency)
        }

        /**
         * Creates a Money instance from a String representation.
         * Parses "50.00" as 5000 pesewas for GHS.
         */
        fun fromString(amountStr: String, currency: Currency = Currency.GHS): Money {
            val cleanedAmount = amountStr
                .replace(",", "")
                .replace(currency.symbol, "")
                .replace(currency.code, "")
                .trim()
            val amount = cleanedAmount.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Invalid amount string: $amountStr")
            return fromMajorUnits(amount, currency)
        }

        /**
         * Creates a zero Money amount.
         */
        fun zero(currency: Currency = Currency.GHS): Money {
            return Money(0, currency)
        }

        // Convenience factory methods for Ghana Cedis

        /**
         * Creates Money from cedis (GHS).
         */
        fun fromCedis(amount: Double): Money = fromMajorUnits(amount, Currency.GHS)

        /**
         * Creates Money from pesewas (GHS smallest unit).
         */
        fun fromPesewas(amount: Long): Money = fromSmallestUnit(amount, Currency.GHS)
    }
}

/**
 * Extension function to convert Double to Money in the specified currency.
 */
fun Double.toMoney(currency: Money.Currency = Money.Currency.GHS): Money {
    return Money.fromMajorUnits(this, currency)
}

/**
 * Extension function to convert Long (smallest unit) to Money.
 */
fun Long.asPesewas(): Money = Money.fromPesewas(this)

/**
 * Extension function to convert Int (smallest unit) to Money.
 */
fun Int.asPesewas(): Money = Money.fromPesewas(this.toLong())
