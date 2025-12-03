# MomoTerminal Design System

A complete, domain-agnostic design system for building super apps with Kotlin, Jetpack Compose, and Material 3. Features a minimalist liquid-glass aesthetic with subtle gradients and soft 3D depth.

## Architecture

```
designsystem/
├── theme/          # Design tokens
│   ├── Color.kt    # Color palette (teal, gold, glass surfaces)
│   ├── Type.kt     # Typography (tabular figures for amounts)
│   ├── Shape.kt    # Corner radii and custom shapes
│   ├── Dimensions.kt # Spacing, sizing, elevation
│   └── Theme.kt    # MomoTerminalTheme composable
├── component/      # Reusable UI components (36 files)
├── motion/         # Animation tokens and haptics
│   ├── MotionTokens.kt # Duration, easing, spring specs
│   └── Haptics.kt  # Haptic feedback patterns
└── example/        # Showcase screens (9 files)
```

## Theme

### Usage
```kotlin
MomoTerminalTheme {
    // Your app content
}
```

### Accessing Tokens
```kotlin
val colors = MomoTheme.colors      // Extended colors (glass, gradients)
val spacing = MomoTheme.spacing    // Spacing scale (xs to xxxl)
val sizing = MomoTheme.sizing      // Icon, button, card sizes
val elevation = MomoTheme.elevation // Elevation levels
```

## Components (36)

### Layout
| Component | Description |
|-----------|-------------|
| `SurfaceScaffold` | Full-screen scaffold with gradient header and glass content |
| `SectionScaffold` | Section container with header |
| `GlassCard` | Card with glass morphism effect |
| `GlassCardGradient` | Glass card with gradient background |

### Navigation
| Component | Description |
|-----------|-------------|
| `MomoTopAppBar` | Top app bar with glass styling |
| `GlassTopAppBar` | Transparent top bar variant |
| `MomoBottomNavBar` | Bottom navigation with badges |
| `MomoTabRow` | Segmented tab row |

### Buttons
| Component | Description |
|-----------|-------------|
| `PrimaryActionButton` | Primary CTA button |
| `SecondaryActionButton` | Secondary button |
| `MomoIconButton` | Icon button (Filled, Glass, Outlined, Ghost) |
| `QuickActionButton` | Compact action button |

### Inputs
| Component | Description |
|-----------|-------------|
| `GlassTextField` | Text field with glass styling |
| `AmountTextField` | Currency amount input |
| `MomoSearchBar` | Search input with clear button |
| `MomoSwitch` | Toggle switch |
| `MomoCheckbox` | Checkbox with label |
| `MomoRadioButton` | Radio button with label |
| `MomoSlider` | Slider with value display |
| `SegmentedButton` | Multi-option selector |

### Display
| Component | Description |
|-----------|-------------|
| `MomoAvatar` | User avatar (initials or icon) |
| `MomoBadge` | Notification badge |
| `BadgedBox` | Container with badge overlay |
| `MomoChip` | Filter/selection chip |
| `FilterChipRow` | Horizontal chip group |
| `TokenChip` | Token/tag display |
| `StatusPill` | Animated status indicator |
| `StatusIndicator` | Status dot with label |
| `StatusBadge` | Status badge variant |

### Lists
| Component | Description |
|-----------|-------------|
| `MomoListItem` | Generic list row |
| `TransactionRow` | Transaction list item |
| `MomoDivider` | Horizontal divider |
| `LabeledDivider` | Divider with centered label |

### Cards
| Component | Description |
|-----------|-------------|
| `PressableCard` | Interactive card with haptics |
| `InfoCard` | Key-value info display |
| `StatCard` | Statistics card with trend |
| `NfcInfoCard` | NFC status card |
| `BalanceHeader` | Balance display header |

