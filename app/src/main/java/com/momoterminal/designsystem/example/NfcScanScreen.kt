package com.momoterminal.designsystem.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
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
import com.momoterminal.designsystem.component.PressableCard
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.delay

sealed class NfcScanState {
    data object Idle : NfcScanState()
    data object Scanning : NfcScanState()
    data class Success(val message: String = "Tag Read Successfully") : NfcScanState()
    data class Error(val message: String = "Failed to read tag") : NfcScanState()
}

@Composable
fun NfcScanScreen(
    state: NfcScanState, amount: String = "0", currencySymbol: String = "GHS",
    onActivate: () -> Unit = {}, onCancel: () -> Unit = {}, onRetry: () -> Unit = {},
    onDismissSuccess: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val view = LocalView.current
    LaunchedEffect(state) {
        when (state) {
            is NfcScanState.Scanning -> view.performMomoHaptic(MomoHaptic.NfcDetected)
            is NfcScanState.Success -> view.performMomoHaptic(MomoHaptic.NfcSuccess)
            is NfcScanState.Error -> view.performMomoHaptic(MomoHaptic.NfcError)
            else -> {}
        }
    }
    LaunchedEffect(state) { if (state is NfcScanState.Success) { delay(2500); onDismissSuccess() } }
    
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AnimatedVisibility(
                visible = state is NfcScanState.Idle || state is NfcScanState.Scanning,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amount to Receive", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("$currencySymbol $amount", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    is NfcScanState.Idle -> IdleNfcIndicator(onActivate)
                    is NfcScanState.Scanning -> ScanningNfcIndicator()
                    is NfcScanState.Success -> SuccessNfcIndicator()
                    is NfcScanState.Error -> ErrorNfcIndicator()
                }
            }
            Spacer(Modifier.height(32.dp))
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(MotionTokens.STANDARD)) togetherWith fadeOut(tween(MotionTokens.QUICK)) },
                label = "statusText"
            ) { s ->
                Text(
                    when (s) { is NfcScanState.Idle -> "Tap to activate NFC terminal"; is NfcScanState.Scanning -> "Hold customer's phone near device"; is NfcScanState.Success -> s.message; is NfcScanState.Error -> s.message },
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (s) { is NfcScanState.Success -> Color(0xFF2E7D32); is NfcScanState.Error -> Color(0xFFC62828); else -> MaterialTheme.colorScheme.onSurfaceVariant },
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(32.dp))
            AnimatedVisibility(
                visible = state is NfcScanState.Scanning || state is NfcScanState.Error,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                when (state) {
                    is NfcScanState.Scanning -> OutlinedButton(onCancel) { Text("Cancel") }
                    is NfcScanState.Error -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { OutlinedButton(onCancel) { Text("Cancel") }; Button(onRetry) { Text("Retry") } }
                    else -> {}
                }
            }
        }
    }
}

@Composable private fun IdleNfcIndicator(onActivate: () -> Unit) = PressableCard(onActivate, Modifier.size(160.dp), shape = CircleShape, containerColor = MaterialTheme.colorScheme.primaryContainer, defaultElevation = 4.dp) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Nfc, "Activate NFC", Modifier.size(64.dp), MaterialTheme.colorScheme.onPrimaryContainer) }
}

@Composable private fun ScanningNfcIndicator() {
    val t = rememberInfiniteTransition(label = "scanning")
    Box(contentAlignment = Alignment.Center) {
        listOf(0, 300, 600).forEach { d ->
            val scale by t.animateFloat(0.5f, 1.5f, infiniteRepeatable(tween(1500, d, MotionTokens.EaseOut), RepeatMode.Restart), label = "s$d")
            val alpha by t.animateFloat(0.6f, 0f, infiniteRepeatable(tween(1500, d, MotionTokens.EaseOut), RepeatMode.Restart), label = "a$d")
            Box(Modifier.size(160.dp).scale(scale).graphicsLayer { this.alpha = alpha }.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
        }
        Box(Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Nfc, null, Modifier.size(48.dp), MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable private fun SuccessNfcIndicator() {
    val scale by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "scale")
    Box(Modifier.size(120.dp).scale(scale).background(Color(0xFF2E7D32), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "Success", Modifier.size(64.dp), Color.White) }
}

@Composable private fun ErrorNfcIndicator() {
    var shake by remember { mutableStateOf(true) }
    val t = rememberInfiniteTransition(label = "error")
    val s by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(100), RepeatMode.Reverse), label = "shake")
    LaunchedEffect(Unit) { delay(500); shake = false }
    Box(Modifier.size(120.dp).graphicsLayer { if (shake) translationX = s }.background(Color(0xFFC62828), CircleShape), Alignment.Center) { Icon(Icons.Default.Close, "Error", Modifier.size(64.dp), Color.White) }
}
