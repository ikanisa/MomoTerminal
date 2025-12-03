package com.momoterminal.core.data.repository

import com.momoterminal.config.UssdConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to load USSD configurations from Supabase.
 * Falls back to local config if network fails.
 */
@Singleton
class UssdConfigRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    private var cachedConfigs: Map<String, UssdConfigDto>? = null

    /**
     * Get USSD config for a country from Supabase.
     */
    suspend fun getConfig(countryCode: String): UssdConfigDto? {
        // Try cache first
        cachedConfigs?.get(countryCode)?.let { return it }

        return try {
            val result = postgrest.from("ussd_configs")
                .select(Columns.ALL) {
                    filter { eq("country_code", countryCode.uppercase()) }
                    filter { eq("is_active", true) }
                    limit(1)
                }
                .decodeList<UssdConfigDto>()

            result.firstOrNull()?.also {
                // Cache it
                cachedConfigs = (cachedConfigs ?: emptyMap()) + (countryCode to it)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load USSD config from Supabase, using local")
            // Fallback to local config
            UssdConfig.getConfig(countryCode)?.let {
                UssdConfigDto(
                    country_code = it.countryCode,
                    country_name = it.countryName,
                    provider = it.provider,
                    currency = it.currency,
                    base_code = it.baseCode,
                    send_to_phone = it.sendToPhone,
                    pay_merchant = it.payMerchant
                )
            }
        }
    }

    /**
     * Load all configs and cache them.
     */
    suspend fun loadAllConfigs(): List<UssdConfigDto> {
        return try {
            val result = postgrest.from("ussd_configs")
                .select(Columns.ALL) {
                    filter { eq("is_active", true) }
                }
                .decodeList<UssdConfigDto>()

            // Cache all
            cachedConfigs = result.associateBy { it.country_code }
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to load all USSD configs")
            emptyList()
        }
    }

    /**
     * Generate merchant payment USSD code.
     */
    suspend fun generateMerchantUssd(
        countryCode: String,
        merchantCode: String,
        amount: String
    ): String {
        val config = getConfig(countryCode)
        return config?.pay_merchant
            ?.replace("{merchant}", merchantCode)
            ?.replace("{amount}", amount)
            ?: "*182*8*1*$merchantCode*$amount#" // Default Rwanda MTN
    }
}

@Serializable
data class UssdConfigDto(
    val country_code: String,
    val country_name: String,
    val provider: String,
    val currency: String,
    val base_code: String,
    val send_to_phone: String,
    val pay_merchant: String
)
