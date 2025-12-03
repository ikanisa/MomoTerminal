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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.config.SupportedCountries
import com.momoterminal.i18n.LanguageSettingsRow
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.SuccessGreen

/**
 * Clean Settings screen with:
 * - User Profile section (showing WhatsApp registration info)
 * - Mobile Money Setup section (with country selection independent from profile)
 * - Security section
 * - About section
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
    val uriHandler = LocalUriHandler.current
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
                .padding(24.dp)
        ) {
            // User Profile Section
            SectionHeader(
                title = stringResource(R.string.user_profile),
                icon = Icons.Default.Person
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Info Card (read-only, from WhatsApp registration)
            ProfileInfoCard(
                phoneNumber = uiState.authPhone,
                profileCountry = uiState.profileCountryName
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(32.dp))

            // Mobile Money Setup Section
            SectionHeader(
                title = stringResource(R.string.mobile_money_setup),
                icon = Icons.Default.AccountBalance
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text about separate country
            Text(
                text = stringResource(R.string.momo_country_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Mobile Money Country Selector
            MomoCountryCard(
                countryName = uiState.momoCountryName,
                currency = uiState.momoCurrency,
                providerName = uiState.momoProviderName,
                onClick = { showMomoCountryPicker = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mobile Money Phone Number
            MomoTextField(
                value = uiState.merchantPhone,
                onValueChange = viewModel::updateMerchantPhone,
                label = stringResource(R.string.mobile_money_number),
                placeholder = stringResource(R.string.mobile_money_number_placeholder),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = uiState.merchantPhone.isNotBlank() && !viewModel.isPhoneValid()
            )
            // ==================== PERMISSIONS SECTION ====================
            SectionHeader(title = "Permissions & Controls", icon = Icons.Default.Security)
            Spacer(modifier = Modifier.height(16.dp))
            
            // SMS Permission
            PermissionItem(
                icon = Icons.Default.Message,
                title = "SMS Access",
                description = if (uiState.permissions.smsGranted) "Granted - Can receive MoMo SMS" else "Required for SMS relay",
                isGranted = uiState.permissions.smsGranted,
                onRequestPermission = {
                    smsPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                }
            )
            
            // NFC Control
            PermissionItem(
                icon = Icons.Default.Nfc,
                title = "NFC Control",
                description = when {
                    !uiState.permissions.nfcAvailable -> "Not available on this device"
                    uiState.permissions.nfcEnabled -> "Enabled - Ready for tap payments"
                    else -> "Disabled - Enable in system settings"
                },
                isGranted = uiState.permissions.nfcEnabled,
                isAvailable = uiState.permissions.nfcAvailable,
                onRequestPermission = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
            )
            
            // Camera Permission
            PermissionItem(
                icon = Icons.Default.CameraAlt,
                title = "Camera Access",
                description = if (uiState.permissions.cameraGranted) "Granted - Can scan QR codes" else "Required for QR scanning",
                isGranted = uiState.permissions.cameraGranted,
                onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
            
            // Notifications (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = if (uiState.permissions.notificationsGranted) "Granted - Will receive alerts" else "Required for payment alerts",
                    isGranted = uiState.permissions.notificationsGranted,
                    onRequestPermission = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }
            
            // Battery Optimization
            PermissionItem(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery Optimization",
                description = if (uiState.permissions.batteryOptimizationIgnored) "Unrestricted - App won't be killed" else "Restricted - May miss SMS in background",
                isGranted = uiState.permissions.batteryOptimizationIgnored,
                onRequestPermission = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== APP CONTROLS ====================
            SectionHeader(title = "App Controls", icon = Icons.Default.Tune)
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsToggle(
                icon = Icons.Default.ScreenLockPortrait,
                title = "Keep Screen On",
                description = "Prevent phone from sleeping during transactions",
                checked = uiState.permissions.keepScreenOnEnabled,
                onCheckedChange = viewModel::toggleKeepScreenOn
            )
            
            SettingsToggle(
                icon = Icons.Default.Vibration,
                title = "Vibration Feedback",
                description = "Vibrate on payment received",
                checked = uiState.permissions.vibrationEnabled,
                onCheckedChange = viewModel::toggleVibration
            )
            
            // About Section
            SectionHeader(
                title = stringResource(R.string.about),
                icon = Icons.Default.Info
            )
            SettingsToggle(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Login",
                description = if (uiState.isBiometricAvailable) "Use fingerprint or face to unlock" else "Not available on this device",
                checked = uiState.isBiometricEnabled,
                onCheckedChange = viewModel::toggleBiometric,
                enabled = uiState.isBiometricAvailable
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Language Selection
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                LanguageSettingsRow(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = viewModel::setLanguage
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== MERCHANT PROFILE ====================
            SectionHeader(title = stringResource(R.string.merchant_profile), icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display registered WhatsApp number (read-only)
            if (uiState.whatsappNumber.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.registered_number), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uiState.whatsappNumber, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // MoMo Input Type Selector (Phone or Code)
            Text(stringResource(R.string.momo_identifier_type), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !uiState.useMomoCode,
                    onClick = { viewModel.setUseMomoCode(false) },
                    label = { Text(stringResource(R.string.momo_phone_number)) }
                )
                FilterChip(
                    selected = uiState.useMomoCode,
                    onClick = { viewModel.setUseMomoCode(true) },
                    label = { Text(stringResource(R.string.momo_code)) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // MoMo Phone Number or Code input (no country prefix)
            MomoTextField(
                value = uiState.momoIdentifier,
                onValueChange = viewModel::updateMomoIdentifier,
                label = if (uiState.useMomoCode) stringResource(R.string.momo_code) else stringResource(R.string.momo_phone_number),
                placeholder = if (uiState.useMomoCode) "123456" else uiState.momoPhonePlaceholder,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (uiState.useMomoCode) KeyboardType.Number else KeyboardType.Phone),
                isError = uiState.momoIdentifier.isNotBlank() && !uiState.isMomoIdentifierValid
            )
            
            if (uiState.momoIdentifier.isNotBlank() && !uiState.isMomoIdentifierValid) {
                Text(
                    text = if (uiState.useMomoCode) stringResource(R.string.invalid_momo_code) else stringResource(R.string.invalid_phone_format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Country Selector
            if (uiState.availableCountries.isNotEmpty()) {
                Text(stringResource(R.string.momo_country), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.availableCountries.forEach { country ->
                    val isSelected = country.code == uiState.momoCountryCode
                    Card(
                        onClick = { viewModel.updateMomoCountry(country.code) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(country.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                Text("${country.currency} • ${country.providerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== ABOUT ====================
            SectionHeader(title = stringResource(R.string.about), icon = Icons.Default.Info)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.app_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uiState.appVersion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { uriHandler.openUri("https://momoterminal.app/privacy") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_policy), modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
            
            // Terms of Service Link
            TextButton(
                onClick = { uriHandler.openUri("https://momoterminal.app/terms") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.terms_of_service),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = { uriHandler.openUri("https://momoterminal.app/terms") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.terms_of_service), modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            MomoButton(
                text = stringResource(R.string.save_configuration),
                onClick = { viewModel.saveSettings() },
                enabled = uiState.isMomoPhoneValid
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { viewModel.showLogoutDialog() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.logout))
            }
            
            AnimatedVisibility(visible = uiState.isConfigured) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.configuration_saved), style = MaterialTheme.typography.bodyMedium, color = SuccessGreen, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Mobile Money Country Picker Dialog
    if (showMomoCountryPicker) {
        MomoCountryPickerDialog(
            selectedCountryCode = uiState.momoCountryCode,
            onCountrySelected = { code ->
                viewModel.updateMomoCountryCode(code)
                showMomoCountryPicker = false
            },
            onDismiss = { showMomoCountryPicker = false }
        )
    }
    
    // Logout Confirmation Dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(); viewModel.hideLogoutDialog(); onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = { TextButton(onClick = { viewModel.hideLogoutDialog() }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/**
 * Profile info card showing WhatsApp registration details.
 */
@Composable
private fun ProfileInfoCard(
    phoneNumber: String,
    profileCountry: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.whatsapp_number),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = phoneNumber.ifBlank { stringResource(R.string.not_set) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.profile_country),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profileCountry,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Mobile Money country card with provider info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MomoCountryCard(
    countryName: String,
    currency: String,
    providerName: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MomoYellow,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = countryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$providerName • $currency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.change_country),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Mobile Money country picker dialog.
 */
@Composable
private fun MomoCountryPickerDialog(
    selectedCountryCode: String,
    onCountrySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val countries = SupportedCountries.getAllCountries()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.select_momo_country))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                countries.forEach { country ->
                    val isSelected = country.code == selectedCountryCode
                    Card(
                        onClick = { onCountrySelected(country.code) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = country.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = "${country.providerName} • ${country.currency}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isAvailable: Boolean = true,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                isGranted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = when {
                !isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isGranted -> SuccessGreen
                else -> MaterialTheme.colorScheme.error
            })
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isAvailable && !isGranted) {
                TextButton(onClick = onRequestPermission) { Text("Grant") }
            } else if (isGranted) {
                Icon(Icons.Default.CheckCircle, "Granted", tint = SuccessGreen)
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}
