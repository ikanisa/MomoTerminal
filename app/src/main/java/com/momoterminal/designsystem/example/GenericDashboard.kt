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

/**
 * Domain-agnostic dashboard demonstrating design system flexibility.
 * Can be adapted for: fintech, mobility, social, e-commerce, health, etc.
 */
@Composable
fun GenericDashboard() {
    MomoTerminalTheme {
        var selectedNav by remember { mutableIntStateOf(0) }

        Box(Modifier.fillMaxSize()) {
            SurfaceScaffold(
                header = {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Good morning",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        BadgedBox(badge = { MomoBadge(count = 3) }) {
                            MomoIconButton(
                                icon = Icons.Rounded.Notifications,
                                onClick = {},
                                style = IconButtonStyle.Glass
                            )
                        }
                    }
                },
                floatingContent = {
                    GlassCardGradient {
                        Column(Modifier.padding(MomoTheme.spacing.lg)) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(MomoTheme.spacing.xs))
                            Text(
                                text = "1,247",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(MomoTheme.spacing.sm))
                            MomoProgressBar(progress = 0.72f)
                            Spacer(Modifier.height(MomoTheme.spacing.xs))
                            Text(
                                text = "72% of monthly goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MomoTheme.spacing.lg)
                        .padding(top = MomoTheme.spacing.xxl * 2)
                ) {
                    // Quick Actions
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(MomoTheme.spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(icon = Icons.Rounded.Add, label = "Create")
                        QuickActionItem(icon = Icons.Rounded.Search, label = "Search")
                        QuickActionItem(icon = Icons.Rounded.Share, label = "Share")
                        QuickActionItem(icon = Icons.Rounded.MoreHoriz, label = "More")
                    }

                    Spacer(Modifier.height(MomoTheme.spacing.xl))

                    // Stats Grid
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(MomoTheme.spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.sm)) {
                        StatCard(
                            label = "Active",
                            value = "847",
                            trend = "+23",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Pending",
                            value = "156",
                            trend = "-12",
                            trendPositive = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(MomoTheme.spacing.xl))

                    // Recent Activity
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(MomoTheme.spacing.md))
                    GlassCard {
                        ActivityItem(
                            title = "New item created",
                            subtitle = "2 minutes ago",
                            icon = Icons.Rounded.Add
                        )
                        MomoDivider(startIndent = MomoTheme.spacing.xxl)
                        ActivityItem(
                            title = "Status updated",
                            subtitle = "1 hour ago",
                            icon = Icons.Rounded.Edit
                        )
                        MomoDivider(startIndent = MomoTheme.spacing.xxl)
                        ActivityItem(
                            title = "Item completed",
                            subtitle = "3 hours ago",
                            icon = Icons.Rounded.CheckCircle
                        )
                    }

                    Spacer(Modifier.height(MomoTheme.spacing.xxl * 3))
                }
            }

            // Bottom Navigation
            MomoBottomNavBar(
                items = listOf(
                    NavItem(Icons.Rounded.Home, "Home"),
                    NavItem(Icons.Rounded.Search, "Explore"),
                    NavItem(Icons.Rounded.Add, "Create"),
                    NavItem(Icons.Rounded.Notifications, "Alerts", badge = 3),
                    NavItem(Icons.Rounded.Person, "Profile")
                ),
                selectedIndex = selectedNav,
                onItemSelected = { selectedNav = it },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        MomoIconButton(icon = icon, onClick = {}, style = IconButtonStyle.Glass)
        Spacer(Modifier.height(MomoTheme.spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActivityItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    MomoListItem(
        title = title,
        subtitle = subtitle,
        leading = {
            MomoAvatar(icon = icon, size = AvatarSize.Medium)
        }
    )
}
