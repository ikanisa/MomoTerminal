package com.momoterminal.nfc

import android.content.Context
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
    
    /** Initialize NFC adapter and check device support. */
    private fun initializeNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        updateNfcState()
    }
    
    /** Update NFC state based on adapter status. */
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
    
    fun isNfcAvailable(): Boolean = nfcAdapter?.isEnabled == true
    fun isNfcSupported(): Boolean = nfcAdapter != null
    
    /** Activate NFC payment broadcasting. */
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
        
        _nfcState.value = NfcState.Activating
        _currentPaymentData.value = paymentData
        
        handler.postDelayed({
            _nfcState.value = NfcState.Active(paymentData)
        }, 300)
        
        startTimeout(timeoutMs)
    }
    
    /** Cancel the current NFC payment. */
    fun cancelPayment() {
        Log.d(TAG, "Cancelling payment")
        cancelTimeout()
        _currentPaymentData.value = null
        _nfcState.value = NfcState.Ready
    }
    
    /** Called when NFC transaction is successful. */
    fun onTransactionSuccess(transactionId: String? = null) {
        cancelTimeout()
        val txId = transactionId ?: generateTransactionId()
        Log.d(TAG, "Transaction successful: $txId")
        _nfcState.value = NfcState.Success(txId)
        
        handler.postDelayed({
            _currentPaymentData.value = null
            _nfcState.value = NfcState.Ready
        }, 3000)
    }
    
    /** Called when NFC transaction fails. */
    fun onTransactionError(errorCode: NfcErrorCode, message: String? = null) {
        cancelTimeout()
        val errorMessage = message ?: errorCode.description
        Log.e(TAG, "Transaction error: $errorCode - $errorMessage")
        _nfcState.value = NfcState.Error(errorMessage, errorCode)
        
        handler.postDelayed({
            _currentPaymentData.value = null
            _nfcState.value = NfcState.Ready
        }, 3000)
    }
    
    /** Update processing progress. */
    fun updateProgress(progress: Float) {
        _nfcState.value = NfcState.Processing(progress.coerceIn(0f, 1f))
    }
    
    private fun startTimeout(timeoutMs: Long) {
        cancelTimeout()
        timeoutRunnable = Runnable {
            Log.w(TAG, "NFC transaction timed out")
            _nfcState.value = NfcState.Timeout
            _currentPaymentData.value = null
            
            handler.postDelayed({
                _nfcState.value = NfcState.Ready
            }, 3000)
        }
        handler.postDelayed(timeoutRunnable!!, timeoutMs)
    }
    
    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }
    
    private fun generateTransactionId(): String {
        return "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
    }
}
