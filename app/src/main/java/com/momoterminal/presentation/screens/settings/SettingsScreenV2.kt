package com.momoterminal.presentation.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.R
import com.momoterminal.presentation.components.MomoButton
import com.momoterminal.presentation.components.MomoTextField
import com.momoterminal.presentation.components.common.MomoTopAppBar
import com.momoterminal.presentation.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * World-Class Settings Screen
 * 
 * Features:
 * - Auto-save with debouncing
 * - Real-time validation
 * - Loading states
 * - Clear save status
 * - Haptic feedback
 * - Confirmation dialogs
 * - Skeleton loaders
 * - Optimistic UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenV2(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Auto-save state
    var businessNameDraft by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var momoNumberDraft by remember(uiState.momoIdentifier) { mutableStateOf(uiState.momoIdentifier) }
    
    // Debounced auto-save for business name
    LaunchedEffect(businessNameDraft) {
        if (businessNameDraft != uiState.userName && businessNameDraft.isNotBlank()) {
            delay(1500) // Wait for user to stop typing
            viewModel.updateMerchantName(businessNameDraft)
            viewModel.saveSettings()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    // Debounced auto-save for mobile money
    LaunchedEffect(momoNumberDraft) {
        if (momoNumberDraft != uiState.momoIdentifier && momoNumberDraft.isNotBlank()) {
            delay(1500)
            viewModel.updateMomoIdentifier(momoNumberDraft)
            if (uiState.isMomoIdentifierValid) {
                viewModel.saveSettings()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Section with skeleton loader
            if (uiState.isLoadingProfile) {
                SkeletonProfileCard()
            } else {
                ProfileCard(
                    businessName = businessNameDraft,
                    onBusinessNameChange = { businessNameDraft = it },
                    phoneNumber = uiState.authPhone,
                    country = uiState.profileCountryName,
                    isSaving = uiState.showSaveSuccess,
                    lastSaved = if (businessNameDraft == uiState.userName) "Saved" else "Not saved"
                )
            }
            
            // Mobile Money Configuration
            MobileMoneyCard(
                momoNumber = momoNumberDraft,
                onMomoNumberChange = { momoNumberDraft = it },
                countryCode = uiState.momoCountryCode,
                countryName = uiState.momoCountryName,
                isValid = uiState.isMomoIdentifierValid,
                isSaving = uiState.showSaveSuccess,
                lastSaved = if (momoNumberDraft == uiState.momoIdentifier && uiState.momoIdentifier.isNotBlank()) "Saved" else "Not saved",
                validationMessage = if (momoNumberDraft.isNotBlank() && !uiState.isMomoIdentifierValid) 
                    "Invalid format for ${uiState.momoCountryName}" else null
            )
            
            // Spacer for logout button
            Spacer(modifier = Modifier.height(32.dp))
            
            // Logout Button with confirmation
            OutlinedButton(
                onClick = { viewModel.showLogoutDialog() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
    
    // Logout Confirmation Dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Sign out?") },
            text = { Text("You'll need to login again with WhatsApp OTP to access your account.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideLogoutDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    phoneNumber: String,
    country: String,
    isSaving: Boolean,
    lastSaved: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Business Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SaveStatusIndicator(isSaving, lastSaved)
            }
            
            HorizontalDivider()
            
            // Business Name - Editable with auto-save
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Business Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MomoTextField(
                    value = businessName,
                    onValueChange = onBusinessNameChange,
                    label = "",
                    placeholder = "Enter your business name",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (businessName.isNotBlank()) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen
                            )
                        }
                    }
                )
                Text(
                    text = "Auto-saves as you type",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            HorizontalDivider()
            
            // WhatsApp Number - Read only
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "WhatsApp Number",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = phoneNumber.ifBlank { "Not set" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Country - Read only
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Country",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = country.ifBlank { "Not set" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileMoneyCard(
    momoNumber: String,
    onMomoNumberChange: (String) -> Unit,
    countryCode: String,
    countryName: String,
    isValid: Boolean,
    isSaving: Boolean,
    lastSaved: String,
    validationMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mobile Money",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SaveStatusIndicator(isSaving, lastSaved)
            }
            
            HorizontalDivider()
            
            // Mobile Money Number
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mobile Money Number",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MomoTextField(
                    value = momoNumber,
                    onValueChange = onMomoNumberChange,
                    label = "",
                    placeholder = "Enter mobile money number",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    isError = momoNumber.isNotBlank() && !isValid,
                    trailingIcon = {
                        when {
                            isSaving -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            momoNumber.isNotBlank() && isValid -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen
                            )
                            momoNumber.isNotBlank() && !isValid -> Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            else -> null
                        }
                    }
                )
                
                if (validationMessage != null) {
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Auto-saves as you type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Country Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Country",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveStatusIndicator(
    isSaving: Boolean,
    lastSaved: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isSaving -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Saving...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            lastSaved == "Saved" -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = lastSaved,
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
            else -> {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(8.dp)
                )
                Text(
                    text = lastSaved,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SkeletonProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Skeleton shimmer boxes
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(24.dp)
                    .shimmerEffect()
            )
            HorizontalDivider()
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha = transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    return this.then(
        Modifier.background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value)
        )
    )
}
