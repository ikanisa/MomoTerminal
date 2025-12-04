package com.momoterminal.presentation.screens.nfc

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.component.AnimatedBalance
import com.momoterminal.designsystem.component.PressableCard
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import com.momoterminal.feature.nfc.NfcState
import kotlinx.coroutines.delay

/**
 * NfcTerminalScreen - Full-featured NFC payment terminal with motion system.
 */
@Composable
fun NfcTerminalScreen(
    amount: Double, currencySymbol: String, nfcState: NfcState,
    onActivate: () -> Unit, onCancel: () -> Unit, onComplete: () -> Unit, modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    LaunchedEffect(nfcState) {
        when (nfcState) {
            is NfcState.Ready -> view.performMomoHaptic(MomoHaptic.NfcDetected)
            is NfcState.Success -> view.performMomoHaptic(MomoHaptic.NfcSuccess)
            is NfcState.Error -> view.performMomoHaptic(MomoHaptic.NfcError)
            else -> {}
        }
    }
    
    LaunchedEffect(nfcState) { if (nfcState is NfcState.Success) { delay(2500); onComplete() } }
    
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Amount section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (nfcState) { is NfcState.Success -> "Payment Received"; is NfcState.Ready -> "Amount to Receive"; else -> "Enter Amount" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (nfcState is NfcState.Success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                AnimatedBalance(
                    amount, "$currencySymbol ",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    defaultColor = when (nfcState) { is NfcState.Success -> Color(0xFF2E7D32); is NfcState.Error -> Color(0xFFC62828); else -> MaterialTheme.colorScheme.onSurface },
                    triggerHaptic = false
                )
            }
            
            Spacer(Modifier.height(48.dp))
            
            // NFC indicator
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                when (nfcState) {
                    is NfcState.Ready -> IdleIndicator(onActivate, amount > 0)
                    is NfcState.Activating, is NfcState.Active, is NfcState.Processing -> ScanningIndicator()
                    is NfcState.Success -> SuccessIndicator()
                    is NfcState.Error, is NfcState.Timeout -> ErrorIndicator()
                    is NfcState.Disabled, is NfcState.NotSupported -> DisabledIndicator()
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Status text
            AnimatedContent(
                targetState = nfcState,
                transitionSpec = { fadeIn(tween(MotionTokens.STANDARD)) togetherWith fadeOut(tween(MotionTokens.QUICK)) },
                label = "status"
            ) { state ->
                Text(
                    state.getDisplayMessage(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (state) {
                        is NfcState.Success -> Color(0xFF2E7D32)
                        is NfcState.Error, is NfcState.Timeout -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Action buttons
            AnimatedVisibility(
                visible = nfcState.isWorking() || nfcState is NfcState.Error || nfcState is NfcState.Timeout,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                when (nfcState) {
                    is NfcState.Activating, is NfcState.Active, is NfcState.Processing -> OutlinedButton(onCancel) { Text("Cancel") }
                    is NfcState.Error, is NfcState.Timeout -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { OutlinedButton(onCancel) { Text("Cancel") }; Button(onActivate) { Text("Retry") } }
                    else -> {}
                }
            }
        }
    }
}

@Composable private fun IdleIndicator(onActivate: () -> Unit, hasAmount: Boolean) = PressableCard(
    onClick = { if (hasAmount) onActivate() },
    modifier = Modifier.size(160.dp),
    enabled = hasAmount,
    shape = CircleShape,
    containerColor = if (hasAmount) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    defaultElevation = if (hasAmount) 4.dp else 0.dp
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Nfc, "Activate NFC", Modifier.size(64.dp), if (hasAmount) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable private fun ScanningIndicator() {
    val t = rememberInfiniteTransition(label = "scanning")
    Box(contentAlignment = Alignment.Center) {
        listOf(0, 300, 600).forEach { d ->
            val scale by t.animateFloat(0.5f, 1.5f, infiniteRepeatable(tween(1500, d, MotionTokens.EaseOut), RepeatMode.Restart), label = "s$d")
            val alpha by t.animateFloat(0.6f, 0f, infiniteRepeatable(tween(1500, d, MotionTokens.EaseOut), RepeatMode.Restart), label = "a$d")
            Box(Modifier.size(160.dp).scale(scale).graphicsLayer { this.alpha = alpha }.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
        }
        val pulse by t.animateFloat(1f, 1.05f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse")
        Box(Modifier.size(100.dp).scale(pulse).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Nfc, null, Modifier.size(48.dp), MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable private fun SuccessIndicator() {
    val scale by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "scale")
    Box(Modifier.size(120.dp).scale(scale).background(Color(0xFF2E7D32), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "Success", Modifier.size(64.dp), Color.White) }
}

@Composable private fun ErrorIndicator() {
    var shake by remember { mutableStateOf(true) }
    val t = rememberInfiniteTransition(label = "error")
    val s by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(80), RepeatMode.Reverse), label = "shake")
    LaunchedEffect(Unit) { delay(400); shake = false }
    Box(Modifier.size(120.dp).graphicsLayer { if (shake) translationX = s }.background(Color(0xFFC62828), CircleShape), Alignment.Center) { Icon(Icons.Default.Close, "Error", Modifier.size(64.dp), Color.White) }
}

@Composable private fun DisabledIndicator() = Box(Modifier.size(120.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), Alignment.Center) {
    Icon(Icons.Default.WifiOff, "NFC Disabled", Modifier.size(64.dp), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
}
