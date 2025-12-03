package com.momoterminal.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.motion.MomoHaptic
import com.momoterminal.core.designsystem.motion.MotionTokens
import com.momoterminal.core.designsystem.motion.performMomoHaptic

@Composable
fun MomoBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    LaunchedEffect(visible) { if (visible) view.performMomoHaptic(MomoHaptic.Tap) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionTokens.STANDARD)),
        exit = fadeOut(tween(MotionTokens.QUICK))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(MotionTokens.STANDARD, easing = MotionTokens.EaseOutExpo)) { it } + fadeIn(tween(MotionTokens.QUICK)),
            exit = slideOutVertically(tween(MotionTokens.QUICK, easing = MotionTokens.EaseIn)) { it } + fadeOut(tween(MotionTokens.QUICK))
        ) {
            Surface(
                modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(remember { MutableInteractionSource() }, null) {},
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                    title?.let {
                        Text(it, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                    }
                    content()
                }
            }
        }
    }
}

@Composable
fun TransactionDetailSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    transactionId: String,
    amount: String,
    currencySymbol: String,
    isCredit: Boolean,
    title: String,
    subtitle: String,
    timestamp: String,
    status: StatusType = StatusType.SUCCESS
) {
    MomoBottomSheet(visible, onDismiss, title = "Transaction Details") {
        AnimatedAmount(
            amount.replace(",", "").toDoubleOrNull() ?: 0.0,
            currencySymbol,
            isCredit,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        StatusPill(status)
        Spacer(Modifier.height(24.dp))
        DetailRow("Description", title)
        DetailRow("From/To", subtitle)
        DetailRow("Transaction ID", transactionId)
        DetailRow("Time", timestamp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onDismiss, Modifier.weight(1f)) { Text("Close") }
            Button({}, Modifier.weight(1f)) { Text("Share Receipt") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
