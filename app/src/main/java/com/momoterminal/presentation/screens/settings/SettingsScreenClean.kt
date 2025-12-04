package com.momoterminal.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.core.common.config.SupportedCountries
import com.momoterminal.i18n.LanguageSettingsRow
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.SuccessGreen

/**
 * Clean, streamlined Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMomoCountryPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissionStates() }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissionStates() }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissionStates() }
    
    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            snackbarHostState.showSnackbar("Settings saved successfully")
        }
    }
    
    Scaffold(
        topBar = {
            MomoTopAppBar(
                title = stringResource(R.string.settings_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // === PROFILE INFO (Read-only) ===
            ProfileSection(
                phoneNumber = uiState.whatsappNumber,
                country = uiState.profileCountryName
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // === MOBILE MONEY SETUP ===
            MobileMoneySection(
                uiState = uiState,
                onCountryClick = { showMomoCountryPicker = true },
                onPhoneChange = viewModel::updateMomoIdentifier,
                onCodeToggle = viewModel::setUseMomoCode,
                onSave = viewModel::saveSettings
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            // === PERMISSIONS ===
            PermissionsSection(
                permissions = uiState.permissions,
                nfcTerminalEnabled = uiState.isNfcTerminalEnabled,
                onToggleNfc = viewModel::toggleNfcTerminal,
                onRequestSms = { smsPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) },
                onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onRequestNotification = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                onOpenBatterySettings = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            // === APP PREFERENCES ===
            AppPreferencesSection(
                keepScreenOn = uiState.permissions.keepScreenOnEnabled,
                vibration = uiState.permissions.vibrationEnabled,
                biometric = uiState.isBiometricEnabled,
                biometricAvailable = uiState.isBiometricAvailable,
                language = uiState.currentLanguage,
                onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
                onToggleVibration = viewModel::toggleVibration,
                onToggleBiometric = viewModel::toggleBiometric,
                onLanguageChange = viewModel::setLanguage
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            // === ACTIONS ===
            ActionsSection(
                appVersion = uiState.appVersion,
                onLogout = { viewModel.showLogoutDialog() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Dialogs
    if (showMomoCountryPicker) {
        MomoCountryPickerDialog(
            selectedCountryCode = uiState.momoCountryCode,
            onCountrySelected = { 
                viewModel.updateMomoCountry(it)
                showMomoCountryPicker = false
            },
            onDismiss = { showMomoCountryPicker = false }
        )
    }
    
    if (uiState.showLogoutDialog) {
        LogoutDialog(
            onConfirm = { 
                viewModel.logout()
                viewModel.hideLogoutDialog()
                onLogout()
            },
            onDismiss = { viewModel.hideLogoutDialog() }
        )
    }
}

// === SECTION COMPOSABLES ===

@Composable
private fun ProfileSection(phoneNumber: String, country: String) {
    SectionHeader(title = stringResource(R.string.user_profile), icon = Icons.Default.Person)
    Spacer(modifier = Modifier.height(12.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(icon = Icons.Default.Phone, label = "WhatsApp Number", value = phoneNumber.ifBlank { "Not set" })
            InfoRow(icon = Icons.Default.Public, label = "Country", value = country)
        }
    }
}

@Composable
private fun MobileMoneySection(
    uiState: SettingsViewModel.SettingsUiState,
    onCountryClick: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    SectionHeader(title = stringResource(R.string.mobile_money_setup), icon = Icons.Default.AccountBalance)
    Spacer(modifier = Modifier.height(12.dp))
    
    // Country Card
    Card(
        onClick = onCountryClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBalance, null, tint = MomoYellow, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(uiState.momoCountryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${uiState.momoProviderName} • ${uiState.momoCurrency}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Phone/Code Toggle
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !uiState.useMomoCode, onClick = { onCodeToggle(false) }, label = { Text("Phone Number") })
        FilterChip(selected = uiState.useMomoCode, onClick = { onCodeToggle(true) }, label = { Text("MoMo Code") })
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Input Field
    MomoTextField(
        value = uiState.momoIdentifier,
        onValueChange = onPhoneChange,
        label = if (uiState.useMomoCode) "MoMo Code" else "MoMo Number",
        placeholder = if (uiState.useMomoCode) "123456" else uiState.momoPhonePlaceholder,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (uiState.useMomoCode) KeyboardType.Number else KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { if (uiState.isMomoIdentifierValid && uiState.momoIdentifier.isNotBlank()) onSave() }),
        leadingIcon = { Icon(Icons.Default.Phone, null) },
        isError = uiState.momoIdentifier.isNotBlank() && !uiState.isMomoIdentifierValid
    )
    
    if (uiState.momoIdentifier.isNotBlank() && !uiState.isMomoIdentifierValid) {
        Text(
            text = if (uiState.useMomoCode) "MoMo code must be 4-10 digits" else "Invalid phone format",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    MomoButton(
        text = "Save Configuration",
        onClick = onSave,
        enabled = uiState.isMomoIdentifierValid && uiState.momoIdentifier.isNotBlank()
    )
    
    AnimatedVisibility(visible = uiState.isConfigured) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Configuration saved", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
        }
    }
}

@Composable
private fun PermissionsSection(
    permissions: SettingsViewModel.PermissionState,
    nfcTerminalEnabled: Boolean,
    onToggleNfc: () -> Unit,
    onRequestSms: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    SectionHeader(title = "Permissions", icon = Icons.Default.Security)
    Spacer(modifier = Modifier.height(12.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactPermissionItem(icon = Icons.Default.Message, title = "SMS", granted = permissions.smsGranted, onClick = onRequestSms)
        CompactPermissionItem(icon = Icons.Default.CameraAlt, title = "Camera", granted = permissions.cameraGranted, onClick = onRequestCamera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CompactPermissionItem(icon = Icons.Default.Notifications, title = "Notifications", granted = permissions.notificationsGranted, onClick = onRequestNotification)
        }
        CompactPermissionItem(icon = Icons.Default.Nfc, title = "NFC", granted = permissions.nfcEnabled, available = permissions.nfcAvailable, onClick = onOpenNfcSettings)
        CompactPermissionItem(icon = Icons.Default.BatteryChargingFull, title = "Battery", granted = permissions.batteryOptimizationIgnored, onClick = onOpenBatterySettings)
    }
    
    if (permissions.nfcAvailable) {
        Spacer(modifier = Modifier.height(12.dp))
        NfcTerminalToggle(enabled = nfcTerminalEnabled, nfcOn = permissions.nfcEnabled, onToggle = onToggleNfc)
    }
}

@Composable
private fun AppPreferencesSection(
    keepScreenOn: Boolean,
    vibration: Boolean,
    biometric: Boolean,
    biometricAvailable: Boolean,
    language: String,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    SectionHeader(title = "Preferences", icon = Icons.Default.Tune)
    Spacer(modifier = Modifier.height(12.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactToggleItem(icon = Icons.Default.ScreenLockPortrait, title = "Keep Screen On", checked = keepScreenOn, onToggle = onToggleKeepScreenOn)
        CompactToggleItem(icon = Icons.Default.Vibration, title = "Vibration", checked = vibration, onToggle = onToggleVibration)
        if (biometricAvailable) {
            CompactToggleItem(icon = Icons.Default.Fingerprint, title = "Biometric", checked = biometric, onToggle = onToggleBiometric)
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        LanguageSettingsRow(currentLanguage = language, onLanguageChange = onLanguageChange)
    }
}

@Composable
private fun ActionsSection(appVersion: String, onLogout: () -> Unit) {
    SectionHeader(title = "About", icon = Icons.Default.Info)
    Spacer(modifier = Modifier.height(12.dp))
    
    Text("Version $appVersion", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out")
    }
}

// === HELPER COMPOSABLES ===

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CompactPermissionItem(icon: ImageVector, title: String, granted: Boolean, available: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !available -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                granted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (granted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (available) {
                if (granted) {
                    Icon(Icons.Default.CheckCircle, "Granted", tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                } else {
                    TextButton(onClick = onClick) { Text("Enable", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
private fun CompactToggleItem(icon: ImageVector, title: String, checked: Boolean, enabled: Boolean = true, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
        }
    }
}

@Composable
private fun NfcTerminalToggle(enabled: Boolean, nfcOn: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (enabled) SuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Nfc, null, tint = if (enabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("NFC Terminal Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (enabled) "Active - Ready for payments" else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!nfcOn) {
                    Text("⚠️ Enable NFC first", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                enabled = nfcOn,
                colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen, checkedTrackColor = SuccessGreen.copy(alpha = 0.5f))
            )
        }
    }
}

// === DIALOGS ===

@Composable
private fun MomoCountryPickerDialog(selectedCountryCode: String, onCountrySelected: (String) -> Unit, onDismiss: () -> Unit) {
    val allCountries = SupportedCountries.getAllCountries()
    val primaryCountries = allCountries.filter { it.isPrimaryMarket }
    var showAll by remember { mutableStateOf(false) }
    val countries = if (showAll) allCountries else primaryCountries
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Country") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!showAll) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Show All Countries")
                        Icon(Icons.Default.ExpandMore, null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Out") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Log Out") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
