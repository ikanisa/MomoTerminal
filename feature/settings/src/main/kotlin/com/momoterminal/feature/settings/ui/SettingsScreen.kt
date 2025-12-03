package com.momoterminal.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momoterminal.feature.settings.viewmodel.SettingsEvent
import com.momoterminal.feature.settings.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications")
            Switch(
                checked = state.notificationsEnabled,
                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleNotifications) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Language: ${state.selectedLanguage}")
    }
}
