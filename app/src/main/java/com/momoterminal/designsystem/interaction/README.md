# Interaction & Motion Layer

A comprehensive interaction system for building fluid, tactile experiences in Jetpack Compose.

## Architecture

```
interaction/
├── InteractionCore.kt      # Press, swipe, drag states
├── AnimatedContainers.kt   # Visibility, stagger, content switching
├── InteractiveBottomSheet.kt # Draggable bottom sheet
├── BouncyList.kt           # Animated list components
├── ScreenTransitions.kt    # Navigation transitions
├── GestureUtils.kt         # Long-press, double-tap, 2D drag
├── ScrollEffects.kt        # Parallax, fade, collapse on scroll
├── FeedbackAnimations.kt   # Success, error, loading animations
└── README.md
```

## Motion Principles

### 1. Natural Physics
- **Springs** for interactive gestures (drag, fling) - feels physical
- **Tweens** for state transitions (enter/exit) - predictable timing
- **Keyframes** for complex multi-stage animations (loading, success)

### 2. Timing Guidelines
| Context | Duration | Easing |
|---------|----------|--------|
| Micro-interactions | 100ms | EaseOut |
| Quick utilities | 200ms | EaseOutExpo |
| Standard transitions | 300ms | EaseOutExpo |
| Financial confirmations | 450ms | EaseFinancial |
| Emphasis/errors | 500ms | EaseOutBack |

### 3. When to Use What
```
Spring → Drag, fling, interactive gestures
Tween  → State transitions, enter/exit
Keyframes → Complex multi-stage animations
```

## Core Components

### InteractionCore.kt

#### PressableState
```kotlin
val pressState = rememberPressableState()

Box(
    modifier = Modifier
        .scale(pressState.scale)
        .pressable(pressState, onClick = { /* action */ })
)
```

#### SwipeRevealState
```kotlin
val swipeState = rememberSwipeRevealState()

Box(
    modifier = Modifier
        .offset { animatedSwipeOffset(swipeState) }
        .swipeToReveal(swipeState, onReveal = { /* action */ })
)
```

### AnimatedContainers.kt

#### AnimatedVisibilityContainer
```kotlin
AnimatedVisibilityContainer(
    visible = isVisible,
    style = AnimationStyle.FadeScale,  // or FadeSlideUp, Expand, etc.
    enterDelay = 100
) {
    Content()
}
```

#### StaggeredAnimatedItem
```kotlin
items.forEachIndexed { index, item ->
    StaggeredAnimatedItem(visible = true, index = index) {
        ItemContent(item)
    }
}
```

#### AnimatedContentSwitcher
```kotlin
AnimatedContentSwitcher(targetState = currentState) { state ->
    when (state) {
        Loading -> LoadingContent()
        Success -> SuccessContent()
    }
}
```

### InteractiveBottomSheet.kt

```kotlin
val sheetState = rememberInteractiveSheetState()

// Show sheet
scope.launch { sheetState.show(SheetPosition.Half) }

// In UI
InteractiveBottomSheet(
    state = sheetState,
    onDismiss = { /* handle dismiss */ }
) {
    SheetContent()
}
```

Features:
- Smooth drag with velocity tracking
- Snap to positions (Hidden, Peek, Half, Expanded)
- Resistance at edges
- Scale/opacity feedback
- Haptic on snap

### BouncyList.kt

#### BouncyLazyColumn
```kotlin
BouncyLazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    items(data) { item ->
        AnimatedListItem(index = data.indexOf(item)) {
            ItemContent(item)
        }
    }
}
```

#### SwipeableListItem
```kotlin
SwipeableListItem(
    onSwipeLeft = { deleteItem() },
    rightContent = {
        DeleteAction()
    }
) {
    ItemContent()
}
```

### ScreenTransitions.kt

```kotlin
NavHost(
    enterTransition = { enterTransition() },
    exitTransition = { exitTransition() },
    popEnterTransition = { popEnterTransition() },
    popExitTransition = { popExitTransition() }
) {
    // routes
}
```

## Animation Styles

