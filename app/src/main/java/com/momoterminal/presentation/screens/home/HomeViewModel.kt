package com.momoterminal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.common.preferences.UserPreferences
import com.momoterminal.data.repository.CountryRepository
import com.momoterminal.data.repository.WalletRepository
import com.momoterminal.feature.nfc.NfcManager
import com.momoterminal.core.domain.model.NfcPaymentData
import com.momoterminal.feature.nfc.NfcState
import com.momoterminal.offline.OfflineFirstManager
import com.momoterminal.offline.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val userPreferences: UserPreferences,
    private val countryRepository: CountryRepository,
    private val offlineFirstManager: OfflineFirstManager,
    private val transactionDao: TransactionDao,
    private val walletRepository: WalletRepository,
    private val supabaseAuthService: com.momoterminal.supabase.SupabaseAuthService
) : ViewModel() {

    enum class PaymentMethod {
        NFC, QR_CODE
    }

    data class HomeUiState(
        val amount: String = "",
        val currency: String = "RWF",
        val currencySymbol: String = "FRw",
        val providerName: String = "MTN MoMo",
        val providerCode: String = "MTN",
        val countryCode: String = "RW",
        val merchantPhone: String = "",
        val isConfigured: Boolean = false,
        val isNfcEnabled: Boolean = true,
        val isNfcAvailable: Boolean = false,
        val walletBalance: Long = 0,
        val recentTransactionCount: Int = 0,
        val selectedPaymentMethod: PaymentMethod? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val nfcState: StateFlow<NfcState> = nfcManager.nfcState
    val syncState: StateFlow<SyncState> = offlineFirstManager.syncState
    val pendingCount: StateFlow<Int> = offlineFirstManager.pendingCount

    init {
        loadProfileFromDatabase()
        loadUserConfig()
        checkNfcAvailability()
        observeLocalData()
    }

    private fun loadProfileFromDatabase() {
        viewModelScope.launch {
            try {
                when (val result = supabaseAuthService.getUserProfile()) {
                    is com.momoterminal.supabase.AuthResult.Success -> {
                        val profile = result.data
                        Timber.d("Profile loaded from database: ${profile.phoneNumber}")
                        
                        val country = countryRepository.getByCode(profile.momoCountryCode ?: profile.countryCode ?: "RW")
                            ?: countryRepository.getCurrentCountry()
                        
                        val momoPhone = profile.momoPhone ?: profile.phoneNumber
                        
                        _uiState.update {
                            it.copy(
                                merchantPhone = momoPhone,
                                countryCode = profile.momoCountryCode ?: profile.countryCode ?: "RW",
                                currency = country.currency,
                                currencySymbol = country.currencySymbol,
                                providerName = country.providerName,
                                providerCode = country.providerCode,
                                isConfigured = momoPhone.isNotBlank()
                            )
                        }
                        
                        // Update local cache
                        userPreferences.updateMomoConfig(
                            momoCountryCode = profile.momoCountryCode ?: profile.countryCode ?: "RW",
                            momoIdentifier = momoPhone,
                            useMomoCode = profile.useMomoCode
                        )
                    }
                    is com.momoterminal.supabase.AuthResult.Error -> {
                        Timber.w("Failed to load profile from database: ${result.message}, falling back to local cache")
                        loadUserConfig()
                    }
                    else -> {
                        loadUserConfig()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading profile from database, falling back to local cache")
                loadUserConfig()
            }
        }
    }

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

    private fun observeLocalData() {
        // Observe wallet balance (offline-first: always available)
        viewModelScope.launch {
            userPreferences.userPreferencesFlow.flatMapLatest { prefs ->
                val userId = prefs.merchantPhone.ifBlank { prefs.phoneNumber.ifBlank { "default" } }
                if (userId.isNotBlank()) {
                    walletRepository.observeWallet(userId).map { it?.balance ?: 0 }
                } else {
                    flowOf(0L)
                }
            }.collect { balance ->
                _uiState.update { it.copy(walletBalance = balance) }
            }
        }

        // Observe recent transaction count
        viewModelScope.launch {
            transactionDao.getRecentTransactions(10).collect { transactions ->
                _uiState.update { it.copy(recentTransactionCount = transactions.size) }
            }
        }
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

    fun toggleNfcEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isNfcEnabled = enabled) }
    }

    fun activatePaymentWithMethod(method: PaymentMethod) {
        val state = _uiState.value
        if (!isAmountValid()) return
        
        // Check if mobile money number is configured
        if (state.merchantPhone.isBlank()) {
            // Show error - merchant phone not configured
            Timber.w("Cannot activate payment: Mobile Money number not configured")
            return
        }
        
        // Only check NFC availability for NFC payment method
        if (method == PaymentMethod.NFC && !state.isNfcEnabled) return

        val amountValue = state.amount.toDoubleOrNull() ?: return
        
        // Store selected payment method
        _uiState.update { it.copy(selectedPaymentMethod = method) }

        val paymentData = NfcPaymentData.fromAmount(
            merchantPhone = state.merchantPhone,
            amount = amountValue,
            currency = state.currency,
            countryCode = state.countryCode,
            provider = NfcPaymentData.Provider.fromString(state.providerCode)
        )

        // Only activate NFC manager for NFC payments
        // QR code doesn't need NFC hardware
        if (method == PaymentMethod.NFC) {
            nfcManager.activatePayment(paymentData)
        } else {
            // For QR code, just store the data and show the QR
            // The UI will render QR code based on selectedPaymentMethod
        }
    }

    fun activatePayment() {
        // Default to NFC for backward compatibility
        activatePaymentWithMethod(PaymentMethod.NFC)
    }

    fun cancelPayment() {
        nfcManager.cancelPayment()
        _uiState.update { it.copy(amount = "", selectedPaymentMethod = null) }
    }

    fun triggerSync() {
        offlineFirstManager.triggerImmediateSync()
    }

    fun isAmountValid(): Boolean {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return false
        return amount > 0
    }

    fun isNfcAvailable(): Boolean = _uiState.value.isNfcAvailable
}
