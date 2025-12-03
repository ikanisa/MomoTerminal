package com.momoterminal.core.designsystem.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.theme.MomoTerminalTheme
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun ComponentCatalog() {
    MomoTerminalTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        var searchQuery by remember { mutableStateOf("") }
        var switchState by remember { mutableStateOf(false) }
        var textFieldValue by remember { mutableStateOf("") }
        var amountValue by remember { mutableStateOf("") }
        var selectedChip by remember { mutableIntStateOf(0) }

        SurfaceScaffold(
            header = {
                MomoTopAppBar(
                    title = "Component Catalog",
                    onNavigateBack = {},
                    actions = {
                        MomoIconButton(
                            icon = Icons.Rounded.Settings,
                            onClick = {},
                            style = IconButtonStyle.Ghost
                        )
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MomoTheme.spacing.lg)
            ) {
                // Search
                SectionLabel("Search")
                MomoSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search components...",
                    onClear = { searchQuery = "" }
                )

                // Tabs
                SectionLabel("Tabs")
                MomoTabRow(
                    selectedIndex = selectedTab,
                    tabs = listOf("All", "Inputs", "Display"),
                    onTabSelected = { selectedTab = it }
                )

                // Text Fields
                SectionLabel("Text Fields")
                GlassTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    placeholder = "Enter text...",
                    leadingIcon = Icons.Rounded.Edit,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(MomoTheme.spacing.sm))
                AmountTextField(
                    value = amountValue,
                    onValueChange = { amountValue = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips
                SectionLabel("Chips")
                FilterChipRow(
                    chips = listOf("All", "Pending", "Completed", "Failed"),
                    selectedIndex = selectedChip,
                    onChipSelected = { selectedChip = it }
                )

                // Buttons
                SectionLabel("Buttons")
                Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    PrimaryActionButton(text = "Primary", onClick = {})
                    SecondaryActionButton(text = "Secondary", onClick = {})
                }
                Spacer(Modifier.height(MomoTheme.spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    MomoIconButton(icon = Icons.Rounded.Add, onClick = {}, style = IconButtonStyle.Filled)
                    MomoIconButton(icon = Icons.Rounded.Share, onClick = {}, style = IconButtonStyle.Glass)
                    MomoIconButton(icon = Icons.Rounded.MoreVert, onClick = {}, style = IconButtonStyle.Outlined)
                    MomoIconButton(icon = Icons.Rounded.Close, onClick = {}, style = IconButtonStyle.Ghost)
                }

                // Switch
                SectionLabel("Switch")
                Row {
                    Text("Enable notifications", modifier = Modifier.weight(1f))
                    MomoSwitch(checked = switchState, onCheckedChange = { switchState = it })
                }

                // Progress
                SectionLabel("Progress")
                MomoProgressBar(progress = 0.65f)
                Spacer(Modifier.height(MomoTheme.spacing.sm))
                StepProgressBar(currentStep = 2, totalSteps = 4)

                // Avatars
                SectionLabel("Avatars")
                Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    MomoAvatar(initials = "JD", size = AvatarSize.Small)
                    MomoAvatar(initials = "AB", size = AvatarSize.Medium)
                    MomoAvatar(icon = Icons.Rounded.Person, size = AvatarSize.Large)
                }

                // Badges
                SectionLabel("Badges")
                Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.lg)) {
                    BadgedBox(badge = { MomoBadge() }) {
                        MomoIconButton(icon = Icons.Rounded.Notifications, onClick = {})
                    }
                    BadgedBox(badge = { MomoBadge(count = 5) }) {
                        MomoIconButton(icon = Icons.Rounded.Email, onClick = {})
                    }
                    BadgedBox(badge = { MomoBadge(count = 150) }) {
                        MomoIconButton(icon = Icons.Rounded.Chat, onClick = {})
                    }
                }

                // List Item
                SectionLabel("List Items")
                GlassCard {
                    MomoListItem(
                        title = "John Doe",
                        subtitle = "Last active 2 hours ago",
                        leading = { MomoAvatar(initials = "JD") },
                        trailing = { MomoChip(label = "Active") }
                    )
                    MomoDivider(startIndent = MomoTheme.spacing.xxl)
                    MomoListItem(
                        title = "Jane Smith",
                        subtitle = "Last active yesterday",
                        leading = { MomoAvatar(initials = "JS") }
                    )
                }

                // Info Cards
                SectionLabel("Info Cards")
                Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                    StatCard(
                        label = "Revenue",
                        value = "GH₵ 12,450",
                        trend = "+12.5%",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Expenses",
                        value = "GH₵ 3,200",
                        trend = "-5.2%",
                        trendPositive = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Snackbars
                SectionLabel("Snackbars")
                MomoSnackbar(
                    message = "Transaction completed successfully",
                    type = SnackbarType.Success,
                    icon = Icons.Rounded.CheckCircle
                )
                Spacer(Modifier.height(MomoTheme.spacing.sm))
                MomoSnackbar(
                    message = "Network connection lost",
                    type = SnackbarType.Error,
                    icon = Icons.Rounded.Warning,
                    actionLabel = "Retry",
                    onAction = {}
                )

                // Loading
                SectionLabel("Loading States")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.xl),
                    modifier = Modifier.padding(vertical = MomoTheme.spacing.md)
                ) {
                    MomoLoadingIndicator(size = 32.dp)
                    PulsingDots()
                }

                // Empty State
                SectionLabel("Empty State")
                GlassCard {
                    EmptyState(
                        title = "No transactions yet",
                        description = "Your transactions will appear here",
                        icon = Icons.Rounded.Receipt,
                        action = { PrimaryActionButton(text = "Add Transaction", onClick = {}) }
                    )
                }

                // Dividers
                SectionLabel("Dividers")
                MomoDivider()
                Spacer(Modifier.height(MomoTheme.spacing.sm))
                LabeledDivider(label = "OR")

                Spacer(Modifier.height(MomoTheme.spacing.xxl))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(MomoTheme.spacing.lg))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(MomoTheme.spacing.sm))
}
