package com.momoterminal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

data class NavItem(
    val icon: ImageVector,
    val label: String,
    val badge: Int? = null
)

@Composable
fun MomoBottomNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MomoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colors.surfaceGlassElevated)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(vertical = MomoTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { index, item ->
            NavBarItem(
                item = item,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIcon"
    )

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = MomoTheme.spacing.lg, vertical = MomoTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(
            badge = {
                if (item.badge != null && item.badge > 0) {
                    MomoBadge(count = item.badge)
                }
            }
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor
        )
    }
}
