package com.momoterminal.data.model

import androidx.compose.ui.graphics.Color

/**
 * Enumeration of all supported mobile money providers across Africa.
 */
enum class MobileMoneyProvider(
    val code: String,
    val displayName: String,
    val shortName: String,
    val brandColorHex: String,
    val countries: List<String>
) {
    MTN("MTN", "MTN Mobile Money", "MoMo", "#FFCC00", listOf("RW", "GH", "CM", "BJ", "ZM", "CG")),
    ORANGE("ORANGE", "Orange Money", "Orange", "#FF6600", listOf("SN", "CI", "ML", "BF", "CF", "CD", "GN")),
    VODACOM("VODACOM", "Vodacom M-Pesa", "M-Pesa", "#E60000", listOf("TZ")),
    AIRTEL("AIRTEL", "Airtel Money", "Airtel", "#ED1C24", listOf("MW", "NE", "TD", "GA", "SC")),
    ECOCASH("ECOCASH", "Econet EcoCash", "EcoCash", "#00A651", listOf("ZW", "BI")),
    MVOLA("MVOLA", "Telma MVola", "MVola", "#E31E24", listOf("MG", "KM")),
    TMONEY("TMONEY", "Togocom T-Money", "T-Money", "#00A651", listOf("TG")),
    MOOV("MOOV", "Moov Money", "Moov", "#6F2C91", listOf("MR")),
    MTC("MTC", "MTC Money (Maris)", "Maris", "#0066B3", listOf("NA")),
    MYT("MYT", "my.t money", "my.t", "#E4002B", listOf("MU")),
    GETESA("GETESA", "GETESA Mobile Money", "GETESA", "#009639", listOf("GQ")),
    DMONEY("DMONEY", "D-Money", "D-Money", "#00A651", listOf("DJ"));

    val brandColor: Color
        get() = Color(android.graphics.Color.parseColor(brandColorHex))

    companion object {
        fun fromCode(code: String): MobileMoneyProvider? =
            entries.find { it.code.equals(code, ignoreCase = true) }

        fun forCountry(countryCode: String): MobileMoneyProvider? =
            entries.find { countryCode.uppercase() in it.countries }
    }
}
