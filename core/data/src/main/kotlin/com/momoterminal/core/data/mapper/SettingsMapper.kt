package com.momoterminal.core.data.mapper

import com.momoterminal.core.domain.model.settings.*
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject

object SettingsMapper {
    
    fun jsonToMerchantSettings(json: JsonObject): MerchantSettings? {
        return try {
            val profile = json["profile"]?.jsonObject?.let { jsonToMerchantProfile(it) }
                ?: return null
            
            val businessDetails = json["businessDetails"]?.jsonObject?.let { 
                jsonToBusinessDetails(it) 
            } ?: BusinessDetails()
            
            val contactInfo = json["contactInfo"]?.jsonObject?.let { 
                jsonToContactInfo(it) 
            } ?: ContactInfo()
            
            val notificationPrefs = json["notificationPrefs"]?.jsonObject?.let {
                jsonToNotificationPreferences(it)
            } ?: NotificationPreferences()
            
            val transactionLimits = json["transactionLimits"]?.jsonObject?.let {
                jsonToTransactionLimits(it)
            } ?: TransactionLimits()
            
            val featureFlags = json["featureFlags"]?.jsonObject?.let {
                jsonToFeatureFlags(it)
            } ?: FeatureFlags()
            
            val paymentProviders = json["paymentProviders"]?.let {
                // Parse array of providers
                emptyList<PaymentProvider>() // TODO: implement array parsing
            } ?: emptyList()
            
            MerchantSettings(
                profile = profile,
                businessDetails = businessDetails,
                contactInfo = contactInfo,
                notificationPrefs = notificationPrefs,
                transactionLimits = transactionLimits,
                featureFlags = featureFlags,
                paymentProviders = paymentProviders
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun jsonToMerchantProfile(json: JsonObject): MerchantProfile {
        return MerchantProfile(
            id = json["id"]?.jsonPrimitive?.content ?: "",
            userId = json["userId"]?.jsonPrimitive?.content ?: "",
            businessName = json["businessName"]?.jsonPrimitive?.content ?: "",
            merchantCode = json["merchantCode"]?.jsonPrimitive?.content ?: "",
            status = json["status"]?.jsonPrimitive?.content?.let {
                MerchantStatus.valueOf(it.uppercase())
            } ?: MerchantStatus.ACTIVE,
            createdAt = json["createdAt"]?.jsonPrimitive?.content?.let {
                Instant.parse(it)
            } ?: Instant.now(),
            updatedAt = json["updatedAt"]?.jsonPrimitive?.content?.let {
                Instant.parse(it)
            } ?: Instant.now()
        )
    }
    
    private fun jsonToBusinessDetails(json: JsonObject): BusinessDetails {
        return BusinessDetails(
            businessType = json["businessType"]?.jsonPrimitive?.content?.let {
                BusinessType.valueOf(it.uppercase())
            },
            taxId = json["taxId"]?.jsonPrimitive?.content,
            registrationNumber = json["registrationNumber"]?.jsonPrimitive?.content,
            location = json["location"]?.jsonObject?.let { locationJson ->
                Location(
                    latitude = locationJson["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    longitude = locationJson["lng"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    address = locationJson["address"]?.jsonPrimitive?.content ?: "",
                    city = locationJson["city"]?.jsonPrimitive?.content ?: "",
                    country = locationJson["country"]?.jsonPrimitive?.content ?: ""
                )
            },
            businessCategory = json["businessCategory"]?.jsonPrimitive?.content,
            description = json["description"]?.jsonPrimitive?.content,
            website = json["website"]?.jsonPrimitive?.content
        )
    }
    
    private fun jsonToContactInfo(json: JsonObject): ContactInfo {
        return ContactInfo(
            email = json["email"]?.jsonPrimitive?.content,
            phone = json["phone"]?.jsonPrimitive?.content,
            whatsapp = json["whatsapp"]?.jsonPrimitive?.content,
            addressLine1 = json["addressLine1"]?.jsonPrimitive?.content,
            addressLine2 = json["addressLine2"]?.jsonPrimitive?.content,
            city = json["city"]?.jsonPrimitive?.content,
            state = json["state"]?.jsonPrimitive?.content,
            postalCode = json["postalCode"]?.jsonPrimitive?.content,
            countryCode = json["countryCode"]?.jsonPrimitive?.content
        )
    }
    
    private fun jsonToNotificationPreferences(json: JsonObject): NotificationPreferences {
        return NotificationPreferences(
            emailEnabled = json["emailEnabled"]?.jsonPrimitive?.boolean ?: true,
            smsEnabled = json["smsEnabled"]?.jsonPrimitive?.boolean ?: true,
            pushEnabled = json["pushEnabled"]?.jsonPrimitive?.boolean ?: true,
            whatsappEnabled = json["whatsappEnabled"]?.jsonPrimitive?.boolean ?: false,
            events = json["eventsConfig"]?.jsonObject?.let { eventsJson ->
                NotificationEvents(
                    transactionSuccess = eventsJson["transaction_success"]?.jsonPrimitive?.boolean ?: true,
                    transactionFailed = eventsJson["transaction_failed"]?.jsonPrimitive?.boolean ?: true,
                    dailySummary = eventsJson["daily_summary"]?.jsonPrimitive?.boolean ?: true,
                    weeklyReport = eventsJson["weekly_report"]?.jsonPrimitive?.boolean ?: false,
                    securityAlerts = eventsJson["security_alerts"]?.jsonPrimitive?.boolean ?: true,
                    systemUpdates = eventsJson["system_updates"]?.jsonPrimitive?.boolean ?: false
                )
            } ?: NotificationEvents(),
            quietHours = json["quietHours"]?.jsonObject?.let { quietJson ->
                QuietHours(
                    startTime = quietJson["start"]?.jsonPrimitive?.content ?: "22:00",
                    endTime = quietJson["end"]?.jsonPrimitive?.content ?: "08:00",
                    enabled = quietJson["enabled"]?.jsonPrimitive?.boolean ?: true
                )
            }
        )
    }
    
    private fun jsonToTransactionLimits(json: JsonObject): TransactionLimits {
        return TransactionLimits(
            dailyLimit = json["dailyLimit"]?.jsonPrimitive?.content?.let { BigDecimal(it) },
            singleTransactionLimit = json["singleTransactionLimit"]?.jsonPrimitive?.content?.let { 
                BigDecimal(it) 
            },
            monthlyLimit = json["monthlyLimit"]?.jsonPrimitive?.content?.let { BigDecimal(it) },
            minimumAmount = json["minimumAmount"]?.jsonPrimitive?.content?.let { 
                BigDecimal(it) 
            } ?: BigDecimal("100.00"),
            maximumAmount = json["maximumAmount"]?.jsonPrimitive?.content?.let { BigDecimal(it) },
            currency = json["currency"]?.jsonPrimitive?.content ?: "XAF",
            requireApprovalAbove = json["requireApprovalAbove"]?.jsonPrimitive?.content?.let { 
                BigDecimal(it) 
            }
        )
    }
    
    private fun jsonToFeatureFlags(json: JsonObject): FeatureFlags {
        return FeatureFlags(
            nfcEnabled = json["nfcEnabled"]?.jsonPrimitive?.boolean ?: false,
            offlineMode = json["offlineMode"]?.jsonPrimitive?.boolean ?: true,
            autoSync = json["autoSync"]?.jsonPrimitive?.boolean ?: true,
            biometricRequired = json["biometricRequired"]?.jsonPrimitive?.boolean ?: false,
            receiptsEnabled = json["receiptsEnabled"]?.jsonPrimitive?.boolean ?: true,
            multiCurrency = json["multiCurrency"]?.jsonPrimitive?.boolean ?: false,
            advancedAnalytics = json["advancedAnalytics"]?.jsonPrimitive?.boolean ?: false,
            apiAccess = json["apiAccess"]?.jsonPrimitive?.boolean ?: false
        )
    }
}
