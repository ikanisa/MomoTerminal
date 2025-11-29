package com.momoterminal.util.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for the Money class to ensure proper handling of monetary values.
 */
class MoneyTest {

    // ==================== Factory Method Tests ====================

    @Test
    fun `fromMajorUnits with Double correctly converts to smallest unit`() {
        val money = Money.fromMajorUnits(50.00, Money.Currency.GHS)
        assertEquals(5000L, money.amountInSmallestUnit)
        assertEquals(Money.Currency.GHS, money.currency)
    }

    @Test
    fun `fromMajorUnits handles decimal precision correctly`() {
        val money = Money.fromMajorUnits(12.34, Money.Currency.GHS)
        assertEquals(1234L, money.amountInSmallestUnit)
    }

    @Test
    fun `fromMajorUnits rounds correctly for sub-pesewa amounts`() {
        // 12.345 should round to 12.35 (1235 pesewas)
        val money = Money.fromMajorUnits(12.345, Money.Currency.GHS)
        assertEquals(1235L, money.amountInSmallestUnit)
    }

    @Test
    fun `fromMajorUnits with BigDecimal provides precise conversion`() {
        val money = Money.fromMajorUnits(BigDecimal("100.50"), Money.Currency.GHS)
        assertEquals(10050L, money.amountInSmallestUnit)
    }

    @Test
    fun `fromSmallestUnit creates Money correctly`() {
        val money = Money.fromSmallestUnit(5000, Money.Currency.GHS)
        assertEquals(5000L, money.amountInSmallestUnit)
        assertEquals(50.0, money.toDouble(), 0.001)
    }

    @Test
    fun `fromCedis convenience method works correctly`() {
        val money = Money.fromCedis(25.50)
        assertEquals(2550L, money.amountInSmallestUnit)
        assertEquals(Money.Currency.GHS, money.currency)
    }

    @Test
    fun `fromPesewas convenience method works correctly`() {
        val money = Money.fromPesewas(2550)
        assertEquals(2550L, money.amountInSmallestUnit)
        assertEquals(25.50, money.toDouble(), 0.001)
    }

    @Test
    fun `fromString parses valid amount strings`() {
        val money = Money.fromString("100.00")
        assertEquals(10000L, money.amountInSmallestUnit)
    }

    @Test
    fun `fromString handles comma separators`() {
        val money = Money.fromString("1,000.50")
        assertEquals(100050L, money.amountInSmallestUnit)
    }

