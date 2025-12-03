package com.momoterminal.core.designsystem.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.component.*
import com.momoterminal.core.designsystem.motion.MomoHaptic
import com.momoterminal.core.designsystem.motion.MotionTokens
import com.momoterminal.core.designsystem.motion.performMomoHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionShowcase(onNavigateBack: () -> Unit = {}) {
    var currentSection by remember { mutableStateOf(ShowcaseSection.Overview) }
    
    Scaffold(
        topBar = { TopAppBar({ Text("Motion System") }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(currentSection.ordinal, Modifier.fillMaxWidth()) {
                ShowcaseSection.entries.forEach { Tab(currentSection == it, { currentSection = it }, text = { Text(it.title) }) }
            }
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = { fadeIn(tween(MotionTokens.STANDARD)) togetherWith fadeOut(tween(MotionTokens.QUICK)) },
                label = "section"
            ) { section ->
                when (section) {
                    ShowcaseSection.Overview -> OverviewSection()
                    ShowcaseSection.Haptics -> HapticsSection()
                    ShowcaseSection.Cards -> CardsSection()
                    ShowcaseSection.Balance -> BalanceSection()
                    ShowcaseSection.Status -> StatusSection()
                    ShowcaseSection.NFC -> NfcSection()
                    ShowcaseSection.SMS -> SmsSyncSection()
                }
            }
        }
    }
}

private enum class ShowcaseSection(val title: String) { Overview("Overview"), Haptics("Haptics"), Cards("Cards"), Balance("Balance"), Status("Status"), NFC("NFC"), SMS("SMS Sync") }

@Composable private fun OverviewSection() = LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item { Text("Motion Principles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    item {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Duration Tokens", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                listOf("INSTANT" to "${MotionTokens.INSTANT}ms - Haptics", "QUICK" to "${MotionTokens.QUICK}ms - List updates", "STANDARD" to "${MotionTokens.STANDARD}ms - Transitions", "FINANCIAL" to "${MotionTokens.FINANCIAL}ms - Balance updates", "EMPHASIS" to "${MotionTokens.EMPHASIS}ms - Important changes").forEach { (name, desc) ->
                    Text("• $name: $desc", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    item {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Easing Curves", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                listOf("EaseOut - Elements entering", "EaseIn - Elements leaving", "EaseOutExpo - Premium feel", "EaseFinancial - Trustworthy", "EaseOutBack - Bouncy success").forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable private fun HapticsSection() {
    val view = LocalView.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Tap to feel each haptic pattern", style = MaterialTheme.typography.titleMedium) }
        items(MomoHaptic.entries) { haptic ->
            PressableCard({ view.performMomoHaptic(haptic) }, Modifier.fillMaxWidth(), haptic = haptic) {
                Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(haptic.name, style = MaterialTheme.typography.titleSmall)
                        Text(hapticDesc(haptic), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.TouchApp, null)
                }
            }
        }
    }
}

private fun hapticDesc(h: MomoHaptic) = when (h) { MomoHaptic.Tap -> "Light tap"; MomoHaptic.NfcDetected -> "NFC detected"; MomoHaptic.NfcSuccess -> "NFC success"; MomoHaptic.NfcError -> "NFC error"; MomoHaptic.PaymentSuccess -> "Payment confirmed"; MomoHaptic.PaymentError -> "Payment failed"; MomoHaptic.BalanceUpdate -> "Balance changed"; MomoHaptic.SmsSync -> "SMS synced"; MomoHaptic.ButtonPress -> "Button press"; MomoHaptic.Warning -> "Warning" }

@Composable private fun CardsSection() = LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item { Text("PressableCard", style = MaterialTheme.typography.titleMedium); Text("Scale + shadow + haptic on press", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item { PressableCard({}, Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) { Text("Wallet Card", style = MaterialTheme.typography.titleMedium); Text("Tap and hold to see the press effect") } } }
    item {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            PressableCard({}, Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Nfc, null); Text("NFC") } }
            PressableCard({}, Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.secondaryContainer) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Sms, null); Text("SMS") } }
        }
    }
}

@Composable private fun BalanceSection() {
    var balance by remember { mutableDoubleStateOf(125000.0) }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AnimatedBalance", style = MaterialTheme.typography.titleMedium)
        Text("Number tweening with color flash", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Available Balance", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                AnimatedBalance(balance, "GHS ", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), defaultColor = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button({ balance += 5000 }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32))) { Text("+5,000") }
            Button({ balance -= 2500 }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFC62828))) { Text("-2,500") }
        }
    }
}

@Composable private fun StatusSection() {
    var status by remember { mutableStateOf(StatusType.SUCCESS) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("StatusPill", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { StatusType.entries.forEach { FilterChip(status == it, { status = it }, label = { Text(it.name) }) } }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) { StatusPill(status) }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) { Text("StatusDot: "); StatusDot(status) }
    }
}

@Composable private fun NfcSection() {
    var state by remember { mutableStateOf<NfcScanState>(NfcScanState.Idle) }
    val scope = rememberCoroutineScope()
    NfcScanScreen(state, "50.00", "GHS",
        onActivate = { state = NfcScanState.Scanning; scope.launch { delay(3000); state = NfcScanState.Success("Payment of GHS 50.00 received!") } },
        onCancel = { state = NfcScanState.Idle },
        onRetry = { state = NfcScanState.Scanning; scope.launch { delay(2000); state = NfcScanState.Error("Connection lost") } },
        onDismissSuccess = { state = NfcScanState.Idle }
    )
}

@Composable private fun SmsSyncSection() {
    var state by remember { mutableStateOf<SmsSyncState>(SmsSyncState.Idle) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SMS Sync Flow", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        SmsSyncIndicator(state, { state = SmsSyncState.Syncing; scope.launch { delay(2500); state = SmsSyncState.Complete(5, "Last sync: just now"); delay(3000); state = SmsSyncState.Idle } })
        Spacer(Modifier.height(24.dp))
        Text("Manual Controls:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ state = SmsSyncState.Idle }) { Text("Idle") }
            OutlinedButton({ state = SmsSyncState.Syncing }) { Text("Syncing") }
            OutlinedButton({ state = SmsSyncState.Complete(3) }) { Text("Complete") }
            OutlinedButton({ state = SmsSyncState.Error("Network error") }) { Text("Error") }
        }
    }
}