| Style | Enter | Exit |
|-------|-------|------|
| `Fade` | Fade in | Fade out |
| `FadeScale` | Fade + scale from 92% | Fade + scale to 92% |
| `FadeSlideUp` | Fade + slide from bottom | Fade + slide to top |
| `FadeSlideDown` | Fade + slide from top | Fade + slide to bottom |
| `FadeSlideLeft` | Fade + slide from right | Fade + slide to left |
| `FadeSlideRight` | Fade + slide from left | Fade + slide to right |
| `Expand` | Fade + expand vertically | Fade + shrink vertically |
| `ExpandHorizontal` | Fade + expand horizontally | Fade + shrink horizontally |

## Haptic Feedback

```kotlin
val view = LocalView.current

// Trigger haptic
view.performMomoHaptic(MomoHaptic.Tap)
view.performMomoHaptic(MomoHaptic.ButtonPress)
view.performMomoHaptic(MomoHaptic.Success)
view.performMomoHaptic(MomoHaptic.Warning)
```

## Performance Tuning

### For Low/Mid Devices

1. **Reduce animation complexity**
```kotlin
val reducedMotion = LocalReducedMotion.current
val duration = if (reducedMotion) 0 else MotionTokens.STANDARD
```

2. **Use hardware layers for complex animations**
```kotlin
Modifier.graphicsLayer {
    // Animations here use hardware acceleration
}
```

3. **Avoid animating layout-affecting properties**
- Prefer `scale`, `alpha`, `translationX/Y` over `size`, `padding`

4. **Batch state updates**
```kotlin
// Bad: Multiple recompositions
state1 = newValue1
state2 = newValue2

// Good: Single recomposition
updateState { copy(value1 = newValue1, value2 = newValue2) }
```

5. **Use `derivedStateOf` for computed values**
```kotlin
val isExpanded by remember {
    derivedStateOf { sheetState.progress > 0.5f }
}
```

## Example Usage

See `InteractionDemo.kt` for a complete example demonstrating:
- Bouncy list scrolling
- Animated list items with stagger
- Tap with haptic + micro-scale
- Swipe-to-reveal actions
- Interactive bottom sheet
- Smooth state transitions

## File Structure

```
interaction/
├── InteractionCore.kt      # Core utilities (pressable, swipe, drag)
├── AnimatedContainers.kt   # Visibility, stagger, content switching
├── InteractiveBottomSheet.kt # Draggable bottom sheet
├── BouncyList.kt           # List with animations
├── ScreenTransitions.kt    # Navigation transitions
└── README.md               # This file
```

## Generic Naming Convention

All components use generic names without domain-specific prefixes:
- `PressableState` not `PaymentPressableState`
- `SwipeRevealState` not `TransactionSwipeState`
- `InteractiveBottomSheet` not `PaymentDetailSheet`

---

## Additional Components

### GestureUtils.kt

#### Long Press with Progress
```kotlin
Box(
    modifier = Modifier.longPressable(
        onLongPress = { /* action */ },
        onProgress = { progress -> /* 0f to 1f */ },
        duration = 500L
    )
)
```

#### Double Tap
```kotlin
Box(modifier = Modifier.doubleTappable(onDoubleTap = { /* action */ }))
```

#### 2D Drag with Spring-back
```kotlin
val dragState = rememberDragState(bounds = -200f..200f)
val offset = animatedDragOffset(dragState)

Box(
    modifier = Modifier
        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
        .draggable2D(dragState)
)
```

### ScrollEffects.kt

#### Collapsing Header
```kotlin
val progress = collapsingHeaderProgress(listState, headerHeight = 200)
```

#### Parallax / Fade / Scale on Scroll
```kotlin
Image(modifier = Modifier.parallaxScroll(listState, rate = 0.5f))
Text(modifier = Modifier.fadeOnScroll(listState))
Box(modifier = Modifier.scaleOnScroll(listState, minScale = 0.8f))
```

### FeedbackAnimations.kt

#### Success / Error / Loading
```kotlin
SuccessAnimation(trigger = showSuccess) { Icon(Icons.Default.Check) }
ErrorShakeAnimation(trigger = hasError) { TextField(...) }
LoadingRotation(isLoading = true) { Icon(Icons.Default.Refresh) }
PulseAnimation(enabled = true) { Badge() }
```

#### Animated Counter
```kotlin
val count = animatedCount(targetValue = 1234)
Text("$count items")
```