### Feedback
| Component | Description |
|-----------|-------------|
| `MomoSnackbar` | Toast notification (Info, Success, Warning, Error) |
| `MomoDialog` | Modal dialog |
| `ConfirmationDialog` | Confirm/cancel dialog |
| `MomoBottomSheet` | Bottom sheet modal |
| `MomoTooltip` | Tooltip popup |

### Progress
| Component | Description |
|-----------|-------------|
| `MomoProgressBar` | Linear progress with gradient |
| `StepProgressBar` | Multi-step progress |
| `MomoLoadingIndicator` | Circular spinner |
| `PulsingDots` | Animated loading dots |
| `LoadingOverlay` | Full-screen loading state |

### Empty States
| Component | Description |
|-----------|-------------|
| `EmptyState` | Empty content placeholder |

### Specialized
| Component | Description |
|-----------|-------------|
| `AnimatedBalance` | Animated currency display |
| `SmsSyncIndicator` | SMS sync status card |

## Motion Tokens

### Durations
```kotlin
MotionTokens.INSTANT    // 100ms
MotionTokens.QUICK      // 200ms
MotionTokens.STANDARD   // 300ms
MotionTokens.EMPHASIS   // 450ms (financial confirmations)
```

### Easing
```kotlin
MotionTokens.EaseOut        // Standard deceleration
MotionTokens.EaseFinancial  // Slower, "money-safe" feel
MotionTokens.EaseOutBack    // Overshoot for success states
```

### Springs
```kotlin
MotionTokens.SpringResponsive  // Quick, snappy
MotionTokens.SpringSnappy      // Medium bounce
MotionTokens.SpringGentle      // Slow, smooth
```

## Haptics

```kotlin
view.performMomoHaptic(MomoHaptic.Tap)         // Light tap
view.performMomoHaptic(MomoHaptic.ButtonPress) // Button feedback
view.performMomoHaptic(MomoHaptic.Success)     // Success confirmation
view.performMomoHaptic(MomoHaptic.Warning)     // Warning alert
view.performMomoHaptic(MomoHaptic.SmsSync)     // SMS sync complete
```

## Example Screens

| Screen | Description |
|--------|-------------|
| `ComponentCatalog` | All components showcase |
| `GenericDashboard` | Domain-agnostic dashboard |
| `WalletScreen` | Fintech wallet example |
| `InteractiveWalletScreen` | Interactive wallet with animations |
| `NfcScanScreen` | NFC payment flow |
| `MotionShowcase` | Animation demonstrations |
| `DesignSystemShowcase` | Theme tokens showcase |
| `SettingsExample` | Settings screen pattern |
| `FormExample` | Form components pattern |

## Color Palette

### Primary
- **MomoTeal**: Primary brand color
- **MomoGold**: Accent/secondary
- **MomoLime**: Tertiary

### Semantic
- **Credit**: Green for incoming money
- **Debit**: Red for outgoing money
- **Warning**: Orange for alerts

### Glass Surfaces
- **surfaceGlass**: 80-90% opacity (light), 60-70% (dark)
- **surfaceGlassElevated**: Elevated glass variant
- **glassBorder**: Subtle border for glass elements

## Spacing Scale

```
xxs: 2dp   xs: 4dp    sm: 8dp
md: 12dp   lg: 16dp   xl: 24dp
xxl: 32dp  xxxl: 48dp
```

## Usage Examples

### Basic Screen
```kotlin
@Composable
fun MyScreen() {
    MomoTerminalTheme {
        SurfaceScaffold(
            header = { MomoTopAppBar(title = "My Screen") }
        ) {
            // Content
        }
    }
}
```

### Form
```kotlin
GlassTextField(
    value = text,
    onValueChange = { text = it },
    placeholder = "Enter text",
    leadingIcon = Icons.Rounded.Person
)
```

### List
```kotlin
GlassCard {
    items.forEach { item ->
        MomoListItem(
            title = item.title,
            subtitle = item.subtitle,
            leading = { MomoAvatar(initials = item.initials) }
        )
        MomoDivider()
    }
}
```
