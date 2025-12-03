package com.momoterminal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momoterminal.core.designsystem.motion.MotionTokens

@Composable
fun StatusPill(
    status: StatusType,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val config = statusConfig(status)
    val infiniteTransition = rememberInfiniteTransition(label = "status")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == StatusType.PENDING) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = MotionTokens.EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    val backgroundColor by animateColorAsState(config.backgroundColor, tween(MotionTokens.STANDARD), label = "bgColor")

    Row(
        modifier = modifier
            .scale(if (status == StatusType.PENDING) pulseScale else 1f)
            .background(backgroundColor, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(label ?: config.defaultLabel, color = config.contentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatusDot(status: StatusType, modifier: Modifier = Modifier) {
    val config = statusConfig(status)
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == StatusType.PENDING) 1.3f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dotScale"
    )
    Box(modifier = modifier.size(8.dp).scale(scale).background(config.backgroundColor, CircleShape))
}

private data class StatusConfig(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val defaultLabel: String
)

@Composable
private fun statusConfig(status: StatusType) = when (status) {
    StatusType.SUCCESS -> StatusConfig(Color(0xFF2E7D32), Color.White, Icons.Default.Check, "Success")
    StatusType.PENDING -> StatusConfig(Color(0xFFF9A825), Color.Black, Icons.Default.Schedule, "Pending")
    StatusType.ERROR -> StatusConfig(Color(0xFFC62828), Color.White, Icons.Default.Close, "Failed")
    StatusType.WARNING -> StatusConfig(Color(0xFFFF8F00), Color.Black, Icons.Default.Warning, "Warning")
    StatusType.INFO -> StatusConfig(Color(0xFF1565C0), Color.White, Icons.Default.Info, "Info")
}
