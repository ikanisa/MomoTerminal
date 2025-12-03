package com.momoterminal.sms.patterns

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for SMS parsing patterns organized by country and provider.
 * Allows adding new providers/locales without changing core parsing logic.
 */
@Singleton
class SmsPatternRegistry @Inject constructor() {
    
    private val patterns = mutableMapOf<PatternKey, ProviderPatterns>()
    
    init {
        registerDefaultPatterns()
    }
    
    data class PatternKey(val countryCode: String, val providerId: String)
    
    data class ProviderPatterns(
        val providerId: String,
        val providerName: String,
        val senderPatterns: List<String>,
        val receivedPattern: Pattern,
        val sentPattern: Pattern,
        val balancePattern: Pattern?,
        val transactionIdPattern: Pattern?,
        val currencyCode: String,
        val amountGroup: Int = 1,
        val partyGroup: Int = 2
    )
    
    fun getPatterns(countryCode: String, providerId: String): ProviderPatterns? {
        return patterns[PatternKey(countryCode.uppercase(), providerId.uppercase())]
    }
    
    fun getPatternsForCountry(countryCode: String): List<ProviderPatterns> {
        return patterns.filterKeys { it.countryCode == countryCode.uppercase() }.values.toList()
    }
    
    fun detectProvider(countryCode: String, sender: String): ProviderPatterns? {
        return getPatternsForCountry(countryCode).find { provider ->
            provider.senderPatterns.any { pattern ->
                sender.contains(pattern, ignoreCase = true)
            }
        }
    }
    
    fun register(countryCode: String, patterns: ProviderPatterns) {
        this.patterns[PatternKey(countryCode.uppercase(), patterns.providerId.uppercase())] = patterns
    }
    
