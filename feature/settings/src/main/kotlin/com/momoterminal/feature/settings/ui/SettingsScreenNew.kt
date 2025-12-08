package com.momoterminal.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.feature.settings.viewmodel.SettingsViewModel
import com.momoterminal.feature.settings.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    userId: String,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(userId) {
        viewModel.loadSettings(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is SettingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadSettings(userId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            is SettingsUiState.Success -> {
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("Profile", "Notifications", "Limits", "Features")
                
                Column(modifier = Modifier.padding(padding)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    when (selectedTab) {
                        0 -> ProfileTab(
                            userId = userId,
                            settings = state.settings,
                            viewModel = viewModel
                        )
                        1 -> NotificationsTab(
                            userId = userId,
                            settings = state.settings,
                            viewModel = viewModel
                        )
                        2 -> LimitsTab(
                            userId = userId,
                            settings = state.settings,
                            viewModel = viewModel
                        )
                        3 -> FeaturesTab(
                            userId = userId,
                            settings = state.settings,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(
    userId: String,
    settings: com.momoterminal.core.domain.model.settings.MerchantSettings,
    viewModel: SettingsViewModel
) {
    var businessName by remember(settings.profile.businessName) {
        mutableStateOf(settings.profile.businessName)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Business Profile",
            style = MaterialTheme.typography.titleLarge
        )
        
        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                if (businessName != settings.profile.businessName) {
                    viewModel.updateBusinessName(userId, businessName)
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = businessName.isNotBlank() && businessName != settings.profile.businessName
        ) {
            Text("Save")
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsInfoItem("Merchant Code", settings.profile.merchantCode)
        SettingsInfoItem("Status", settings.profile.status.name)
        SettingsInfoItem(
            "Business Type",
            settings.businessDetails.businessType?.name ?: "Not set"
        )
        
        settings.businessDetails.taxId?.let {
            SettingsInfoItem("Tax ID", it)
        }
        
        settings.contactInfo.email?.let {
            SettingsInfoItem("Email", it)
        }
        
        settings.contactInfo.phone?.let {
            SettingsInfoItem("Phone", it)
        }
    }
}

@Composable
private fun NotificationsTab(
    userId: String,
    settings: com.momoterminal.core.domain.model.settings.MerchantSettings,
    viewModel: SettingsViewModel
) {
    val prefs = settings.notificationPrefs
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Notification Channels",
            style = MaterialTheme.typography.titleLarge
        )
        
        SettingsToggleItem(
            title = "Email Notifications",
            description = "Receive notifications via email",
            checked = prefs.emailEnabled,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(emailEnabled = it)
                )
            }
        )
        
        SettingsToggleItem(
            title = "SMS Notifications",
            description = "Receive notifications via SMS",
            checked = prefs.smsEnabled,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(smsEnabled = it)
                )
            }
        )
        
        SettingsToggleItem(
            title = "Push Notifications",
            description = "Receive push notifications on this device",
            checked = prefs.pushEnabled,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(pushEnabled = it)
                )
            }
        )
        
        SettingsToggleItem(
            title = "WhatsApp Notifications",
            description = "Receive notifications via WhatsApp",
            checked = prefs.whatsappEnabled,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(whatsappEnabled = it)
                )
            }
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text(
            text = "Notification Events",
            style = MaterialTheme.typography.titleMedium
        )
        
        SettingsToggleItem(
            title = "Transaction Success",
            checked = prefs.events.transactionSuccess,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(events = prefs.events.copy(transactionSuccess = it))
                )
            }
        )
        
        SettingsToggleItem(
            title = "Transaction Failed",
            checked = prefs.events.transactionFailed,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(events = prefs.events.copy(transactionFailed = it))
                )
            }
        )
        
        SettingsToggleItem(
            title = "Daily Summary",
            checked = prefs.events.dailySummary,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(events = prefs.events.copy(dailySummary = it))
                )
            }
        )
        
        SettingsToggleItem(
            title = "Security Alerts",
            checked = prefs.events.securityAlerts,
            onCheckedChange = {
                viewModel.updateNotifications(
                    userId,
                    prefs.copy(events = prefs.events.copy(securityAlerts = it))
                )
            }
        )
    }
}

@Composable
private fun LimitsTab(
    userId: String,
    settings: com.momoterminal.core.domain.model.settings.MerchantSettings,
    viewModel: SettingsViewModel
) {
    val limits = settings.transactionLimits
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Transaction Limits",
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = "Configure transaction limits for your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        SettingsInfoItem(
            "Currency",
            limits.currency
        )
        
        SettingsInfoItem(
            "Minimum Amount",
            "${limits.minimumAmount} ${limits.currency}"
        )
        
        limits.dailyLimit?.let {
            SettingsInfoItem(
                "Daily Limit",
                "$it ${limits.currency}"
            )
        } ?: Text(
            "Daily Limit: Not set",
            style = MaterialTheme.typography.bodyMedium
        )
        
        limits.singleTransactionLimit?.let {
            SettingsInfoItem(
                "Single Transaction Limit",
                "$it ${limits.currency}"
            )
        } ?: Text(
            "Single Transaction Limit: Not set",
            style = MaterialTheme.typography.bodyMedium
        )
        
        limits.monthlyLimit?.let {
            SettingsInfoItem(
                "Monthly Limit",
                "$it ${limits.currency}"
            )
        } ?: Text(
            "Monthly Limit: Not set",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Contact support to modify transaction limits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturesTab(
    userId: String,
    settings: com.momoterminal.core.domain.model.settings.MerchantSettings,
    viewModel: SettingsViewModel
) {
    val flags = settings.featureFlags
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Feature Settings",
            style = MaterialTheme.typography.titleLarge
        )
        
        SettingsToggleItem(
            title = "NFC Payments",
            description = "Enable NFC tap-to-pay functionality",
            checked = flags.nfcEnabled,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "nfc", it)
            }
        )
        
        SettingsToggleItem(
            title = "Offline Mode",
            description = "Allow transactions when offline",
            checked = flags.offlineMode,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "offline", it)
            }
        )
        
        SettingsToggleItem(
            title = "Auto Sync",
            description = "Automatically sync data when online",
            checked = flags.autoSync,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "autoSync", it)
            }
        )
        
        SettingsToggleItem(
            title = "Biometric Authentication",
            description = "Require fingerprint or face unlock",
            checked = flags.biometricRequired,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "biometric", it)
            }
        )
        
        SettingsToggleItem(
            title = "Digital Receipts",
            description = "Generate PDF receipts for transactions",
            checked = flags.receiptsEnabled,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "receipts", it)
            }
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text(
            text = "Advanced Features",
            style = MaterialTheme.typography.titleMedium
        )
        
        SettingsToggleItem(
            title = "Multi-Currency",
            description = "Enable support for multiple currencies",
            checked = flags.multiCurrency,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "multiCurrency", it)
            }
        )
        
        SettingsToggleItem(
            title = "Advanced Analytics",
            description = "Detailed transaction analytics and reports",
            checked = flags.advancedAnalytics,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "analytics", it)
            }
        )
        
        SettingsToggleItem(
            title = "API Access",
            description = "Enable API access for integrations",
            checked = flags.apiAccess,
            onCheckedChange = {
                viewModel.toggleFeatureFlag(userId, "api", it)
            }
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
