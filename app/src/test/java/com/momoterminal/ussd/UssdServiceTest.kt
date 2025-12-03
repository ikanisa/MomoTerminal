package com.momoterminal.ussd

import com.momoterminal.data.model.CountryConfig
import org.junit.Assert.*
import org.junit.Test

class UssdServiceTest {

    private val testCountries = mapOf(
        "RW" to CountryConfig(
            id = "rw", code = "RW", name = "Rwanda", nameLocal = "Rwanda",
            currency = "RWF", currencySymbol = "FRw", currencyDecimals = 0,
            phonePrefix = "+250", phoneLength = 9, flagEmoji = "🇷🇼",
            primaryLanguage = "rw",
            providerName = "MTN MoMo", providerCode = "MTN", providerColor = "#FFCC00",
            ussdBaseCode = "*182#",
            ussdSendToPhone = "*182*1*1*{phone}*{amount}#",
            ussdPayMerchant = "*182*8*1*{merchant}*{amount}#",
            hasUssdSupport = true
        ),
        "TG" to CountryConfig(
            id = "tg", code = "TG", name = "Togo", nameLocal = "Togo",
            currency = "XOF", currencySymbol = "CFA", currencyDecimals = 0,
            phonePrefix = "+228", phoneLength = 8, flagEmoji = "🇹🇬",
            primaryLanguage = "fr",
            providerName = "Togocom T-Money", providerCode = "TMONEY", providerColor = "#00A651",
            ussdBaseCode = "*145#",
            ussdSendToPhone = "*145*1*{amount}*{phone}#",
            ussdPayMerchant = "*145*3*{merchant}*{amount}#",
            hasUssdSupport = true
        ),
        "MU" to CountryConfig(
            id = "mu", code = "MU", name = "Mauritius", nameLocal = "Mauritius",
            currency = "MUR", currencySymbol = "₨", currencyDecimals = 2,
            phonePrefix = "+230", phoneLength = 8, flagEmoji = "🇲🇺",
            primaryLanguage = "en",
            providerName = "my.t money", providerCode = "MYT", providerColor = "#E4002B",
            ussdBaseCode = null,
            ussdSendToPhone = null,
            ussdPayMerchant = null,
            hasUssdSupport = false, hasAppSupport = true, hasQrSupport = true
        )
    )

    @Test
    fun `Rwanda MTN MoMo merchant payment USSD is generated correctly`() {
        val country = testCountries["RW"]!!
        val ussd = country.generateMerchantPaymentUssd("12345", "5000")
        assertEquals("*182*8*1*12345*5000#", ussd)
    }

    @Test
    fun `Rwanda MTN MoMo send money USSD is generated correctly`() {
        val country = testCountries["RW"]!!
        val ussd = country.generateSendMoneyUssd("078123456", "10000")
        assertEquals("*182*1*1*078123456*10000#", ussd)
    }

    @Test
    fun `Togo T-Money send money has amount before phone`() {
        val country = testCountries["TG"]!!
        val ussd = country.ussdSendToPhone
            ?.replace("{amount}", "5000")
            ?.replace("{phone}", "90123456")
        assertEquals("*145*1*5000*90123456#", ussd)
    }

    @Test
    fun `Mauritius returns null for USSD as it is app-based`() {
        val country = testCountries["MU"]!!
        assertNull(country.generateMerchantPaymentUssd("12345", "100"))
        assertNull(country.generateSendMoneyUssd("12345678", "100"))
        assertFalse(country.hasUssdSupport)
        assertTrue(country.hasAppSupport)
    }

    @Test
    fun `Phone number validation works correctly`() {
        val rwanda = testCountries["RW"]!!
        assertTrue(rwanda.isValidPhoneLength("781234567"))
        assertFalse(rwanda.isValidPhoneLength("78123456"))
        assertFalse(rwanda.isValidPhoneLength("7812345678"))
    }

    @Test
    fun `Currency formatting respects decimals`() {
        val rwanda = testCountries["RW"]!!
        val mauritius = testCountries["MU"]!!
        assertEquals("FRw 5000", rwanda.formatAmount(5000.0))
        assertEquals("₨ 100.50", mauritius.formatAmount(100.50))
    }

    @Test
    fun `All countries have required fields`() {
        testCountries.values.forEach { country ->
            assertNotNull("Country ${country.code} missing id", country.id)
            assertNotNull("Country ${country.code} missing name", country.name)
            assertNotNull("Country ${country.code} missing currency", country.currency)
            assertNotNull("Country ${country.code} missing phonePrefix", country.phonePrefix)
            assertNotNull("Country ${country.code} missing providerName", country.providerName)
            assertTrue("Country ${country.code} phoneLength should be positive", country.phoneLength > 0)
        }
    }

    @Test
    fun `USSD templates contain required placeholders`() {
        testCountries.values
            .filter { it.hasUssdSupport }
            .forEach { country ->
                country.ussdPayMerchant?.let { template ->
                    assertTrue("Country ${country.code} merchant template missing {merchant}", template.contains("{merchant}"))
                    assertTrue("Country ${country.code} merchant template missing {amount}", template.contains("{amount}"))
                }
                country.ussdSendToPhone?.let { template ->
                    assertTrue("Country ${country.code} send template missing {phone}", template.contains("{phone}"))
                    assertTrue("Country ${country.code} send template missing {amount}", template.contains("{amount}"))
                }
            }
    }
}
