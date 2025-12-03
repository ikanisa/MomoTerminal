package com.momoterminal.presentation.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.momoterminal.presentation.theme.MomoAnimation
import com.momoterminal.presentation.theme.MomoTerminalTheme
import com.momoterminal.presentation.theme.MomoYellow

/**
 * Navigation item data class.
 */
data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Modern bottom navigation bar with animated selection indicator.
 * Features smooth transitions and haptic feedback.
 */
@Composable
fun MomoBottomNavigation(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavBarItem(
                    item = item,
                    isSelected = item.route == selectedRoute,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onItemSelected(item.route)
                    }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animated scale
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(MomoAnimation.DURATION_FAST, easing = MomoAnimation.EaseOutBack),
        label = "scale"
    )
    
    // Animated icon color
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MomoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "iconColor"
    )
    
    // Animated text color
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "textColor"
    )
    
    // Animated background
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MomoYellow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "backgroundColor"
    )
    
    // Animated indicator offset
    val indicatorOffset by animateDpAsState(
        targetValue = if (isSelected) 0.dp else 4.dp,
        animationSpec = tween(MomoAnimation.DURATION_FAST),
        label = "indicatorOffset"
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            modifier = Modifier.offset(y = -indicatorOffset)
        )
        
        // Selection indicator dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MomoYellow)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MomoBottomNavigationPreview() {
    MomoTerminalTheme {
        val items = listOf(
            NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
            NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
        
        MomoBottomNavigation(
            items = items,
            selectedRoute = "home",
            onItemSelected = {}
        )
    }
}