    private fun registerDefaultPatterns() {
        // ==================== GHANA ====================
        register("GH", ProviderPatterns(
            providerId = "MTN",
            providerName = "MTN MoMo",
            senderPatterns = listOf("MTN", "MoMo", "Mobile Money"),
            receivedPattern = Pattern.compile(
                "(?:You have received|Received)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:You have sent|Sent|Transfer of)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+to\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:balance|bal)\\s*(?:is|:)?\\s*GH[SC]?\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*(?:is|:)?|ID:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "GHS"
        ))
        
        // ==================== RWANDA ====================
        register("RW", ProviderPatterns(
            providerId = "MTN",
            providerName = "MTN MoMo",
            senderPatterns = listOf("MTN", "MoMo", "M-Money", "MobileMoney"),
            receivedPattern = Pattern.compile(
                "(?:You have received|Received|Wabonye)\\s+(?:RWF|FRw)?\\s*([\\d,]+)\\s+(?:from|kuva kuri)\\s+(.+?)(?:\\.|\\s+Trans|\\s+Your|\\s+Amafaranga)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:You have sent|Sent|Transfer of|Wohereje)\\s+(?:RWF|FRw)?\\s*([\\d,]+)\\s+(?:to|kuri)\\s+(.+?)(?:\\.|\\s+Trans|\\s+Your)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:balance|bal|Amafaranga asigaye)\\s*(?:is|:)?\\s*(?:RWF|FRw)?\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*(?:is|:)?|ID:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "RWF"
        ))
        
        // ==================== KENYA ====================
        register("KE", ProviderPatterns(
            providerId = "MPESA",
            providerName = "M-Pesa",
            senderPatterns = listOf("MPESA", "M-PESA", "Safaricom"),
            receivedPattern = Pattern.compile(
                "([A-Z0-9]+)\\s+Confirmed\\.?\\s+You have received\\s+Ksh([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)\\s+on",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "([A-Z0-9]+)\\s+Confirmed\\.?\\s+Ksh([\\d,]+\\.?\\d*)\\s+sent to\\s+(.+?)\\s+on",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:balance|bal)\\s*(?:is|:)?\\s*Ksh([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("^([A-Z0-9]+)\\s+Confirmed", Pattern.CASE_INSENSITIVE),
            currencyCode = "KES",
            amountGroup = 2,
            partyGroup = 3
        ))
        
        // ==================== SENEGAL ====================
        register("SN", ProviderPatterns(
            providerId = "ORANGE",
            providerName = "Orange Money",
            senderPatterns = listOf("Orange", "OM", "Orange Money"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+de\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé|Transfert de)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|XOF)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Ref(?:erence)?\\s*(?:No)?\\s*(?:est|:)?|Ref:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "XOF"
        ))
        
        register("SN", ProviderPatterns(
            providerId = "WAVE",
            providerName = "Wave",
            senderPatterns = listOf("Wave"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|F)\\s+de\\s+(.+?)(?:\\.|\\s+)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé)\\s+([\\d\\s]+)\\s*(?:FCFA|F)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|F)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = null,
            currencyCode = "XOF"
        ))
        
        // ==================== CÔTE D'IVOIRE ====================
        register("CI", ProviderPatterns(
            providerId = "MTN",
            providerName = "MTN MoMo",
            senderPatterns = listOf("MTN", "MoMo"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+de\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|XOF)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*(?:est|:)?|ID:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "XOF"
        ))
        
        register("CI", ProviderPatterns(
            providerId = "ORANGE",
            providerName = "Orange Money",
            senderPatterns = listOf("Orange", "OM"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+de\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé)\\s+([\\d\\s]+)\\s*(?:FCFA|XOF)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|XOF)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Ref(?:erence)?\\s*:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "XOF"
        ))
        
        // ==================== TANZANIA ====================
        register("TZ", ProviderPatterns(
            providerId = "MPESA",
            providerName = "M-Pesa",
            senderPatterns = listOf("MPESA", "M-PESA", "Vodacom"),
            receivedPattern = Pattern.compile(
                "(?:Umepokea|Received)\\s+(?:TSh|TZS)?\\s*([\\d,]+)\\s+(?:kutoka|from)\\s+(.+?)(?:\\.|\\s+)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Umetuma|Sent)\\s+(?:TSh|TZS)?\\s*([\\d,]+)\\s+(?:kwa|to)\\s+(.+?)(?:\\.|\\s+)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:Salio|balance)\\s*(?:ni|:)?\\s*(?:TSh|TZS)?\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "TZS"
        ))
        
        // ==================== UGANDA ====================
        register("UG", ProviderPatterns(
            providerId = "MTN",
            providerName = "MTN MoMo",
            senderPatterns = listOf("MTN", "MoMo"),
            receivedPattern = Pattern.compile(
                "(?:You have received|Received)\\s+(?:UGX|USh)?\\s*([\\d,]+)\\s+from\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:You have sent|Sent)\\s+(?:UGX|USh)?\\s*([\\d,]+)\\s+to\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:balance|bal)\\s*(?:is|:)?\\s*(?:UGX|USh)?\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "UGX"
        ))
        
        // ==================== CAMEROON ====================
        register("CM", ProviderPatterns(
            providerId = "MTN",
            providerName = "MTN MoMo",
            senderPatterns = listOf("MTN", "MoMo"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|XAF)\\s+de\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé)\\s+([\\d\\s]+)\\s*(?:FCFA|XAF)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+Trans)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|XAF)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Trans(?:action)?\\s*(?:ID)?\\s*:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "XAF"
        ))
        
        register("CM", ProviderPatterns(
            providerId = "ORANGE",
            providerName = "Orange Money",
            senderPatterns = listOf("Orange", "OM"),
            receivedPattern = Pattern.compile(
                "(?:Vous avez reçu|Reçu)\\s+([\\d\\s]+)\\s*(?:FCFA|XAF)\\s+de\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            sentPattern = Pattern.compile(
                "(?:Vous avez envoyé|Envoyé)\\s+([\\d\\s]+)\\s*(?:FCFA|XAF)\\s+(?:à|a)\\s+(.+?)(?:\\.|\\s+Ref)",
                Pattern.CASE_INSENSITIVE
            ),
            balancePattern = Pattern.compile("(?:solde|balance)\\s*(?:est|:)?\\s*([\\d\\s]+)\\s*(?:FCFA|XAF)", Pattern.CASE_INSENSITIVE),
            transactionIdPattern = Pattern.compile("(?:Ref(?:erence)?\\s*:?)\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            currencyCode = "XAF"
        ))
    }
    
    fun getSupportedCountries(): Set<String> = patterns.keys.map { it.countryCode }.toSet()
    
    fun getProvidersForCountry(countryCode: String): List<String> {
        return patterns.filterKeys { it.countryCode == countryCode.uppercase() }.values.map { it.providerName }
    }
}
