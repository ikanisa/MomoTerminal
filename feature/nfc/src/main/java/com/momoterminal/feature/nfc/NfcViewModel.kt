package com.momoterminal.feature.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcManager: NfcManager
) : ViewModel() {

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NfcState.Ready
        )

    /**
     * Activate NFC payment with the specified amount.
     * This will enable NFC HCE and wait for payer to tap.
     */
    fun activateNfcPayment(amount: Long, currency: String = "RWF") {
        val paymentData = NfcPaymentData(
            merchantPhone = "", // Will be populated from AppConfig in NfcManager
            amountInMinorUnits = amount * 100, // Convert to minor units (cents)
            currency = currency,
            reference = generateReference()
        )
        nfcManager.activatePayment(paymentData)
    }

    fun onScanStarted() {
        // Logic to prepare for scanning if needed
    }

    fun onScanCancelled() {
        nfcManager.cancelPayment()
    }
    
    private fun generateReference(): String {
        return "REF-${System.currentTimeMillis()}"
    }
}
