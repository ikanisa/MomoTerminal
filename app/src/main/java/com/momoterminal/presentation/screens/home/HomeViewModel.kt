package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.config.AppConfig
import com.momoterminal.config.SupportedCountries
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.data.repository.CountryRepository
import com.momoterminal.nfc.NfcManager
import com.momoterminal.nfc.NfcPaymentData
import com.momoterminal.nfc.NfcState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val userPreferences: UserPreferences,
    private val countryRepository: CountryRepository
) : ViewModel() {

    data class HomeUiState(
        val amount: String = "",
        val currency: String = "RWF",
        val currencySymbol: String = "FRw",
        val providerName: String = "",
        val providerCode: String = "MTN",
        val countryCode: String = "RW",
        val countryName: String = "Rwanda",
        val providerName: String = "MTN",
        val providerDisplayName: String = "MTN MoMo",
        val isPaymentActive: Boolean = false,
        // NFC Writer toggle
        val isNfcWriterEnabled: Boolean = true
        val merchantPhone: String = "",
        val isConfigured: Boolean = false,
        val isNfcEnabled: Boolean = true,
        val isNfcAvailable: Boolean = false
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState

    init {
        loadUserConfig()
        checkNfcAvailability()
    }

    private fun loadConfig() {
        val countryCode = appConfig.getCountryCode()
        val country = SupportedCountries.getByCode(countryCode)
        val providerName = appConfig.getMomoProvider()
        
        _uiState.value = _uiState.value.copy(
            isConfigured = appConfig.isConfigured(),
            merchantPhone = appConfig.getMerchantPhone(),
            currency = appConfig.getCurrency(),
            countryCode = countryCode,
            countryName = country?.name ?: "Rwanda",
            providerName = providerName,
            providerDisplayName = getProviderDisplayName(providerName),
            isNfcWriterEnabled = appConfig.isNfcWriterEnabled()
        )
    }
    
    private fun getProviderDisplayName(providerCode: String): String {
        return when (providerCode.uppercase()) {
            "MTN" -> "MTN MoMo"
            "AIRTEL" -> "Airtel Money"
            "VODACOM" -> "M-Pesa"
            "VODAFONE" -> "Vodafone Cash"
            "ORANGE" -> "Orange Money"
            "TIGO" -> "Tigo Pesa"
            "WAVE" -> "Wave"
            "MOOV" -> "Moov Money"
            "ECOCASH" -> "EcoCash"
            "TMONEY" -> "T-Money"
            "MVOLA" -> "MVola"
            else -> providerCode
        }
    }

    private fun observePendingCount() {
    private fun loadUserConfig() {
        viewModelScope.launch {
            userPreferences.userPreferencesFlow.collect { prefs ->
                val country = countryRepository.getByCode(prefs.momoCountryCode)
                    ?: countryRepository.getCurrentCountry()
                
                _uiState.update {
                    it.copy(
                        merchantPhone = prefs.merchantPhone,
                        countryCode = prefs.momoCountryCode.ifEmpty { country.code },
                        currency = country.currency,
                        currencySymbol = country.currencySymbol,
                        providerName = country.providerName,
                        providerCode = country.providerCode,
                        isConfigured = prefs.merchantPhone.isNotBlank()
                    )
                }
            }
        }
    }

    private fun checkNfcAvailability() {
        _uiState.update { it.copy(isNfcAvailable = nfcManager.isNfcAvailable()) }
    }

    fun onDigitClick(digit: String) {
        val currentAmount = _uiState.value.amount
        if (currentAmount.length < 10) {
            _uiState.update { it.copy(amount = currentAmount + digit) }
        }
    }

    fun onBackspaceClick() {
        val currentAmount = _uiState.value.amount
        if (currentAmount.isNotEmpty()) {
            _uiState.update { it.copy(amount = currentAmount.dropLast(1)) }
        }
    }

    fun onClearClick() {
        _uiState.update { it.copy(amount = "") }
    }

    fun toggleNfcWriter(enabled: Boolean) {
        appConfig.setNfcWriterEnabled(enabled)
        _uiState.value = _uiState.value.copy(isNfcWriterEnabled = enabled)
    fun toggleNfcEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isNfcEnabled = enabled) }
    }

    fun activatePayment() {
        val state = _uiState.value
        if (state.amount.isEmpty() || state.merchantPhone.isEmpty()) return
        if (!state.isNfcWriterEnabled) return

        val amountDouble = state.amount.toDoubleOrNull() ?: return
        val amountInMinorUnits = (amountDouble * 100).toLong()
        
        // Get provider from country configuration
        val provider = NfcPaymentData.Provider.fromString(state.providerName)

        val paymentData = NfcPaymentData(
        if (!isAmountValid() || !state.isNfcEnabled) return

        val amountValue = state.amount.toDoubleOrNull() ?: return
        
        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = state.merchantPhone,
            amount = amountValue,
            currency = state.currency,
            countryCode = state.countryCode,
            provider = provider
            provider = NfcPaymentData.Provider.fromString(state.providerCode)
        )
        
        nfcManager.activatePayment(paymentData)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.update { it.copy(amount = "") }
    }

    fun isNfcAvailable(): Boolean = nfcManager.isNfcAvailable() && _uiState.value.isNfcWriterEnabled

    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return false
        return amount > 0
    }

    fun isNfcAvailable(): Boolean = _uiState.value.isNfcAvailable
}
