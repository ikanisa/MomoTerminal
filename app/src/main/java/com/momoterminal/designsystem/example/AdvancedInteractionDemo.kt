package com.momoterminal.designsystem.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.interaction.*
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Advanced demo showcasing:
 * - Collapsing header with parallax
 * - Long-press actions
 * - Success/error feedback animations
 * - Animated counters
 * - 2D drag gestures
 */
@Composable
fun AdvancedInteractionDemo() {
    MomoTerminalTheme {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        
        var balance by remember { mutableIntStateOf(12500) }
        var showSuccess by remember { mutableStateOf(false) }
        var showError by remember { mutableStateOf(false) }
        
        // Scroll-based effects
        val headerProgress = collapsingHeaderProgress(listState, 200)
        val headerElevation = stickyHeaderElevation(listState)
        
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero header with parallax
                item {
                    HeroHeader(
                        balance = balance,
                        progress = headerProgress,
                        onAddMoney = {
                            balance += 1000
                            showSuccess = true
                        },
                        onError = { showError = true }
                    )
                }
                
                // Draggable card demo
                item {
                    DraggableCardDemo()
                }
                
                // Long-press demo
                item {
                    LongPressDemo()
                }
                
                // Counter demo
                item {
                    CounterDemo()
                }
                
                // Spacer
                item { Spacer(Modifier.height(100.dp)) }
            }
            
            // Sticky header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = headerProgress },
                shadowElevation = headerElevation.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced Interactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Success overlay
            if (showSuccess) {
                SuccessOverlay(onComplete = { showSuccess = false })
            }
            
            // Error overlay
            if (showError) {
                ErrorOverlay(onComplete = { showError = false })
            }
        }
    }
}

@Composable
private fun HeroHeader(
    balance: Int,
    progress: Float,
    onAddMoney: () -> Unit,
    onError: () -> Unit
) {
    val animatedBalance = animatedCount(balance)
    val colors = MomoTheme.colors
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer {
                alpha = 1f - progress * 0.5f
                scaleX = 1f - progress * 0.1f
                scaleY = 1f - progress * 0.1f
            }
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(colors.gradientStart, colors.gradientEnd)
                )
            )
            .padding(24.dp)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Balance",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Text(
                "$$animatedBalance",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Success button
                SuccessAnimation(trigger = false) {
                    Button(
                        onClick = onAddMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Money")
                    }
                }
                
                // Error button
                ErrorShakeAnimation(trigger = false) {
                    OutlinedButton(
                        onClick = onError,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Warning, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Test Error")
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableCardDemo() {
    val dragState = rememberDragState(bounds = -100f..100f)
    val offset = animatedDragOffset(dragState)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "2D Drag (with spring-back)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Target area
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
            
            // Draggable card
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                    .draggable2D(dragState),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = if (dragState.isDragging) 8.dp else 2.dp
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.OpenWith,
                        contentDescription = "Drag me",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun LongPressDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Long Press (hold to confirm)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .longPressable(
                    onLongPress = { completed = true },
                    onProgress = { progress = it },
                    duration = 1000L
                ),
            contentAlignment = Alignment.Center
        ) {
            // Progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    .align(Alignment.CenterStart)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (completed) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmed!", color = MaterialTheme.colorScheme.error)
                } else {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (progress > 0) "Hold... ${(progress * 100).toInt()}%" else "Hold to Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        if (completed) {
            TextButton(onClick = { completed = false; progress = 0f }) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun CounterDemo() {
    var targetValue by remember { mutableIntStateOf(0) }
    val animatedValue = animatedCount(targetValue)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Animated Counter",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$animatedValue",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { targetValue += 100 }) {
                    Text("+100")
                }
                FilledTonalButton(onClick = { targetValue += 1000 }) {
                    Text("+1000")
                }
                OutlinedButton(onClick = { targetValue = 0 }) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun SuccessOverlay(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        SuccessAnimation(trigger = true) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF4CAF50),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        ErrorShakeAnimation(trigger = true) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF44336),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Error",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
