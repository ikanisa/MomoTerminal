package com.momoterminal.monitoring

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Firebase Analytics integration.
 * Provides methods for logging screen views and custom events.
 */
@Singleton
class AnalyticsHelper @Inject constructor() {

    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    /**
     * Log a screen view event.
     * @param screenName The name of the screen
     * @param screenClass The class name of the screen
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { param(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
    }

    /**
     * Log when NFC is activated for payment.
     * @param amount The payment amount
     * @param provider The mobile money provider
     * @param currency The currency code
     */
    fun logNfcActivated(amount: String, provider: String, currency: String = "GHS") {
        analytics.logEvent(EVENT_NFC_ACTIVATED) {
            param(PARAM_AMOUNT, amount)
            param(PARAM_PROVIDER, provider)
            param(PARAM_CURRENCY, currency)
        }
    }

    /**
     * Log when NFC payment is cancelled.
     * @param reason The cancellation reason
     */
    fun logNfcCancelled(reason: String = "user_cancelled") {
        analytics.logEvent(EVENT_NFC_CANCELLED) {
            param(PARAM_REASON, reason)
        }
    }

    /**
     * Log a successful NFC tap event.
     * @param transactionId The transaction ID
     * @param amount The payment amount
     * @param provider The mobile money provider
     */
    fun logNfcTapSuccess(transactionId: String, amount: String, provider: String) {
        analytics.logEvent(EVENT_NFC_TAP_SUCCESS) {
            param(PARAM_TRANSACTION_ID, transactionId)
            param(PARAM_AMOUNT, amount)
            param(PARAM_PROVIDER, provider)
        }
    }

    /**
     * Log an NFC tap failure.
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    fun logNfcTapError(errorCode: String, errorMessage: String) {
        analytics.logEvent(EVENT_NFC_TAP_ERROR) {
            param(PARAM_ERROR_CODE, errorCode)
            param(PARAM_ERROR_MESSAGE, errorMessage)
        }
    }

    /**
     * Log a payment received via SMS.
     * @param amount The payment amount
     * @param provider The mobile money provider
     * @param transactionId The transaction ID
     */
    fun logPaymentReceived(amount: Double, provider: String, transactionId: String?) {
        analytics.logEvent(EVENT_PAYMENT_RECEIVED) {
            param(PARAM_AMOUNT, amount.toString())
            param(PARAM_PROVIDER, provider)
            transactionId?.let { param(PARAM_TRANSACTION_ID, it) }
        }
    }

    /**
     * Log when transaction sync is started.
     * @param pendingCount Number of pending transactions
     */
    fun logSyncStarted(pendingCount: Int) {
        analytics.logEvent(EVENT_SYNC_STARTED) {
            param(PARAM_PENDING_COUNT, pendingCount.toLong())
        }
    }

    /**
     * Log successful transaction sync.
     * @param syncedCount Number of transactions synced
     * @param durationMs Duration of sync in milliseconds
     */
    fun logSyncCompleted(syncedCount: Int, durationMs: Long) {
        analytics.logEvent(EVENT_SYNC_COMPLETED) {
            param(PARAM_SYNCED_COUNT, syncedCount.toLong())
            param(PARAM_DURATION_MS, durationMs)
        }
    }

    /**
     * Log sync failure.
     * @param errorMessage The error message
     * @param pendingCount Number of pending transactions
     */
    fun logSyncFailed(errorMessage: String, pendingCount: Int) {
        analytics.logEvent(EVENT_SYNC_FAILED) {
            param(PARAM_ERROR_MESSAGE, errorMessage)
            param(PARAM_PENDING_COUNT, pendingCount.toLong())
        }
    }

    /**
     * Log merchant setup completed.
     * @param provider The default provider
     */
    fun logMerchantSetup(provider: String) {
        analytics.logEvent(EVENT_MERCHANT_SETUP) {
            param(PARAM_PROVIDER, provider)
        }
    }

    /**
     * Log user login/app open.
     * @param merchantCode The merchant code
     * @param loginMethod The login method (biometric, pin, etc.)
     */
    fun logLogin(merchantCode: String, loginMethod: String = "app_open") {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(PARAM_MERCHANT_CODE, merchantCode)
            param(FirebaseAnalytics.Param.METHOD, loginMethod)
        }
    }

    /**
     * Set user property for analytics segmentation.
     * @param name The property name
     * @param value The property value
     */
    fun setUserProperty(name: String, value: String) {
        analytics.setUserProperty(name, value)
    }

    /**
     * Set the merchant ID for analytics.
     * @param merchantId The merchant ID
     */
    fun setMerchantId(merchantId: String) {
        analytics.setUserId(merchantId)
        setUserProperty(PROPERTY_MERCHANT_ID, merchantId)
    }

    /**
     * Set the primary provider for the merchant.
     * @param provider The provider name
     */
    fun setPrimaryProvider(provider: String) {
        setUserProperty(PROPERTY_PRIMARY_PROVIDER, provider)
    }

    /**
     * Enable or disable analytics collection.
     * @param enabled Whether to enable analytics
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    companion object {
        // Custom Events
        private const val EVENT_NFC_ACTIVATED = "nfc_activated"
        private const val EVENT_NFC_CANCELLED = "nfc_cancelled"
        private const val EVENT_NFC_TAP_SUCCESS = "nfc_tap_success"
        private const val EVENT_NFC_TAP_ERROR = "nfc_tap_error"
        private const val EVENT_PAYMENT_RECEIVED = "payment_received"
        private const val EVENT_SYNC_STARTED = "sync_started"
        private const val EVENT_SYNC_COMPLETED = "sync_completed"
        private const val EVENT_SYNC_FAILED = "sync_failed"
        private const val EVENT_MERCHANT_SETUP = "merchant_setup"

        // Custom Parameters
        private const val PARAM_AMOUNT = "amount"
        private const val PARAM_PROVIDER = "provider"
        private const val PARAM_CURRENCY = "currency"
        private const val PARAM_TRANSACTION_ID = "transaction_id"
        private const val PARAM_ERROR_CODE = "error_code"
        private const val PARAM_ERROR_MESSAGE = "error_message"
        private const val PARAM_REASON = "reason"
        private const val PARAM_PENDING_COUNT = "pending_count"
        private const val PARAM_SYNCED_COUNT = "synced_count"
        private const val PARAM_DURATION_MS = "duration_ms"
        private const val PARAM_MERCHANT_CODE = "merchant_code"

        // User Properties
        private const val PROPERTY_MERCHANT_ID = "merchant_id"
        private const val PROPERTY_PRIMARY_PROVIDER = "primary_provider"
    }
}
