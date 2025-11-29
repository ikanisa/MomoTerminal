package com.momoterminal.capabilities

import android.app.Activity
import android.view.WindowManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Capabilities Demo Screen
 * 
 * This screen demonstrates various Android app capabilities including:
 * - Network status monitoring and HTTP requests
 * - NFC availability checking
 * - Vibration control
 * - Biometric authentication
 * - Screen wake / WakeLock management
 * - Boot completed receiver status
 * - Foreground service controls
 * - Install Referrer API
 * - Advertising ID fetching
 * 
 * Each section includes clear explanations and interactive demos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilitiesDemoScreen(
    onNavigateBack: () -> Unit,
    viewModel: CapabilitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Handle screen wake flag using WindowManager
    DisposableEffect(uiState.wakeLockHeld) {
        if (uiState.wakeLockHeld && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Capabilities Demo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearResults() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Results"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Network Section
            NetworkSection(
                networkStatus = uiState.networkStatus,
                httpRequestLoading = uiState.httpRequestLoading,
                httpRequestResult = uiState.httpRequestResult,
                onMakeHttpRequest = { viewModel.makeHttpRequest() }
            )

            // NFC Section
            NfcSection(nfcStatus = uiState.nfcStatus)

            // Vibration Section
            VibrationSection(
                vibrationResult = uiState.vibrationResult,
                onTestVibration = { viewModel.testVibration() }
            )

            // Biometric Section
            BiometricSection(
                biometricStatus = uiState.biometricStatus,
                biometricAuthResult = uiState.biometricAuthResult,
                onBiometricResult = { success, message -> 
                    viewModel.onBiometricResult(success, message) 
                }
            )

            // Screen Wake Section
            ScreenWakeSection(
                wakeLockHeld = uiState.wakeLockHeld,
                onToggleWakeLock = { enabled ->
                    if (enabled) viewModel.acquireWakeLock() else viewModel.releaseWakeLock()
                }
            )

            // Foreground Service Section
            ForegroundServiceSection(
                isRunning = uiState.foregroundServiceRunning,
                lastUpdate = uiState.foregroundServiceLastUpdate,
                onStart = { viewModel.startForegroundService() },
                onStop = { viewModel.stopForegroundService() }
            )

            // Boot Completed Section
            BootCompletedSection(bootInfo = uiState.bootCompletedInfo)

            // Install Referrer Section
            InstallReferrerSection(
                isLoading = uiState.installReferrerLoading,
                result = uiState.installReferrerResult,
                onFetch = { viewModel.fetchInstallReferrer() }
            )

            // Advertising ID Section
            AdvertisingIdSection(
                isLoading = uiState.advertisingIdLoading,
                result = uiState.advertisingIdResult,
                onFetch = { viewModel.fetchAdvertisingId() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CapabilityCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun NetworkSection(
    networkStatus: NetworkStatus,
    httpRequestLoading: Boolean,
    httpRequestResult: String?,
    onMakeHttpRequest: () -> Unit
) {
    CapabilityCard(
        title = "Network Access",
        icon = when (networkStatus) {
            NetworkStatus.WIFI -> Icons.Default.Wifi
            NetworkStatus.MOBILE -> Icons.Default.SignalCellularAlt
            NetworkStatus.OFFLINE -> Icons.Default.SignalWifiOff
            else -> Icons.Default.Wifi
        }
    ) {
        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val (statusText, statusColor) = when (networkStatus) {
                NetworkStatus.WIFI -> "Online via Wi-Fi" to Color(0xFF4CAF50)
                NetworkStatus.MOBILE -> "Online via Mobile Data" to Color(0xFF2196F3)
                NetworkStatus.ETHERNET -> "Online via Ethernet" to Color(0xFF4CAF50)
                NetworkStatus.OFFLINE -> "Offline" to Color(0xFFF44336)
                NetworkStatus.UNKNOWN -> "Unknown" to Color(0xFFFF9800)
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (networkStatus != NetworkStatus.OFFLINE) 
                        Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Required permissions:\n• INTERNET\n• ACCESS_NETWORK_STATE",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onMakeHttpRequest,
            enabled = !httpRequestLoading && networkStatus != NetworkStatus.OFFLINE,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (httpRequestLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (httpRequestLoading) "Loading..." else "Test HTTP GET Request")
        }

        AnimatedVisibility(visible = httpRequestResult != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Response:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = httpRequestResult ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun NfcSection(nfcStatus: NfcStatus) {
    CapabilityCard(
        title = "NFC Control",
        icon = Icons.Default.Nfc
    ) {
        val (statusText, statusColor) = when (nfcStatus) {
            NfcStatus.ENABLED -> "NFC is available and enabled" to Color(0xFF4CAF50)
            NfcStatus.DISABLED -> "NFC is available but disabled" to Color(0xFFFF9800)
            NfcStatus.NOT_SUPPORTED -> "NFC is not supported on this device" to Color(0xFFF44336)
            NfcStatus.UNKNOWN -> "NFC status unknown" to Color(0xFF9E9E9E)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (nfcStatus == NfcStatus.ENABLED) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Required in manifest:\n• android.permission.NFC\n• <uses-feature android:name=\"android.hardware.nfc\" android:required=\"false\"/>",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (nfcStatus == NfcStatus.DISABLED) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 Enable NFC in your device settings to use NFC features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VibrationSection(
    vibrationResult: String?,
    onTestVibration: () -> Unit
) {
    CapabilityCard(
        title = "Vibration Control",
        icon = Icons.Default.Vibration
    ) {
        Text(
            text = "Required permission: android.permission.VIBRATE",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onTestVibration,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Vibration")
        }

        AnimatedVisibility(visible = vibrationResult != null) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = vibrationResult ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (vibrationResult?.contains("success") == true) 
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BiometricSection(
    biometricStatus: BiometricStatus,
    biometricAuthResult: String?,
    onBiometricResult: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity

    CapabilityCard(
        title = "Biometric Authentication",
        icon = Icons.Default.Fingerprint
    ) {
        val (statusText, statusColor) = when (biometricStatus) {
            BiometricStatus.AVAILABLE -> "Biometric authentication available" to Color(0xFF4CAF50)
            BiometricStatus.NO_HARDWARE -> "No biometric hardware found" to Color(0xFFF44336)
            BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware unavailable" to Color(0xFFFF9800)
            BiometricStatus.NOT_ENROLLED -> "No biometrics enrolled" to Color(0xFFFF9800)
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Security update required" to Color(0xFFFF9800)
            BiometricStatus.UNKNOWN -> "Status unknown" to Color(0xFF9E9E9E)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (biometricStatus == BiometricStatus.AVAILABLE) 
                    Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Required permissions:\n• USE_BIOMETRIC\n• USE_FINGERPRINT (legacy)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                fragmentActivity?.let { activity ->
                    showBiometricPrompt(activity, onBiometricResult)
                }
            },
            enabled = biometricStatus == BiometricStatus.AVAILABLE && fragmentActivity != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Biometric Login")
        }

        AnimatedVisibility(visible = biometricAuthResult != null) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = biometricAuthResult ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (biometricAuthResult?.startsWith("✓") == true) 
                        Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onResult: (Boolean, String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(true, "Authentication successful!")
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onResult(false, "Error: $errString")
        }

        override fun onAuthenticationFailed() {
            onResult(false, "Authentication failed - try again")
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Authenticate using your fingerprint or face")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
private fun ScreenWakeSection(
    wakeLockHeld: Boolean,
    onToggleWakeLock: (Boolean) -> Unit
) {
    CapabilityCard(
        title = "Prevent Phone from Sleeping",
        icon = Icons.Default.PhoneAndroid
    ) {
        Text(
            text = "Demonstrates two approaches:\n" +
                    "1. FLAG_KEEP_SCREEN_ON (for activities)\n" +
                    "2. WakeLock via PowerManager (for background)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Required permission: android.permission.WAKE_LOCK",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep screen awake",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (wakeLockHeld) "Screen will stay on" else "Normal sleep behavior",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = wakeLockHeld,
                onCheckedChange = onToggleWakeLock
            )
        }

        if (wakeLockHeld) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ WakeLock is held - this uses extra battery!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun ForegroundServiceSection(
    isRunning: Boolean,
    lastUpdate: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    CapabilityCard(
        title = "Foreground Service",
        icon = Icons.Default.Notifications
    ) {
        Text(
            text = "Required permissions:\n" +
                    "• FOREGROUND_SERVICE\n" +
                    "• FOREGROUND_SERVICE_DATA_SYNC\n" +
                    "• POST_NOTIFICATIONS (Android 13+)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "Service is running" else "Service is stopped",
                fontWeight = FontWeight.Medium,
                color = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = lastUpdate,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = !isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Service")
            }
            OutlinedButton(
                onClick = onStop,
                enabled = isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Service")
            }
        }
    }
}

@Composable
private fun BootCompletedSection(bootInfo: String) {
    CapabilityCard(
        title = "Run at Startup",
        icon = Icons.Default.PowerSettingsNew
    ) {
        Text(
            text = "Required permission: RECEIVE_BOOT_COMPLETED\n\n" +
                    "The app registers a BroadcastReceiver that listens for BOOT_COMPLETED. " +
                    "When the device boots, it schedules a WorkManager job for initialization.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = bootInfo,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InstallReferrerSection(
    isLoading: Boolean,
    result: String?,
    onFetch: () -> Unit
) {
    CapabilityCard(
        title = "Play Install Referrer API",
        icon = Icons.Default.Bluetooth // Using as placeholder for referrer icon
    ) {
        Text(
            text = "Fetches referrer information from Google Play Store " +
                    "to track which campaign or source led to the app install.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onFetch,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Fetching..." else "Fetch Install Referrer")
        }

        AnimatedVisibility(visible = result != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AdvertisingIdSection(
    isLoading: Boolean,
    result: String?,
    onFetch: () -> Unit
) {
    CapabilityCard(
        title = "Advertising ID",
        icon = Icons.Default.Bluetooth // Using as placeholder
    ) {
        Text(
            text = "Required permission: com.google.android.gms.permission.AD_ID\n\n" +
                    "⚠️ PRIVACY IMPORTANT: In production apps, you must obtain user consent " +
                    "and comply with privacy laws (GDPR, CCPA, etc.) before collecting this data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onFetch,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Fetching..." else "Fetch Advertising ID")
        }

        AnimatedVisibility(visible = result != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
