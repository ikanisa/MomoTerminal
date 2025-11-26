package com.momoterminal.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.momoterminal.config.AppConfig
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
 */
@Singleton
class NfcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NfcManager"
        private const val DEFAULT_TIMEOUT_MS = 60_000L // 60 seconds
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
     */
    fun cancelPayment() {
        Log.d(TAG, "Cancelling payment")
        cancelTimeout()
        _currentPaymentData.value = null
        _nfcState.value = NfcState.Ready
    }
    
    /**
     * Called when NFC transaction is successful.
     * @param transactionId The transaction identifier
     */
    fun onTransactionSuccess(transactionId: String? = null) {
        cancelTimeout()
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