    @Test
    fun `fromString handles currency symbol`() {
        val money = Money.fromString("₵100.00", Money.Currency.GHS)
        assertEquals(10000L, money.amountInSmallestUnit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromString throws for invalid input`() {
        Money.fromString("invalid")
    }

    @Test
    fun `zero creates zero amount`() {
        val money = Money.zero(Money.Currency.GHS)
        assertEquals(0L, money.amountInSmallestUnit)
        assertTrue(money.isZero())
    }

    // ==================== Display Format Tests ====================

    @Test
    fun `toDisplayString formats GHS correctly`() {
        val money = Money.fromCedis(50.00)
        assertEquals("₵50.00", money.toDisplayString())
    }

    @Test
    fun `toDisplayString handles decimal amounts`() {
        val money = Money.fromPesewas(1234)
        assertEquals("₵12.34", money.toDisplayString())
    }

    @Test
    fun `toPlainString formats without symbol`() {
        val money = Money.fromCedis(99.99)
        assertEquals("99.99", money.toPlainString())
    }

    @Test
    fun `toFullDisplayString includes currency code`() {
        val money = Money.fromCedis(50.00)
        assertEquals("GHS 50.00", money.toFullDisplayString())
    }

    @Test
    fun `toBigDecimal returns precise value`() {
        val money = Money.fromPesewas(12345)
        assertEquals(BigDecimal("123.45"), money.toBigDecimal())
    }

    @Test
    fun `toDouble returns correct floating point`() {
        val money = Money.fromPesewas(5000)
        assertEquals(50.0, money.toDouble(), 0.001)
    }

    // ==================== Arithmetic Operation Tests ====================

    @Test
    fun `plus adds amounts correctly`() {
        val m1 = Money.fromCedis(10.00)
        val m2 = Money.fromCedis(5.50)
        val result = m1 + m2
        assertEquals(1550L, result.amountInSmallestUnit)
        assertEquals(15.50, result.toDouble(), 0.001)
    }

    @Test
    fun `minus subtracts amounts correctly`() {
        val m1 = Money.fromCedis(10.00)
        val m2 = Money.fromCedis(3.50)
        val result = m1 - m2
        assertEquals(650L, result.amountInSmallestUnit)
        assertEquals(6.50, result.toDouble(), 0.001)
    }

    @Test
    fun `minus can result in negative amount`() {
        val m1 = Money.fromCedis(5.00)
        val m2 = Money.fromCedis(10.00)
        val result = m1 - m2
        assertEquals(-500L, result.amountInSmallestUnit)
        assertTrue(result.isNegative())
    }

    @Test
    fun `times multiplies by integer correctly`() {
        val money = Money.fromCedis(5.00)
        val result = money * 3
        assertEquals(1500L, result.amountInSmallestUnit)
    }

    @Test
    fun `multiply with BigDecimal handles percentages`() {
        val money = Money.fromCedis(100.00)
        val result = money.multiply(BigDecimal("0.10")) // 10%
        assertEquals(1000L, result.amountInSmallestUnit)
        assertEquals(10.0, result.toDouble(), 0.001)
    }

    @Test
    fun `div divides correctly`() {
        val money = Money.fromCedis(15.00)
        val result = money / 3
        assertEquals(500L, result.amountInSmallestUnit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `div throws for zero divisor`() {
        val money = Money.fromCedis(10.00)
        money / 0
    }

    @Test
    fun `unaryMinus negates amount`() {
        val money = Money.fromCedis(50.00)
        val result = -money
        assertEquals(-5000L, result.amountInSmallestUnit)
    }

    @Test
    fun `abs returns absolute value`() {
        val negative = Money.fromSmallestUnit(-5000)
        assertEquals(5000L, negative.abs().amountInSmallestUnit)
        
        val positive = Money.fromCedis(50.00)
        assertEquals(5000L, positive.abs().amountInSmallestUnit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plus throws for different currencies`() {
        val ghs = Money.fromCedis(10.00)
        val usd = Money.fromMajorUnits(10.00, Money.Currency.USD)
        ghs + usd
    }

    @Test(expected = IllegalArgumentException::class)
    fun `minus throws for different currencies`() {
        val ghs = Money.fromCedis(10.00)
        val usd = Money.fromMajorUnits(10.00, Money.Currency.USD)
        ghs - usd
    }

    // ==================== Comparison Tests ====================

    @Test
    fun `compareTo returns correct ordering`() {
        val small = Money.fromCedis(10.00)
        val large = Money.fromCedis(20.00)
        val equal = Money.fromCedis(10.00)
        
        assertTrue(small < large)
        assertTrue(large > small)
        assertEquals(0, small.compareTo(equal))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compareTo throws for different currencies`() {
        val ghs = Money.fromCedis(10.00)
        val usd = Money.fromMajorUnits(10.00, Money.Currency.USD)
        ghs.compareTo(usd)
    }

    @Test
    fun `isZero returns true for zero amount`() {
        assertTrue(Money.zero().isZero())
        assertTrue(Money.fromPesewas(0).isZero())
        assertFalse(Money.fromPesewas(1).isZero())
    }

    @Test
    fun `isPositive returns correct value`() {
        assertTrue(Money.fromCedis(10.00).isPositive())
        assertFalse(Money.fromCedis(-10.00).isPositive())
        assertFalse(Money.zero().isPositive())
    }

    @Test
    fun `isNegative returns correct value`() {
        assertTrue(Money.fromSmallestUnit(-100).isNegative())
        assertFalse(Money.fromCedis(10.00).isNegative())
        assertFalse(Money.zero().isNegative())
    }

    // ==================== Currency Tests ====================

    @Test
    fun `RWF currency has no decimals`() {
        val money = Money.fromMajorUnits(1000.0, Money.Currency.RWF)
        assertEquals(1000L, money.amountInSmallestUnit)
        assertEquals("FRw1000", money.toDisplayString())
    }

    @Test
    fun `USD currency works correctly`() {
        val money = Money.fromMajorUnits(99.99, Money.Currency.USD)
        assertEquals(9999L, money.amountInSmallestUnit)
        assertEquals("$99.99", money.toDisplayString())
    }

    // ==================== Extension Function Tests ====================

    @Test
    fun `toMoney extension creates Money from Double`() {
        val money = 50.0.toMoney()
        assertEquals(5000L, money.amountInSmallestUnit)
        assertEquals(Money.Currency.GHS, money.currency)
    }

    @Test
    fun `toMoney extension with currency works correctly`() {
        val money = 100.0.toMoney(Money.Currency.USD)
        assertEquals(10000L, money.amountInSmallestUnit)
        assertEquals(Money.Currency.USD, money.currency)
    }

    @Test
    fun `asPesewas Long extension works correctly`() {
        val money = 5000L.asPesewas()
        assertEquals(5000L, money.amountInSmallestUnit)
        assertEquals(50.0, money.toDouble(), 0.001)
    }

    @Test
    fun `asPesewas Int extension works correctly`() {
        val money = 2500.asPesewas()
        assertEquals(2500L, money.amountInSmallestUnit)
        assertEquals(25.0, money.toDouble(), 0.001)
    }

    // ==================== Precision Tests ====================

    @Test
    fun `no floating point errors with repeated operations`() {
        // This test ensures we don't get floating point errors like 0.1 + 0.2 != 0.3
        var money = Money.zero()
        repeat(100) {
            money += Money.fromCedis(0.01)
        }
        assertEquals(100L, money.amountInSmallestUnit)
        assertEquals(1.0, money.toDouble(), 0.001)
    }

    @Test
    fun `large amounts handled correctly`() {
        val largeAmount = Money.fromCedis(999999999.99)
        assertEquals(99999999999L, largeAmount.amountInSmallestUnit)
        assertEquals("₵999999999.99", largeAmount.toDisplayString())
    }

    @Test
    fun `small amounts handled correctly`() {
        val smallAmount = Money.fromPesewas(1)
        assertEquals(1L, smallAmount.amountInSmallestUnit)
        assertEquals("₵0.01", smallAmount.toDisplayString())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `negative amounts display correctly`() {
        val negative = Money.fromSmallestUnit(-5000)
        assertEquals("-50.00", negative.toPlainString())
    }

    @Test
    fun `equals works correctly`() {
        val m1 = Money.fromCedis(10.00)
        val m2 = Money.fromPesewas(1000)
        val m3 = Money.fromCedis(10.00)
        
        assertEquals(m1, m2)
        assertEquals(m1, m3)
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val m1 = Money.fromCedis(10.00)
        val m2 = Money.fromPesewas(1000)
        
        assertEquals(m1.hashCode(), m2.hashCode())
    }
}
