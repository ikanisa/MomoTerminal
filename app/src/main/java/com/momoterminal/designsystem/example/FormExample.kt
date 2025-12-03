package com.momoterminal.designsystem.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun FormExample() {
    MomoTerminalTheme {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var selectedProvider by remember { mutableStateOf("MTN") }
        var agreeTerms by remember { mutableStateOf(false) }
        var receiveUpdates by remember { mutableStateOf(true) }
        var notificationLevel by remember { mutableFloatStateOf(50f) }

        SurfaceScaffold(
            header = {
                MomoTopAppBar(title = "Payment Form", onNavigateBack = {})
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(MomoTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MomoTheme.spacing.lg)
            ) {
                // Recipient Section
                Text("Recipient", style = MaterialTheme.typography.titleMedium)
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Recipient name",
                    leadingIcon = Icons.Rounded.Person,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "Phone number",
                    leadingIcon = Icons.Rounded.Phone,
                    modifier = Modifier.fillMaxWidth()
                )

                MomoDivider()

                // Amount Section
                Text("Amount", style = MaterialTheme.typography.titleMedium)
                AmountTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth()
                )

                MomoDivider()

                // Provider Selection
                Text("Provider", style = MaterialTheme.typography.titleMedium)
                SegmentedButton(
                    options = listOf("MTN", "Airtel", "Orange"),
                    selectedOption = selectedProvider,
                    onOptionSelected = { selectedProvider = it },
                    modifier = Modifier.fillMaxWidth()
                )

                MomoDivider()

                // Preferences
                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                
                Text("Notification volume", style = MaterialTheme.typography.bodyMedium)
                MomoSlider(
                    value = notificationLevel,
                    onValueChange = { notificationLevel = it },
                    valueRange = 0f..100f,
                    showValue = true,
                    valueFormatter = { "${it.toInt()}%" }
                )

                MomoCheckbox(
                    checked = agreeTerms,
                    onCheckedChange = { agreeTerms = it },
                    label = "I agree to the terms and conditions"
                )
                MomoCheckbox(
                    checked = receiveUpdates,
                    onCheckedChange = { receiveUpdates = it },
                    label = "Receive transaction updates via SMS"
                )

                Spacer(Modifier.height(MomoTheme.spacing.lg))

                // Submit Button
                PrimaryActionButton(
                    text = "Send Payment",
                    onClick = {},
                    enabled = name.isNotBlank() && phone.isNotBlank() && amount.isNotBlank() && agreeTerms,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(MomoTheme.spacing.xxl))
            }
        }
    }
}
