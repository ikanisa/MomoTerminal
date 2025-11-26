package com.momoterminal.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Firebase Crashlytics integration.
 * Provides methods for logging exceptions, setting user data, and adding breadcrumbs.
 */
@Singleton
class CrashlyticsHelper @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    /**
     * Set the current user ID for crash reports.
     * @param userId The merchant/user identifier
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    /**
     * Set a custom key-value pair for crash context.
     * @param key The key name
     * @param value The value (String, Boolean, Int, Long, Float, Double)
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set the merchant code for crash context.
     * @param merchantCode The merchant code
     */
    fun setMerchantCode(merchantCode: String) {
        setCustomKey(KEY_MERCHANT_CODE, merchantCode)
    }

    /**
     * Set the current provider for crash context.
     * @param provider The mobile money provider (MTN, VODAFONE, AIRTELTIGO)
     */
    fun setProvider(provider: String) {
        setCustomKey(KEY_PROVIDER, provider)
    }

    /**
     * Set NFC availability status.
     * @param available Whether NFC is available on the device
     */
    fun setNfcAvailable(available: Boolean) {
        setCustomKey(KEY_NFC_AVAILABLE, available)
    }

    /**
     * Log a breadcrumb message for debugging context.
     * @param message The breadcrumb message
     */
    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }

    /**
     * Log a non-fatal exception.
     * @param exception The exception to log
     */
    fun logException(exception: Throwable) {
        crashlytics.recordException(exception)
    }

    /**
     * Log a non-fatal exception with additional context.
     * @param exception The exception to log
     * @param message Additional context message
     */
    fun logException(exception: Throwable, message: String) {
        crashlytics.log(message)
        crashlytics.recordException(exception)
    }

    /**
     * Log an NFC-related error.
     * @param errorCode The NFC error code
     * @param message Error message
     * @param exception Optional exception
     */
    fun logNfcError(errorCode: String, message: String, exception: Throwable? = null) {
        setCustomKey(KEY_LAST_NFC_ERROR, errorCode)
        logBreadcrumb("NFC Error: $errorCode - $message")
        exception?.let { logException(it) }
    }

    /**
     * Log a payment-related error.
     * @param paymentId The payment/transaction ID if available
     * @param errorMessage Error message
     * @param exception Optional exception
     */
    fun logPaymentError(paymentId: String?, errorMessage: String, exception: Throwable? = null) {
        paymentId?.let { setCustomKey(KEY_LAST_PAYMENT_ID, it) }
        logBreadcrumb("Payment Error: $errorMessage")
        exception?.let { logException(it) }
    }

    /**
     * Log a sync-related error.
     * @param pendingCount Number of pending transactions
     * @param errorMessage Error message
     * @param exception Optional exception
     */
    fun logSyncError(pendingCount: Int, errorMessage: String, exception: Throwable? = null) {
        setCustomKey(KEY_PENDING_SYNC_COUNT, pendingCount)
        logBreadcrumb("Sync Error: $errorMessage (pending: $pendingCount)")
        exception?.let { logException(it) }
    }

    /**
     * Enable or disable crash collection.
     * @param enabled Whether to enable crash collection
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    companion object {
        private const val KEY_MERCHANT_CODE = "merchant_code"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_NFC_AVAILABLE = "nfc_available"
        private const val KEY_LAST_NFC_ERROR = "last_nfc_error"
        private const val KEY_LAST_PAYMENT_ID = "last_payment_id"
        private const val KEY_PENDING_SYNC_COUNT = "pending_sync_count"
    }
}
