package com.momoterminal.feature.payment.viewmodel

import androidx.lifecycle.viewModelScope
import com.momoterminal.core.common.UiEffect
import com.momoterminal.core.common.UiEvent
import com.momoterminal.core.common.UiState
import com.momoterminal.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor() : BaseViewModel<PaymentState, PaymentEvent, PaymentEffect>(PaymentState()) {

    override fun onEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.UpdateAmount -> updateState { copy(amount = event.amount) }
            is PaymentEvent.InitiatePayment -> initiatePayment()
        }
    }

    private fun initiatePayment() {
        viewModelScope.launch {
            updateState { copy(isProcessing = true, error = null) }
            // Payment logic here
            updateState { copy(isProcessing = false) }
            sendEffect(PaymentEffect.PaymentSuccess)
        }
    }
}

data class PaymentState(
    val amount: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null
) : UiState

sealed class PaymentEvent : UiEvent {
    data class UpdateAmount(val amount: String) : PaymentEvent()
    data object InitiatePayment : PaymentEvent()
}

sealed class PaymentEffect : UiEffect {
    data object PaymentSuccess : PaymentEffect()
}
