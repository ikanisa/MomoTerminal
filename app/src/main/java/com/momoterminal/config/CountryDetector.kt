package com.momoterminal.config

/**
 * Detects country from phone number prefix.
 */
object CountryDetector {

    private val prefixToCountry = mapOf(
        "+250" to "RW",  // Rwanda
        "+243" to "CD",  // DR Congo
        "+257" to "BI",  // Burundi
        "+255" to "TZ",  // Tanzania
        "+260" to "ZM",  // Zambia
        "+221" to "SN",  // Senegal
        "+225" to "CI",  // Côte d'Ivoire
        "+223" to "ML",  // Mali
        "+226" to "BF",  // Burkina Faso
        "+227" to "NE",  // Niger
        "+229" to "BJ",  // Benin
        "+228" to "TG",  // Togo
        "+224" to "GN",  // Guinea
        "+222" to "MR",  // Mauritania
        "+237" to "CM",  // Cameroon
        "+241" to "GA",  // Gabon
        "+242" to "CG",  // Congo
        "+236" to "CF",  // Central African Rep.
        "+235" to "TD",  // Chad
        "+240" to "GQ",  // Equatorial Guinea
        "+265" to "MW",  // Malawi
        "+263" to "ZW",  // Zimbabwe
        "+264" to "NA",  // Namibia
        "+233" to "GH",  // Ghana
        "+261" to "MG",  // Madagascar
        "+269" to "KM",  // Comoros
        "+248" to "SC",  // Seychelles
        "+253" to "DJ"   // Djibouti
    )

    /**
     * Detect country code from phone number.
     * @param phoneNumber Phone number with country prefix (e.g., +250788123456)
     * @return ISO country code (e.g., "RW") or "RW" as default
     */
    fun detectCountry(phoneNumber: String): String {
        val normalized = phoneNumber.trim().replace(" ", "")
        
        // Try each prefix
        for ((prefix, country) in prefixToCountry) {
            if (normalized.startsWith(prefix)) {
                return country
            }
        }
        
        // Default to Rwanda
        return "RW"
    }

    /**
     * Get currency for a country code.
     */
    fun getCurrency(countryCode: String): String {
        return SupportedCountries.getByCode(countryCode)?.currency ?: "RWF"
    }

    /**
     * Get phone prefix for a country.
     */
    fun getPhonePrefix(countryCode: String): String {
        return SupportedCountries.getByCode(countryCode)?.phonePrefix ?: "+250"
    }
}
