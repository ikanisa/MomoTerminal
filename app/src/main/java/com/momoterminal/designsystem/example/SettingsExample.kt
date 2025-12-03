package com.momoterminal.designsystem.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun SettingsExample() {
    MomoTerminalTheme {
        var biometricEnabled by remember { mutableStateOf(true) }
        var notificationsEnabled by remember { mutableStateOf(true) }
        var darkMode by remember { mutableStateOf(false) }
        var autoSync by remember { mutableStateOf(true) }

        SurfaceScaffold(
            header = {
                MomoTopAppBar(title = "Settings", onNavigateBack = {})
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MomoTheme.spacing.lg)
            ) {
                // Account Section
                SettingsSection("Account") {
                    SettingsItem(
                        icon = Icons.Rounded.Person,
                        title = "Profile",
                        subtitle = "+250 788 123 456"
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Business,
                        title = "Business Info",
                        subtitle = "Kigali Shop"
                    )
                }

                // Security Section
                SettingsSection("Security") {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Biometric Login",
                        subtitle = "Use fingerprint to unlock",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Lock,
                        title = "Change PIN",
                        subtitle = "Update your security PIN"
                    )
                }

                // Preferences Section
                SettingsSection("Preferences") {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = "Transaction alerts",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.Sync,
                        title = "Auto Sync",
                        subtitle = "Sync transactions automatically",
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }

                // About Section
                SettingsSection("About") {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        subtitle = "1.0.0 (Build 42)"
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Description,
                        title = "Terms of Service"
                    )
                    SettingsItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy"
                    )
                }

                Spacer(Modifier.height(MomoTheme.spacing.xxl))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Spacer(Modifier.height(MomoTheme.spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = MomoTheme.spacing.sm)
        )
        GlassCard { Column { content() } }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    MomoListItem(
        title = title,
        subtitle = subtitle,
        leading = {
            MomoIconButton(
                icon = icon,
                onClick = {},
                style = IconButtonStyle.Ghost,
                size = 40.dp
            )
        },
        trailing = if (onClick != null) {
            { MomoIconButton(icon = Icons.Rounded.ChevronRight, onClick = {}, style = IconButtonStyle.Ghost, size = 32.dp) }
        } else null,
        onClick = onClick
    )
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MomoListItem(
        title = title,
        subtitle = subtitle,
        leading = {
            MomoIconButton(
                icon = icon,
                onClick = {},
                style = IconButtonStyle.Ghost,
                size = 40.dp
            )
        },
        trailing = { MomoSwitch(checked = checked, onCheckedChange = onCheckedChange) },
        onClick = { onCheckedChange(!checked) }
    )
}
