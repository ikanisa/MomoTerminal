# 🎯 Vending Module Integration Plan
**Date**: December 8, 2025 21:06 UTC  
**Duration**: 2-3 hours  
**Status**: Ready to Execute

---

## 📋 Overview

Complete the vending module integration with the main MomoTerminal app by:
1. Fixing Android UI screen signatures
2. Wiring navigation properly
3. Adding home button for vending access
4. Testing end-to-end functionality

---

## ✅ Phase 1: Fix Android UI Screen Signatures (30 min)

### Current Issues
The vending UI screens need proper Android/Compose signatures for navigation and integration.

### Tasks:

#### 1.1 Update MachinesScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/machines/MachinesScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun MachinesScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEventOrder: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MachinesViewModel = hiltViewModel()
)
```

#### 1.2 Update MachineDetailScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/detail/MachineDetailScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun MachineDetailScreen(
    machineId: String,
    onNavigateToPayment: (String, String) -> Unit, // machineId, productId
    onNavigateBack: () -> Unit,
    viewModel: MachineDetailViewModel = hiltViewModel()
)
```

#### 1.3 Update PaymentConfirmationScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/payment/PaymentConfirmationScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun PaymentConfirmationScreen(
    machineId: String,
    productId: String,
    onNavigateToCode: (String) -> Unit, // orderId
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
)
```

#### 1.4 Update CodeDisplayScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/code/CodeDisplayScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun CodeDisplayScreen(
    orderId: String,
    onNavigateToHistory: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: CodeDisplayViewModel = hiltViewModel()
)
```

#### 1.5 Update OrderHistoryScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/history/OrderHistoryScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun OrderHistoryScreen(
    onNavigateToCode: (String) -> Unit, // orderId
    onNavigateBack: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
)
```

#### 1.6 Update EventOrderScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/event/EventOrderScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun EventOrderScreen(
    eventId: String,
    onNavigateToCode: (String) -> Unit, // orderId
    onNavigateBack: () -> Unit
)
```

#### 1.7 Update VendingHelpScreen.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/ui/help/VendingHelpScreen.kt`

**Required Signature**:
```kotlin
@Composable
fun VendingHelpScreen(
    onNavigateBack: () -> Unit
)
```

---

## ✅ Phase 2: Wire Navigation (45 min)

### 2.1 Update VendingNavigation.kt
**File**: `feature/vending/src/main/java/com/momoterminal/feature/vending/navigation/VendingNavigation.kt`

**Implementation**:
```kotlin
package com.momoterminal.feature.vending.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.momoterminal.feature.vending.ui.machines.MachinesScreen
import com.momoterminal.feature.vending.ui.detail.MachineDetailScreen
import com.momoterminal.feature.vending.ui.payment.PaymentConfirmationScreen
import com.momoterminal.feature.vending.ui.code.CodeDisplayScreen
import com.momoterminal.feature.vending.ui.history.OrderHistoryScreen
import com.momoterminal.feature.vending.ui.event.EventOrderScreen
import com.momoterminal.feature.vending.ui.help.VendingHelpScreen

object VendingDestinations {
    const val VENDING_ROUTE = "vending"
    const val MACHINES_ROUTE = "vending/machines"
    const val MACHINE_DETAIL_ROUTE = "vending/machine/{machineId}"
    const val PAYMENT_ROUTE = "vending/payment/{machineId}/{productId}"
    const val CODE_DISPLAY_ROUTE = "vending/code/{orderId}"
    const val ORDER_HISTORY_ROUTE = "vending/history"
    const val EVENT_ORDER_ROUTE = "vending/event/{eventId}"
    const val HELP_ROUTE = "vending/help"
    
    fun machineDetailRoute(machineId: String) = "vending/machine/$machineId"
    fun paymentRoute(machineId: String, productId: String) = "vending/payment/$machineId/$productId"
    fun codeDisplayRoute(orderId: String) = "vending/code/$orderId"
    fun eventOrderRoute(eventId: String) = "vending/event/$eventId"
}

fun NavGraphBuilder.vendingNavGraph(
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    navigation(
        startDestination = VendingDestinations.MACHINES_ROUTE,
        route = VendingDestinations.VENDING_ROUTE
    ) {
        // Machines List Screen
        composable(VendingDestinations.MACHINES_ROUTE) {
            MachinesScreen(
                onNavigateToDetail = { machineId ->
                    navController.navigate(VendingDestinations.machineDetailRoute(machineId))
                },
                onNavigateToEventOrder = {
                    // Navigate to event order with default eventId
                    navController.navigate(VendingDestinations.eventOrderRoute("default"))
                },
                onNavigateToHistory = {
                    navController.navigate(VendingDestinations.ORDER_HISTORY_ROUTE)
                },
                onNavigateToHelp = {
                    navController.navigate(VendingDestinations.HELP_ROUTE)
                },
                onNavigateBack = onNavigateBack
            )
        }
        
        // Machine Detail Screen
        composable(
            route = VendingDestinations.MACHINE_DETAIL_ROUTE,
            arguments = listOf(navArgument("machineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
            MachineDetailScreen(
                machineId = machineId,
                onNavigateToPayment = { mId, pId ->
                    navController.navigate(VendingDestinations.paymentRoute(mId, pId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Payment Confirmation Screen
        composable(
            route = VendingDestinations.PAYMENT_ROUTE,
            arguments = listOf(
                navArgument("machineId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId") ?: return@composable
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            PaymentConfirmationScreen(
                machineId = machineId,
                productId = productId,
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestinations.codeDisplayRoute(orderId)) {
                        popUpTo(VendingDestinations.MACHINES_ROUTE)
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Code Display Screen
        composable(
            route = VendingDestinations.CODE_DISPLAY_ROUTE,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            CodeDisplayScreen(
                orderId = orderId,
                onNavigateToHistory = {
                    navController.navigate(VendingDestinations.ORDER_HISTORY_ROUTE) {
                        popUpTo(VendingDestinations.MACHINES_ROUTE)
                    }
                },
                onNavigateHome = {
                    navController.popBackStack(VendingDestinations.MACHINES_ROUTE, false)
                }
            )
        }
        
        // Order History Screen
        composable(VendingDestinations.ORDER_HISTORY_ROUTE) {
            OrderHistoryScreen(
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestinations.codeDisplayRoute(orderId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Event Order Screen
        composable(
            route = VendingDestinations.EVENT_ORDER_ROUTE,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventOrderScreen(
                eventId = eventId,
                onNavigateToCode = { orderId ->
                    navController.navigate(VendingDestinations.codeDisplayRoute(orderId)) {
                        popUpTo(VendingDestinations.MACHINES_ROUTE)
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Help Screen
        composable(VendingDestinations.HELP_ROUTE) {
            VendingHelpScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
```

