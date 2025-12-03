# Android OS Integration Architecture - Domain-Agnostic Design

## Overview

This document outlines a **generic, reusable OS integration layer** that makes the super app a first-class Android citizen while remaining domain-neutral.

## Architecture Principles

1. **Domain-Agnostic**: All integrations use generic models (Entity, Action, Item)
2. **Modular**: Each integration is a separate module
3. **Compose-First**: Native Compose integration where possible
4. **Type-Safe**: Sealed classes for navigation and actions
5. **Testable**: Clear interfaces and dependency injection

## Module Structure

```
:core:os-integration/
├── notifications/
│   ├── NotificationManager.kt
│   ├── NotificationChannel.kt
│   └── NotificationModel.kt
├── deeplinks/
│   ├── DeepLinkHandler.kt
│   └── AppLinkVerifier.kt
├── shortcuts/
│   ├── ShortcutManager.kt
│   └── ShortcutModel.kt
├── widgets/
│   ├── QuickActionsWidget.kt
│   └── RecentItemsWidget.kt
└── capabilities/
    ├── LocationProvider.kt
    ├── CameraProvider.kt
    └── BiometricProvider.kt
```

## High-Level Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Android System                        │
│  (Notifications, Deep Links, Shortcuts, Widgets)        │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              :core:os-integration                        │
│  • Generic models (NotificationModel, DeepLink, etc.)   │
│  • System API wrappers                                  │
│  • Domain-agnostic handlers                             │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              Navigation Layer                            │
│  • Route mapping                                        │
│  • Deep link → Screen resolution                        │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│              Feature Modules                             │
│  • Consume OS events                                    │
│  • Provide data for widgets/shortcuts                   │
└─────────────────────────────────────────────────────────┘
```

## Key Design Decisions

### 1. Generic Models
All OS integrations use generic models that can be mapped to any domain:
- `NotificationModel` (not "PaymentNotification")
- `DeepLink` (not "TransactionDeepLink")
- `ShortcutAction` (not "PaymentShortcut")

### 2. Navigation-Centric
All OS entry points resolve to Navigation Compose routes:
- Notification tap → `navController.navigate(route)`
- Deep link → `navController.navigate(route)`
- Shortcut → `navController.navigate(route)`

### 3. Data Layer Abstraction
Widgets and shortcuts fetch data through repository interfaces:
- `RecentItemsRepository` (generic)
- `QuickActionsRepository` (generic)

### 4. Capability Providers
System services wrapped in domain-agnostic providers:
- `LocationProvider` → returns `Location` (not "NearbyMerchants")
- `CameraProvider` → returns `Bitmap` (not "ReceiptScan")
- `BiometricProvider` → returns `AuthResult` (not "PaymentAuth")
