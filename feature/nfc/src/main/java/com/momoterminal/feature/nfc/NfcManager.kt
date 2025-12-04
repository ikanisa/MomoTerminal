package com.momoterminal.feature.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for NFC state and operations.
 * Provides centralized NFC state management and event handling.
 * 
 * Communicates with NfcHceService via Intent to send/clear payment data.
 */
@Singleton
class NfcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NfcManager"
        private const val DEFAULT_TIMEOUT_MS = 60_000L // 60 seconds
        
        // Intent actions for HCE service communication
        // These must match the constants in NfcHceService
        private const val ACTION_SET_PAYMENT_DATA = "com.momoterminal.action.SET_PAYMENT_DATA"
        private const val ACTION_CLEAR_PAYMENT_DATA = "com.momoterminal.action.CLEAR_PAYMENT_DATA"
        
        // Intent extras
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_MERCHANT_CODE = "extra_merchant_code"
        private const val EXTRA_PROVIDER = "extra_provider"
        private const val EXTRA_CURRENCY = "extra_currency"
        private const val EXTRA_COUNTRY_CODE = "extra_country_code"
        private const val EXTRA_USSD_STRING = "extra_ussd_string"
        
        // HCE service class name (in app module)
        private const val HCE_SERVICE_CLASS = "com.momoterminal.NfcHceService"
    }
    
    private val _nfcState = MutableStateFlow<NfcState>(NfcState.Ready)
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()
    
    private val _currentPaymentData = MutableStateFlow<NfcPaymentData?>(null)
    val currentPaymentData: StateFlow<NfcPaymentData?> = _currentPaymentData.asStateFlow()
    
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var nfcAdapter: NfcAdapter? = null
    
    init {
        initializeNfc()
    }
    
    /**
     * Initialize NFC adapter and check device support.
     */
    private fun initializeNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        updateNfcState()
    }
    
    /**
     * Update NFC state based on adapter status.
     */
    fun updateNfcState() {
        val newState = when {
            nfcAdapter == null -> NfcState.NotSupported
            !nfcAdapter!!.isEnabled -> NfcState.Disabled
            _currentPaymentData.value != null -> {
                _currentPaymentData.value?.let { NfcState.Active(it) } ?: NfcState.Ready
            }
            else -> NfcState.Ready
        }
        _nfcState.value = newState
        Log.d(TAG, "NFC state updated: $newState")
    }
    
    /**
     * Check if NFC is available and enabled.
     */
    fun isNfcAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    /**
     * Check if NFC is supported on this device.
     */
    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }
    
    /**
     * Activate NFC payment broadcasting.
     * Sends payment data to HCE service via Intent.
     * @param paymentData The payment data to broadcast
     * @param timeoutMs Timeout in milliseconds (default 60 seconds)
     */
    fun activatePayment(paymentData: NfcPaymentData, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        if (!isNfcAvailable()) {
            _nfcState.value = if (isNfcSupported()) NfcState.Disabled else NfcState.NotSupported
            return
        }
        
        if (!paymentData.isValid()) {
            _nfcState.value = NfcState.Error(
                "Invalid payment data",
                NfcErrorCode.INVALID_AMOUNT
            )
            return
        }
        
        Log.d(TAG, "Activating payment: $paymentData")
        
        // Send payment data to HCE Service via Intent
        sendPaymentDataToHceService(paymentData)
        // Get merchant phone from AppConfig
        val appConfig = AppConfig(context)
        val merchantPhone = appConfig.getMerchantPhone()
        
        // Update PaymentState for HCE service
        com.momoterminal.feature.payment.nfc.PaymentState.setPaymentData(
            amount = paymentData.amountInMinorUnits / 100, // Convert back to whole units
            merchantPhone = if (merchantPhone.isBlank()) paymentData.merchantPhone else merchantPhone,
            currency = paymentData.currency
        )
        
        // Update state to activating
        _nfcState.value = NfcState.Activating
        _currentPaymentData.value = paymentData
        
        // Short delay to show activation state
        handler.postDelayed({
            _nfcState.value = NfcState.Active(paymentData)
        }, 300)
        
        // Start timeout timer
        startTimeout(timeoutMs)
    }
    
    /**
     * Cancel the current NFC payment.
     * Clears payment data in HCE service via Intent.
     */
    fun cancelPayment() {
        Log.d(TAG, "Cancelling payment")
        cancelTimeout()
        
        // Clear payment data in HCE Service
        clearPaymentDataInHceService()
        
        _currentPaymentData.value = null
        _nfcState.value = NfcState.Ready
        
        // Clear PaymentState
        com.momoterminal.feature.payment.nfc.PaymentState.reset()
    }
    
    /**
     * Send payment data to HCE service via explicit Intent.
     */
    private fun sendPaymentDataToHceService(paymentData: NfcPaymentData) {
        try {
            val intent = Intent().apply {
                setClassName(context.packageName, HCE_SERVICE_CLASS)
                action = ACTION_SET_PAYMENT_DATA
                putExtra(EXTRA_AMOUNT, paymentData.getDisplayAmount())
                putExtra(EXTRA_MERCHANT_CODE, paymentData.merchantPhone)
                putExtra(EXTRA_PROVIDER, paymentData.provider.name)
                putExtra(EXTRA_CURRENCY, paymentData.currency)
                putExtra(EXTRA_COUNTRY_CODE, paymentData.countryCode)
                putExtra(EXTRA_USSD_STRING, paymentData.toUssdString())
            }
            context.startService(intent)
            Log.d(TAG, "Payment data sent to HCE service: ${paymentData.toUssdString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send payment data to HCE service", e)
        }
    }
    
    /**
     * Clear payment data in HCE service via explicit Intent.
     */
    private fun clearPaymentDataInHceService() {
        try {
            val intent = Intent().apply {
                setClassName(context.packageName, HCE_SERVICE_CLASS)
                action = ACTION_CLEAR_PAYMENT_DATA
            }
            context.startService(intent)
            Log.d(TAG, "Payment data cleared in HCE service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear payment data in HCE service", e)
        }
    }
    
    /**
     * Called when NFC transaction is successful.
     * @param transactionId The transaction identifier
     */
    fun onTransactionSuccess(transactionId: String? = null) {
        cancelTimeout()
        
        // Clear payment data in HCE service
        clearPaymentDataInHceService()
        
        val txId = transactionId ?: generateTransactionId()
        Log.d(TAG, "Transaction successful: $txId")
        _nfcState.value = NfcState.Success(txId)
        
        // Reset to ready after delay
        handler.postDelayed({
            _currentPaymentData.value = null
            _nfcState.value = NfcState.Ready
        }, 3000)
    }
    
    /**
     * Called when NFC transaction fails.
     * @param errorCode The error code
     * @param message Optional error message
     */
    fun onTransactionError(errorCode: NfcErrorCode, message: String? = null) {
        cancelTimeout()
        
        // Clear payment data in HCE service
        clearPaymentDataInHceService()
        
        val errorMessage = message ?: errorCode.description
        Log.e(TAG, "Transaction error: $errorCode - $errorMessage")
        _nfcState.value = NfcState.Error(errorMessage, errorCode)
        
        // Reset to ready after delay
        handler.postDelayed({
            _currentPaymentData.value = null
            _nfcState.value = NfcState.Ready
        }, 3000)
    }
    
    /**
     * Update processing progress.
     * @param progress Progress from 0.0 to 1.0
     */
    fun updateProgress(progress: Float) {
        _nfcState.value = NfcState.Processing(progress.coerceIn(0f, 1f))
    }
    
    /**
     * Start the timeout timer.
     */
    private fun startTimeout(timeoutMs: Long) {
        cancelTimeout()
        timeoutRunnable = Runnable {
            Log.w(TAG, "NFC transaction timed out")
            
            // Clear payment data in HCE service on timeout
            clearPaymentDataInHceService()
            
            _nfcState.value = NfcState.Timeout
            _currentPaymentData.value = null
            
            // Reset to ready after delay
            handler.postDelayed({
                _nfcState.value = NfcState.Ready
            }, 3000)
        }
        handler.postDelayed(timeoutRunnable!!, timeoutMs)
    }
    
    /**
     * Cancel the timeout timer.
     */
    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }
    
    /**
     * Generate a unique transaction ID.
     */
    private fun generateTransactionId(): String {
        return "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
    }
    
    /**
     * Get the current payment URI for HCE service.
     */
    fun getCurrentPaymentUri(): String? {
        return _currentPaymentData.value?.toPaymentUri()
    }
    
    /**
     * Get the current USSD string for HCE service.
     */
    fun getCurrentUssdString(): String? {
        return _currentPaymentData.value?.toUssdString()
    }
}
