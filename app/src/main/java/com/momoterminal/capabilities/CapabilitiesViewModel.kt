package com.momoterminal.capabilities

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for the Capabilities Demo screen.
 * 
 * Manages the state and business logic for demonstrating various Android app capabilities:
 * - Network status and HTTP requests
 * - NFC availability
 * - Vibration
 * - Biometric authentication
 * - Screen wake / WakeLock
 * - Install Referrer
 * - Advertising ID
 * - Foreground service status
 * - Boot completed status
 */
@HiltViewModel
class CapabilitiesViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // OkHttp client for making HTTP requests
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // UI State
    private val _uiState = MutableStateFlow(CapabilitiesUiState())
    val uiState: StateFlow<CapabilitiesUiState> = _uiState.asStateFlow()

    // WakeLock for preventing device from sleeping
    private var wakeLock: PowerManager.WakeLock? = null

    // Network callback for observing network changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateNetworkStatus()
        }
    }

    init {
        // Register network callback
        registerNetworkCallback()
        
        // Initial state updates
        updateNetworkStatus()
        updateNfcStatus()
        updateBiometricStatus()
        updateBootCompletedStatus()
        
        // Observe foreground service status
        observeForegroundServiceStatus()
    }

    /**
     * Registers a NetworkCallback to observe real-time network changes.
     * This demonstrates the VIEW NETWORK CONNECTIONS capability.
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Timber.d("Network callback registered")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register network callback")
        }
    }

    /**
     * Updates the network status in the UI state.
     * Detects whether the device is on Wi-Fi, Mobile data, or Offline.
     */
    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val networkStatus = when {
            capabilities == null -> NetworkStatus.OFFLINE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkStatus.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkStatus.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus.ETHERNET
            else -> NetworkStatus.UNKNOWN
        }

        _uiState.update { it.copy(networkStatus = networkStatus) }
        Timber.d("Network status updated: $networkStatus")
    }

    /**
     * Updates NFC availability status.
     * Checks if NFC hardware is present and enabled.
     */
    private fun updateNfcStatus() {
        val nfcAdapter = nfcManager?.defaultAdapter ?: NfcAdapter.getDefaultAdapter(context)
        val nfcStatus = when {
            nfcAdapter == null -> NfcStatus.NOT_SUPPORTED
            !nfcAdapter.isEnabled -> NfcStatus.DISABLED
            else -> NfcStatus.ENABLED
        }

        _uiState.update { it.copy(nfcStatus = nfcStatus) }
        Timber.d("NFC status updated: $nfcStatus")
    }

    /**
     * Updates biometric authentication availability.
     * Checks if biometric hardware (fingerprint/face) is available and enrolled.
     */
    private fun updateBiometricStatus() {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        )

        val biometricStatus = when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNKNOWN
        }

        _uiState.update { it.copy(biometricStatus = biometricStatus) }
        Timber.d("Biometric status updated: $biometricStatus")
    }

    /**
     * Updates boot completed receiver status from SharedPreferences.
     */
    private fun updateBootCompletedStatus() {
        val prefs = context.getSharedPreferences("capabilities_demo_prefs", Context.MODE_PRIVATE)
        val bootReceiverTriggered = prefs.getBoolean("boot_receiver_triggered", false)
        val lastBootTime = prefs.getLong("last_boot_time", 0L)
        val bootWorkRunCount = prefs.getInt("boot_work_run_count", 0)

        val bootInfo = if (bootReceiverTriggered && lastBootTime > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            "Last boot: ${dateFormat.format(Date(lastBootTime))}\nBoot work run count: $bootWorkRunCount"
        } else {
            "Boot receiver has not been triggered yet.\nRestart your device to test this feature."
        }

        _uiState.update { it.copy(bootCompletedInfo = bootInfo) }
    }

    /**
     * Observes the foreground service status from the service's StateFlow.
     */
    private fun observeForegroundServiceStatus() {
        viewModelScope.launch {
            CapabilitiesForegroundService.isRunning.collect { isRunning ->
                _uiState.update { it.copy(foregroundServiceRunning = isRunning) }
            }
        }
        viewModelScope.launch {
            CapabilitiesForegroundService.lastUpdate.collect { lastUpdate ->
                _uiState.update { it.copy(foregroundServiceLastUpdate = lastUpdate) }
            }
        }
    }

    /**
     * Makes an HTTP GET request to a public API.
     * Demonstrates the FULL NETWORK ACCESS capability.
     */
    fun makeHttpRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(httpRequestLoading = true, httpRequestResult = null) }

            try {
                val result = withContext(Dispatchers.IO) {
                    // Using httpbin.org which is a simple HTTP testing service
                    val request = Request.Builder()
                        .url("https://httpbin.org/get")
                        .get()
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: "Empty response"
                            // Truncate for display
                            if (body.length > 500) {
                                body.take(500) + "..."
                            } else {
                                body
                            }
                        } else {
                            "HTTP Error: ${response.code}"
                        }
                    }
                }

                _uiState.update { 
                    it.copy(
                        httpRequestLoading = false,
                        httpRequestResult = result
                    )
                }
                Timber.d("HTTP request successful")
            } catch (e: Exception) {
                Timber.e(e, "HTTP request failed")
                _uiState.update { 
                    it.copy(
                        httpRequestLoading = false,
                        httpRequestResult = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Triggers device vibration.
     * Demonstrates the CONTROL VIBRATION capability.
     * 
     * Uses VibratorManager on Android 12+ and Vibrator on older versions.
     */
    // Suppressing MissingPermission because android.permission.VIBRATE is declared
    // in AndroidManifest.xml and is a normal permission (auto-granted at install time)
    @SuppressLint("MissingPermission")
    fun testVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ uses VibratorManager
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                // Older versions use Vibrator directly
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                // Create a short vibration pattern: vibrate 100ms, pause 100ms, vibrate 100ms
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ uses VibrationEffect
                    val pattern = longArrayOf(0, 100, 100, 100, 100, 200)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                } else {
                    // Older versions use deprecated method
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 200), -1)
                }

                _uiState.update { it.copy(vibrationResult = "Vibration triggered successfully!") }
                Timber.d("Vibration triggered")
            } else {
                _uiState.update { it.copy(vibrationResult = "Device does not have a vibrator") }
                Timber.w("Device does not have a vibrator")
            }
        } catch (e: Exception) {
            Timber.e(e, "Vibration failed")
            _uiState.update { it.copy(vibrationResult = "Vibration failed: ${e.message}") }
        }
    }

    /**
     * Updates biometric authentication result.
     * Called from the UI after BiometricPrompt completes.
     */
    fun onBiometricResult(success: Boolean, message: String) {
        _uiState.update { 
            it.copy(biometricAuthResult = if (success) "✓ $message" else "✗ $message")
        }
    }

    /**
     * Acquires a WakeLock to prevent the device from sleeping.
     * Demonstrates the PREVENT PHONE FROM SLEEPING capability.
     * 
     * IMPORTANT: Always release WakeLocks when no longer needed to preserve battery.
     * 
     * Note: For this demo, we acquire with a 10-minute timeout to prevent indefinite
     * battery drain if the user forgets to release it. In production apps, you should:
     * - Use the shortest timeout that meets your requirements
     * - Release the WakeLock as soon as possible
     * - Consider using WorkManager for background tasks instead
     */
    fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            Timber.d("WakeLock already held")
            return
        }

        // Acquire with a 10-minute timeout (600,000 ms) to prevent indefinite battery drain
        // The WakeLock will automatically release after this timeout if not explicitly released
        val timeoutMs = 10 * 60 * 1000L // 10 minutes
        
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MomoTerminal:CapabilitiesDemoWakeLock"
        ).apply {
            acquire(timeoutMs)
        }

        _uiState.update { it.copy(wakeLockHeld = true) }
        Timber.d("WakeLock acquired with ${timeoutMs / 1000}s timeout")
    }

    /**
     * Releases the WakeLock, allowing the device to sleep normally.
     */
    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
            _uiState.update { it.copy(wakeLockHeld = false) }
            Timber.d("WakeLock released")
        }
    }

    /**
     * Starts the foreground service.
     * Demonstrates the RUN FOREGROUND SERVICE capability.
     */
    fun startForegroundService() {
        CapabilitiesForegroundService.start(context)
        Timber.d("Foreground service start requested")
    }

    /**
     * Stops the foreground service.
     */
    fun stopForegroundService() {
        CapabilitiesForegroundService.stop(context)
        Timber.d("Foreground service stop requested")
    }

    /**
     * Fetches the Install Referrer information.
     * Demonstrates the PLAY INSTALL REFERRER API capability.
     * 
     * The Install Referrer API provides referrer information about which campaign
     * or source led the user to install the app from the Google Play Store.
     */
    fun fetchInstallReferrer() {
        _uiState.update { it.copy(installReferrerLoading = true) }

        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val referrerDetails = referrerClient.installReferrer
                            val referrerInfo = buildString {
                                appendLine("Referrer URL: ${referrerDetails.installReferrer}")
                                appendLine("Click Timestamp: ${referrerDetails.referrerClickTimestampSeconds}")
                                appendLine("Install Timestamp: ${referrerDetails.installBeginTimestampSeconds}")
                                appendLine("Click Server Time: ${referrerDetails.referrerClickTimestampServerSeconds}")
                                appendLine("Install Server Time: ${referrerDetails.installBeginTimestampServerSeconds}")
                                appendLine("Install Version: ${referrerDetails.installVersion}")
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    installReferrerLoading = false,
                                    installReferrerResult = referrerInfo
                                )
                            }
                            Timber.d("Install referrer fetched successfully")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to get referrer details")
                            _uiState.update { 
                                it.copy(
                                    installReferrerLoading = false,
                                    installReferrerResult = "Error: ${e.message}"
                                )
                            }
                        } finally {
                            referrerClient.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        _uiState.update { 
                            it.copy(
                                installReferrerLoading = false,
                                installReferrerResult = "Install Referrer API not supported on this device"
                            )
                        }
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        _uiState.update { 
                            it.copy(
                                installReferrerLoading = false,
                                installReferrerResult = "Google Play services not available"
                            )
                        }
                        referrerClient.endConnection()
                    }
                    else -> {
                        _uiState.update { 
                            it.copy(
                                installReferrerLoading = false,
                                installReferrerResult = "Unknown error (code: $responseCode)"
                            )
                        }
                        referrerClient.endConnection()
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Timber.d("Install referrer service disconnected")
            }
        })
    }

    /**
     * Fetches the Google Advertising ID.
     * Demonstrates the ADVERTISING ID PERMISSION capability.
     * 
     * IMPORTANT PRIVACY NOTES:
     * 1. The Advertising ID is used for advertising and analytics purposes.
     * 2. Users can reset or disable their Advertising ID in device settings.
     * 3. In a real app, you MUST:
     *    - Inform users how you use the Advertising ID
     *    - Obtain consent where required by law (e.g., GDPR)
     *    - Respect the "Limit Ad Tracking" preference
     *    - Never link the Advertising ID to personally identifiable information
     *    - Have a clear privacy policy explaining data collection
     * 4. The AD_ID permission is required for apps targeting Android 13+.
     */
    fun fetchAdvertisingId() {
        _uiState.update { it.copy(advertisingIdLoading = true) }

        viewModelScope.launch {
            try {
                val adInfo = withContext(Dispatchers.IO) {
                    // This MUST be called on a background thread
                    AdvertisingIdClient.getAdvertisingIdInfo(context)
                }

                val limitAdTracking = adInfo.isLimitAdTrackingEnabled
                val advertisingId = if (limitAdTracking) {
                    // User has opted out of ad tracking - respect their preference
                    "User has limited ad tracking (ID not shown)"
                } else {
                    // Mask part of the ID for privacy in demo
                    val id = adInfo.id ?: "Not available"
                    if (id.length > 8) {
                        "${id.take(8)}****-****-****-************"
                    } else {
                        id
                    }
                }

                val result = buildString {
                    appendLine("Advertising ID: $advertisingId")
                    appendLine("Limit Ad Tracking: $limitAdTracking")
                    appendLine()
                    appendLine("⚠️ PRIVACY NOTICE:")
                    appendLine("In production apps, you must:")
                    appendLine("• Obtain user consent (GDPR, etc.)")
                    appendLine("• Respect 'Limit Ad Tracking' preference")
                    appendLine("• Have a clear privacy policy")
                    appendLine("• Never link to personal data")
                }

                _uiState.update { 
                    it.copy(
                        advertisingIdLoading = false,
                        advertisingIdResult = result
                    )
                }
                Timber.d("Advertising ID fetched (limit tracking: $limitAdTracking)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Advertising ID")
                _uiState.update { 
                    it.copy(
                        advertisingIdLoading = false,
                        advertisingIdResult = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears cached results for a fresh demo.
     */
    fun clearResults() {
        _uiState.update { 
            it.copy(
                httpRequestResult = null,
                vibrationResult = null,
                biometricAuthResult = null,
                installReferrerResult = null,
                advertisingIdResult = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister network callback
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister network callback")
        }
        
        // Release WakeLock if held
        releaseWakeLock()
    }
}

/**
 * UI state for the Capabilities Demo screen.
 */
data class CapabilitiesUiState(
    // Network
    val networkStatus: NetworkStatus = NetworkStatus.UNKNOWN,
    val httpRequestLoading: Boolean = false,
    val httpRequestResult: String? = null,
    
    // NFC
    val nfcStatus: NfcStatus = NfcStatus.UNKNOWN,
    val lastNfcTag: String? = null,
    
    // Vibration
    val vibrationResult: String? = null,
    
    // Biometric
    val biometricStatus: BiometricStatus = BiometricStatus.UNKNOWN,
    val biometricAuthResult: String? = null,
    
    // Screen Wake
    val wakeLockHeld: Boolean = false,
    
    // Boot Completed
    val bootCompletedInfo: String = "Checking...",
    
    // Foreground Service
    val foregroundServiceRunning: Boolean = false,
    val foregroundServiceLastUpdate: String = "Service not started",
    
    // Install Referrer
    val installReferrerLoading: Boolean = false,
    val installReferrerResult: String? = null,
    
    // Advertising ID
    val advertisingIdLoading: Boolean = false,
    val advertisingIdResult: String? = null
)

enum class NetworkStatus {
    WIFI, MOBILE, ETHERNET, OFFLINE, UNKNOWN
}

enum class NfcStatus {
    ENABLED, DISABLED, NOT_SUPPORTED, UNKNOWN
}

enum class BiometricStatus {
    AVAILABLE, NO_HARDWARE, HARDWARE_UNAVAILABLE, NOT_ENROLLED, SECURITY_UPDATE_REQUIRED, UNKNOWN
}
