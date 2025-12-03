package com.momoterminal.presentation.screens.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.auth.SessionManager
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import com.momoterminal.nfc.NfcWalletIntegrationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NfcTerminalViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val nfcWalletService: NfcWalletIntegrationService,
    private val sessionManager: SessionManager
) : ViewModel() {

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
    val currentPaymentData: StateFlow<NfcPaymentData?> = nfcManager.currentPaymentData

    private val _uiState = MutableStateFlow(NfcTerminalUiState())
    val uiState: StateFlow<NfcTerminalUiState> = _uiState.asStateFlow()

    fun activatePayment(amount: Double, currency: String, merchantCode: String) {
        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = merchantCode,
            amount = amount,
            currency = currency
        )
        nfcManager.activatePayment(paymentData)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
    }

    fun processTagScan(tagId: String, tagData: ByteArray?) {
        val userId = sessionManager.currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            val result = nfcWalletService.processTagScan(userId, tagId, tagData)
            
            _uiState.update { state ->
                when (result) {
                    is NfcWalletIntegrationService.ScanResult.TokenPackRedeemed -> {
                        nfcManager.onTransactionSuccess("TOKEN-${result.tagId.take(8)}")
                        state.copy(
                            isProcessing = false,
                            message = "Redeemed ${result.tokens} tokens! Balance: ${result.wallet.balance}"
                        )
                    }
                    is NfcWalletIntegrationService.ScanResult.TokenPackAlreadyRedeemed -> {
                        state.copy(isProcessing = false, error = "Token pack already redeemed")
                    }
                    is NfcWalletIntegrationService.ScanResult.MerchantTag -> {
                        state.copy(isProcessing = false, merchantId = result.merchantId)
                    }
                    is NfcWalletIntegrationService.ScanResult.UnknownTag -> {
                        state.copy(isProcessing = false, error = "Unknown NFC tag")
                    }
                    else -> state.copy(isProcessing = false)
                }
            }
        }
    }

    fun registerTokenPack(tagId: String, tokenAmount: Long) {
        viewModelScope.launch {
            val success = nfcWalletService.createTokenPackTag(tagId, tokenAmount)
            _uiState.update {
                it.copy(message = if (success) "Token pack registered" else "Failed to register")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun isNfcAvailable(): Boolean = nfcManager.isNfcAvailable()
    fun isNfcSupported(): Boolean = nfcManager.isNfcSupported()
}

data class NfcTerminalUiState(
    val isProcessing: Boolean = false,
    val merchantId: String? = null,
    val message: String? = null,
    val error: String? = null
)
