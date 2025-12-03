package com.momoterminal.designsystem.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.delay

sealed class SmsSyncState {
    data object Idle : SmsSyncState()
    data object Syncing : SmsSyncState()
    data class Complete(val count: Int, val message: String = "") : SmsSyncState()
    data class Error(val message: String) : SmsSyncState()
}

@Composable
fun SmsSyncIndicator(state: SmsSyncState, onSync: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    LaunchedEffect(state) {
        when (state) {
            is SmsSyncState.Complete -> view.performMomoHaptic(MomoHaptic.SmsSync)
            is SmsSyncState.Error -> view.performMomoHaptic(MomoHaptic.Warning)
            else -> {}
        }
    }

    PressableCard(
        onClick = { if (state is SmsSyncState.Idle) onSync() },
        enabled = state is SmsSyncState.Idle,
        modifier = modifier.fillMaxWidth(),
        haptic = MomoHaptic.ButtonPress
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), Alignment.Center) {
                when (state) {
                    is SmsSyncState.Idle -> IdleSyncIcon()
                    is SmsSyncState.Syncing -> SyncingIcon()
                    is SmsSyncState.Complete -> CompleteIcon()
                    is SmsSyncState.Error -> ErrorIcon()
                }
            }
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn(tween(MotionTokens.STANDARD)) togetherWith fadeOut(tween(MotionTokens.QUICK)) },
                    label = "syncText"
                ) { s ->
                    when (s) {
                        is SmsSyncState.Idle -> Text("Sync SMS Messages", style = MaterialTheme.typography.titleMedium)
                        is SmsSyncState.Syncing -> Text("Syncing...", style = MaterialTheme.typography.titleMedium)
                        is SmsSyncState.Complete -> Column {
                            Text("${s.count} transactions updated", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                            if (s.message.isNotEmpty()) Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is SmsSyncState.Error -> Text(s.message, style = MaterialTheme.typography.titleMedium, color = Color(0xFFC62828))
                    }
                }
                if (state is SmsSyncState.Idle) {
                    Text("Tap to scan for new transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IdleSyncIcon() {
    Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), Alignment.Center) {
        Icon(Icons.Default.Sms, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SyncingIcon() {
    val t = rememberInfiniteTransition(label = "syncing")
    val pulse by t.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600, easing = MotionTokens.EaseInOut), RepeatMode.Reverse), label = "pulse")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "rot")
    Box(Modifier.size(48.dp).scale(pulse).background(MaterialTheme.colorScheme.primary, CircleShape), Alignment.Center) {
        Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.rotate(rot))
    }
}

@Composable
private fun CompleteIcon() {
    val scale by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "scale")
    Box(Modifier.size(48.dp).scale(scale).background(Color(0xFF2E7D32), CircleShape), Alignment.Center) {
        Icon(Icons.Default.Check, null, tint = Color.White)
    }
}

@Composable
private fun ErrorIcon() {
    Box(Modifier.size(48.dp).background(Color(0xFFC62828), CircleShape), Alignment.Center) {
        Icon(Icons.Default.Sms, null, tint = Color.White)
    }
}

@Composable
fun SyncSummaryBanner(count: Int, visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    LaunchedEffect(visible) {
        if (visible) {
            view.performMomoHaptic(MomoHaptic.SmsSync)
            delay(3000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2E7D32),
            tonalElevation = 4.dp
        ) {
            Row(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = Color.White)
                Text("$count transactions synced", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}