### 2.2 Integrate into Main Navigation
**File**: `app/src/main/java/com/momoterminal/presentation/navigation/MomoNavGraph.kt`

**Add**:
```kotlin
import com.momoterminal.feature.vending.navigation.VendingDestinations
import com.momoterminal.feature.vending.navigation.vendingNavGraph

// Inside NavHost
vendingNavGraph(
    navController = navController,
    onNavigateBack = { navController.navigateUp() }
)
```

---

## ✅ Phase 3: Add Home Button for Vending (30 min)

### 3.1 Update HomeScreen.kt
**File**: `app/src/main/java/com/momoterminal/presentation/screens/home/HomeScreen.kt`

**Add Vending Button**:
```kotlin
// After existing quick action buttons
Card(
    modifier = Modifier
        .weight(1f)
        .height(80.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
    ),
    onClick = onNavigateToVending
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalDrink,
            contentDescription = "Vending",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Vending",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
```

**Update Signature**:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToPayment: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToVending: () -> Unit, // ADD THIS
    viewModel: HomeViewModel = hiltViewModel()
)
```

### 3.2 Update Main Navigation
**File**: `app/src/main/java/com/momoterminal/MainActivity.kt` or main nav setup

**Add Navigation Call**:
```kotlin
HomeScreen(
    // ... existing params
    onNavigateToVending = {
        navController.navigate(VendingDestinations.VENDING_ROUTE)
    }
)
```

---

## ✅ Phase 4: End-to-End Testing (45 min)

### 4.1 Test User Flow
```
Home Screen
  ↓ Click "Vending"
Machines List
  ↓ Click Machine
Machine Detail (with products)
  ↓ Select Product
Payment Confirmation
  ↓ Confirm Payment
Code Display (with QR/barcode)
  ↓ Optional: View History
Order History
  ↓ Click Order
Code Display (reopen)
```

### 4.2 Test Event Mode Flow
```
Machines List
  ↓ Click "Event Mode"
Event Order Screen
  ↓ Scan Event QR
  ↓ Select Cup Size
  ↓ Confirm
Code Display
```

### 4.3 Verify Checklist
- [ ] Navigation works between all screens
- [ ] Back buttons work correctly
- [ ] Data loads from ViewModel/Repository
- [ ] Payment flow completes successfully
- [ ] QR/Barcode displays correctly
- [ ] Order history shows completed orders
- [ ] Event mode works independently
- [ ] Home button returns to vending list
- [ ] No crashes or UI issues

---

## 📝 Implementation Checklist

### Phase 1: UI Screen Signatures (30 min)
- [ ] Update MachinesScreen.kt signature
- [ ] Update MachineDetailScreen.kt signature
- [ ] Update PaymentConfirmationScreen.kt signature
- [ ] Update CodeDisplayScreen.kt signature
- [ ] Update OrderHistoryScreen.kt signature
- [ ] Update EventOrderScreen.kt signature
- [ ] Update VendingHelpScreen.kt signature

### Phase 2: Navigation Wiring (45 min)
- [ ] Update VendingNavigation.kt with all routes
- [ ] Add navArguments for parameterized routes
- [ ] Integrate vendingNavGraph into main NavHost
- [ ] Test navigation flow

### Phase 3: Home Button (30 min)
- [ ] Add Vending button to HomeScreen
- [ ] Update HomeScreen signature
- [ ] Wire navigation from Home to Vending
- [ ] Add icon import (LocalDrink)

### Phase 4: Testing (45 min)
- [ ] Test full user flow (Home → Code Display)
- [ ] Test Event Mode flow
- [ ] Test back navigation
- [ ] Test Order History
- [ ] Verify all data loads correctly
- [ ] Check UI responsiveness
- [ ] Test error handling
- [ ] Verify build succeeds

---

## 🎯 Success Criteria

1. ✅ All screens have proper Android/Compose signatures
2. ✅ Navigation flows work end-to-end
3. ✅ Home button launches vending module
4. ✅ Users can complete purchase flow
5. ✅ QR codes display correctly
6. ✅ Order history accessible
7. ✅ Event mode functional
8. ✅ No build errors
9. ✅ No runtime crashes
10. ✅ Clean navigation stack management

---

## 🚀 Ready to Execute

**Estimated Time**: 2-3 hours  
**Priority**: High  
**Dependencies**: None (all code exists)  
**Risk**: Low (following established patterns)

**Let's begin implementation!**

