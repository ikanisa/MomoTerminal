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

    fun onScanStarted() {
        // Logic to prepare for scanning if needed
    }

    fun onScanCancelled() {
        nfcManager.cancelPayment()
    }
}
