package com.momoterminal.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CountryRepositoryTest {

    private lateinit var repository: CountryRepository

    @Before
    fun setup() {
        repository = CountryRepository()
    }

    @Test
    fun `fallback countries contain all required countries from spec`() = runTest {
        val requiredCountries = listOf(
            "RW", "BI", "CM", "MG", "MU", "TZ", "ZM", "ZW", "MW", "NA",
            "GH", "BJ", "BF", "CF", "TD", "KM", "CG", "CI", "CD", "DJ",
            "GQ", "GA", "GN", "ML", "MR", "NE", "SN", "SC", "TG"
        )
        requiredCountries.forEach { code ->
            val country = repository.getByCode(code)
            assertNotNull("Missing required country: $code", country)
        }
    }

    @Test
    fun `primary markets are correctly flagged`() = runTest {
        val primaryMarkets = repository.getPrimaryMarkets()
        assertTrue("Should have primary markets", primaryMarkets.isNotEmpty())
        primaryMarkets.forEach { country ->
            assertTrue("${country.code} should be primary market", country.isPrimaryMarket)
        }
    }

    @Test
    fun `USSD supported countries have valid templates`() = runTest {
        val ussdCountries = repository.getUssdSupportedCountries()
        ussdCountries.forEach { country ->
            assertTrue("${country.code} should have USSD support flag", country.hasUssdSupport)
            assertNotNull("${country.code} should have base USSD code", country.ussdBaseCode)
            assertNotNull("${country.code} should have send USSD template", country.ussdSendToPhone)
            assertNotNull("${country.code} should have merchant USSD template", country.ussdPayMerchant)
        }
    }

    @Test
    fun `Mauritius is app-based only`() = runTest {
        val mauritius = repository.getByCode("MU")
        assertNotNull(mauritius)
        assertFalse("Mauritius should not have USSD support", mauritius!!.hasUssdSupport)
        assertTrue("Mauritius should have app support", mauritius.hasAppSupport)
        assertNull("Mauritius should not have USSD base code", mauritius.ussdBaseCode)
    }

    @Test
    fun `default country is Rwanda`() {
        val default = repository.getDefault()
        assertEquals("RW", default.code)
        assertEquals("Rwanda", default.name)
        assertEquals("MTN MoMo", default.providerName)
    }

    @Test
    fun `countries are sorted by launch priority`() = runTest {
        val countries = repository.getPrimaryMarkets()
        for (i in 0 until countries.size - 1) {
            assertTrue(
                "Countries should be sorted by priority",
                countries[i].launchPriority <= countries[i + 1].launchPriority
            )
        }
    }

    @Test
    fun `search finds countries by various fields`() = runTest {
        var results = repository.searchCountries("Rwanda")
        assertTrue(results.any { it.code == "RW" })

        results = repository.searchCountries("Cameroun")
        assertTrue(results.any { it.code == "CM" })

        results = repository.searchCountries("Orange Money")
        assertTrue(results.isNotEmpty())
        results.forEach { assertTrue(it.providerCode == "ORANGE") }

        results = repository.searchCountries("TZ")
        assertTrue(results.any { it.code == "TZ" })
    }

    @Test
    fun `currency decimals are correct for each region`() = runTest {
        val senegal = repository.getByCode("SN")
        assertEquals(0, senegal?.currencyDecimals)

        val mauritius = repository.getByCode("MU")
        assertEquals(2, mauritius?.currencyDecimals)
    }
}
