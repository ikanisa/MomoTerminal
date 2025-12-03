package com.momoterminal.designsystem.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.component.*
import com.momoterminal.designsystem.interaction.*
import com.momoterminal.designsystem.motion.MomoHaptic
import com.momoterminal.designsystem.motion.MotionTokens
import com.momoterminal.designsystem.motion.performMomoHaptic
import com.momoterminal.designsystem.theme.MomoTerminalTheme
import com.momoterminal.designsystem.theme.MomoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Demo data class for list items.
 */
data class DemoItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isPositive: Boolean = true
)

/**
 * Comprehensive interaction demo showcasing:
 * - Bouncy list scrolling
 * - Animated list items with stagger
 * - Tap with haptic + micro-scale
 * - Swipe-to-reveal actions
 * - Interactive bottom sheet
 * - Smooth state transitions
 */
@Composable
fun InteractionDemo() {
    MomoTerminalTheme {
        val scope = rememberCoroutineScope()
        val view = LocalView.current
        val sheetState = rememberInteractiveSheetState()
        var selectedItem by remember { mutableStateOf<DemoItem?>(null) }
        var items by remember { mutableStateOf(generateDemoItems()) }
        var isLoading by remember { mutableStateOf(true) }
        
        // Simulate loading
        LaunchedEffect(Unit) {
            delay(500)
            isLoading = false
        }
        
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Header with animated title
                InteractionDemoHeader(
                    onRefresh = {
                        scope.launch {
                            isLoading = true
                            view.performMomoHaptic(MomoHaptic.ButtonPress)
                            delay(800)
                            items = generateDemoItems()
                            isLoading = false
                        }
                    }
                )
                
                // Main list
                AnimatedContentSwitcher(targetState = isLoading) { loading ->
                    if (loading) {
                        LoadingState()
                    } else {
                        DemoList(
                            items = items,
                            onItemClick = { item ->
                                selectedItem = item
                                scope.launch { sheetState.show(SheetPosition.Half) }
                            },
                            onItemDelete = { item ->
                                items = items.filter { it.id != item.id }
                                view.performMomoHaptic(MomoHaptic.Warning)
                            }
                        )
                    }
                }
            }
            
            // Bottom sheet
            if (sheetState.isVisible) {
                InteractiveBottomSheet(
                    state = sheetState,
                    onDismiss = { selectedItem = null }
                ) {
                    selectedItem?.let { item ->
                        ItemDetailSheet(
                            item = item,
                            onClose = {
                                scope.launch {
                                    sheetState.hide()
                                    selectedItem = null
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
private fun InteractionDemoHeader(onRefresh: () -> Unit) {
    val pressState = rememberPressableState()
    val scale by animateFloatAsState(
        targetValue = pressState.scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "headerScale"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Interaction Demo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap, swipe, drag interactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // Animated refresh button
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pressable(pressState, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { index ->
            ShimmerContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun DemoList(
    items: List<DemoItem>,
    onItemClick: (DemoItem) -> Unit,
    onItemDelete: (DemoItem) -> Unit
) {
    BouncyLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            SwipeableListItem(
                onSwipeLeft = { onItemDelete(item) },
                rightContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE53935), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                }
            ) {
                AnimatedListItem(
                    index = items.indexOf(item),
                    onClick = { onItemClick(item) }
                ) {
                    DemoItemCard(item)
                }
            }
        }
    }
}

@Composable
private fun DemoItemCard(item: DemoItem) {
    val colors = MomoTheme.colors
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceGlass,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isPositive) colors.credit.copy(alpha = 0.15f)
                            else colors.debit.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (item.isPositive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (item.isPositive) colors.credit else colors.debit
                    )
                }
                
                Column {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                item.amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isPositive) colors.credit else colors.debit
            )
        }
    }
}

@Composable
private fun ItemDetailSheet(
    item: DemoItem,
    onClose: () -> Unit
) {
    val colors = MomoTheme.colors
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated amount
        var showAmount by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(200)
            showAmount = true
        }
        
        AnimatedVisibilityContainer(
            visible = showAmount,
            style = AnimationStyle.FadeScale
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isPositive) colors.credit.copy(alpha = 0.15f)
                            else colors.debit.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (item.isPositive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (item.isPositive) colors.credit else colors.debit,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    item.amount,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPositive) colors.credit else colors.debit
                )
                
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Action buttons with stagger
        StaggeredAnimatedItem(visible = showAmount, index = 0) {
            PrimaryActionButton(
                text = "Share Receipt",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        StaggeredAnimatedItem(visible = showAmount, index = 1) {
            SecondaryActionButton(
                text = "Close",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun generateDemoItems(): List<DemoItem> {
    val names = listOf("John Doe", "Jane Smith", "Alex Johnson", "Maria Garcia", "David Lee")
    val actions = listOf("Payment received", "Transfer sent", "Refund", "Subscription", "Purchase")
    
    return (1..10).map { i ->
        val isPositive = i % 3 != 0
        DemoItem(
            id = "item_$i",
            title = names.random(),
            subtitle = actions.random(),
            amount = "${if (isPositive) "+" else "-"}$${(100..9999).random()}",
            isPositive = isPositive
        )
    }
}
