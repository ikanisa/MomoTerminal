package com.momoterminal.designsystem.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.interaction.*
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme
import kotlinx.coroutines.launch

/**
 * Complete interaction showcase demonstrating all patterns.
 */
@Composable
fun InteractionShowcase() {
    MomoTerminalTheme {
        ReducedMotionProvider {
            val scope = rememberCoroutineScope()
            val listState = rememberLazyListState()
            val sheetState = rememberInteractiveSheetState()
            var selectedDemo by remember { mutableStateOf<DemoType?>(null) }
            
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Header
                    item {
                        ShowcaseHeader(
                            modifier = Modifier
                                .fadeOnScroll(listState, threshold = 150)
                                .scaleOnScroll(listState, minScale = 0.9f)
                        )
                    }
                    
                    // Demo cards with staggered animation
                    itemsIndexed(DemoType.entries) { index, demo ->
                        DemoCard(
                            demo = demo,
                            index = index,
                            onClick = {
                                selectedDemo = demo
                                scope.launch { sheetState.show(SheetPosition.Half) }
                            }
                        )
                    }
                }
                
                // Bottom sheet with demo details
                if (sheetState.isVisible && selectedDemo != null) {
                    InteractiveBottomSheet(
                        state = sheetState,
                        onDismiss = { selectedDemo = null }
                    ) {
                        DemoDetailContent(
                            demo = selectedDemo!!,
                            onClose = {
                                scope.launch {
                                    sheetState.hide()
                                    selectedDemo = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowcaseHeader(modifier: Modifier = Modifier) {
    val colors = MomoTheme.colors
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(colors.gradientStart, colors.gradientEnd)
                )
            )
            .padding(24.dp)
            .statusBarsPadding(),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.bounceIn()) {
            Text(
                "Interaction Patterns",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Tap any card to explore",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DemoCard(
    demo: DemoType,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .slideUp(delay = index * 50)
            .interactiveClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MomoTheme.colors.surfaceGlass,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(demo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    demo.icon,
                    contentDescription = null,
                    tint = demo.color
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    demo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    demo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DemoDetailContent(
    demo: DemoType,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(demo.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(demo.icon, null, tint = demo.color)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    demo.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            demo.details,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Interactive demo area
        demo.DemoContent()
        
        Spacer(Modifier.height(24.dp))
        
        SecondaryActionButton(
            text = "Close",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private enum class DemoType(
    val title: String,
    val description: String,
    val details: String,
    val icon: ImageVector,
    val color: Color
) {
    PRESS(
        "Press Feedback",
        "Scale + haptic on tap",
        "Interactive elements respond with subtle scale animation and haptic feedback for tactile confirmation.",
        Icons.Rounded.TouchApp,
        Color(0xFF2196F3)
    ) {
        @Composable
        override fun DemoContent() {
            var count by remember { mutableIntStateOf(0) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.1f))
                    .interactiveClickable { count++ },
                contentAlignment = Alignment.Center
            ) {
                Text("Tapped $count times", fontWeight = FontWeight.Medium)
            }
        }
    },
    
    LONG_PRESS(
        "Long Press",
        "Hold to confirm actions",
        "Destructive or important actions require holding to prevent accidental triggers.",
        Icons.Rounded.Timer,
        Color(0xFFFF5722)
    ) {
        @Composable
        override fun DemoContent() {
            var progress by remember { mutableFloatStateOf(0f) }
            var done by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f))
                    .longPressable(
                        onLongPress = { done = true },
                        onProgress = { progress = it }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(color.copy(alpha = 0.2f))
                        .align(Alignment.CenterStart)
                )
                Text(
                    if (done) "Done!" else "Hold to confirm",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    },
    
    BOUNCE(
        "Bounce Animation",
        "Playful entrance effects",
        "Elements enter with a subtle bounce for a polished, premium feel.",
        Icons.Rounded.Animation,
        Color(0xFF9C27B0)
    ) {
        @Composable
        override fun DemoContent() {
            var show by remember { mutableStateOf(true) }
            
            Column {
                Button(onClick = { show = !show }) {
                    Text(if (show) "Hide" else "Show")
                }
                Spacer(Modifier.height(16.dp))
                AnimatedVisibilityContainer(
                    visible = show,
                    style = AnimationStyle.FadeScale
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Star, null, tint = Color.White)
                    }
                }
            }
        }
    },
    
    COUNTER(
        "Animated Counter",
        "Smooth number transitions",
        "Numbers animate smoothly between values for a polished data display.",
        Icons.Rounded.Numbers,
        Color(0xFF4CAF50)
    ) {
        @Composable
        override fun DemoContent() {
            var target by remember { mutableIntStateOf(1000) }
            val animated = animatedCount(target)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$animated",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { target += 500 }) { Text("+500") }
                    OutlinedButton(onClick = { target = 0 }) { Text("Reset") }
                }
            }
        }
    },
    
    SUCCESS(
        "Success Feedback",
        "Celebratory animations",
        "Success states use bounce and haptic to create a satisfying confirmation.",
        Icons.Rounded.CheckCircle,
        Color(0xFF4CAF50)
    ) {
        @Composable
        override fun DemoContent() {
            var trigger by remember { mutableStateOf(false) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SuccessAnimation(trigger = trigger, onComplete = { trigger = false }) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { trigger = true }) { Text("Trigger") }
            }
        }
    },
    
    ERROR(
        "Error Shake",
        "Attention-grabbing feedback",
        "Errors shake to draw attention and provide clear negative feedback.",
        Icons.Rounded.Error,
        Color(0xFFF44336)
    ) {
        @Composable
        override fun DemoContent() {
            var trigger by remember { mutableStateOf(false) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorShakeAnimation(trigger = trigger, onComplete = { trigger = false }) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { trigger = true }) { Text("Trigger") }
            }
        }
    };
    
    @Composable
    abstract fun DemoContent()
}
