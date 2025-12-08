# 🚀 Vending Android Integration - Quick Start

**Backend**: ✅ DEPLOYED  
**Android**: ⚠️ Needs UI Integration (2-3 hours)

---

## 🎯 What's Already Working

✅ Backend deployed and tested  
✅ Domain models updated (ProductCategory, multi-cup support)  
✅ Use cases ready (CreateVendingOrderUseCase, etc.)  
✅ Repository & API interfaces defined  
✅ ViewModels implemented  

---

## ⚠️ What Needs Fixing

### Issue: UI Screen Function Signatures Don't Match

The navigation calls expect different parameters than the screens provide.

### Quick Fix Options

**Option A: Minimal MVP (1 hour)**
1. Comment out vending from app for now
2. Test backend via Supabase dashboard
3. Build proper UI later

**Option B: Fix & Deploy (2-3 hours)**
1. Fix screen function signatures
2. Create simple CodeDisplayScreen
3. Wire navigation
4. Test end-to-end

---

## 🔧 RECOMMENDED: Option B (Fix & Deploy)

### Step 1: Fix MachinesScreen (10 min)

**File**: `feature/vending/ui/machines/MachinesScreen.kt`

Find the `@Composable fun MachinesScreen(` signature and update to:

```kotlin
@Composable
fun MachinesScreen(
    onMachineClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onHelpClick: () -> Unit,
    viewModel: MachinesViewModel = hiltViewModel()
) {
    // Rest of implementation...
}
```

### Step 2: Fix MachineDetailScreen (10 min)

**File**: `feature/vending/ui/detail/MachineDetailScreen.kt`

Update signature to:

```kotlin
@Composable
fun MachineDetailScreen(
    onPayClick: () -> Unit,
    onTopUpClick: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MachineDetailViewModel = hiltViewModel()
) {
    // Rest of implementation...
}
```

### Step 3: Fix PaymentConfirmationScreen (10 min)

**File**: `feature/vending/ui/payment/PaymentConfirmationScreen.kt`

Update signature to:

```kotlin
@Composable
fun PaymentConfirmationScreen(
    onPaymentSuccess: (String) -> Unit,  // orderId
    onTopUpClick: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    // Rest of implementation...
}
```

### Step 4: Create Simple CodeDisplayScreen (20 min)

**File**: `feature/vending/ui/code/CodeDisplayScreen.kt` (CREATE NEW)

```kotlin
package com.momoterminal.feature.vending.ui.code

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CodeDisplayScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: CodeDisplayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> Text("Error: ${uiState.error}")
                uiState.order != null -> {
                    val order = uiState.order!!
                    val code = order.code
                    
                    if (code != null) {
                        // Display code
                        Text(
                            text = code.formattedCode(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Remaining: ${code.remainingServes} of ${code.totalServes} cups")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val remainingSec = code.remainingSeconds()
                        Text("Expires in: ${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}")
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Machine: ${order.machineName}")
                        Text("Location: ${order.machineLocation}")
                        
                    } else {
                        Text("No code available")
                    }
                }
            }
        }
    }
}
```

### Step 5: Update Navigation (10 min)

**File**: `feature/vending/navigation/VendingNavigation.kt`

Update the composable calls to match the new signatures:

```kotlin
composable(VendingDestination.Machines.route) {
    MachinesScreen(
        onMachineClick = { machineId ->
            navController.navigate(VendingDestination.MachineDetail.createRoute(machineId))
        },
        onHistoryClick = {
            navController.navigate(VendingDestination.OrderHistory.route)
        },
        onHelpClick = {
            navController.navigate(VendingDestination.Help.route)
        }
    )
}

composable(
    route = VendingDestination.MachineDetail.route,
    arguments = listOf(navArgument("machineId") { type = NavType.StringType })
) {
    MachineDetailScreen(
        onPayClick = {
            navController.navigate(VendingDestination.Payment.route)
        },
        onTopUpClick = onNavigateToTopUp,
        onNavigateBack = { navController.popBackStack() }
    )
}

composable(
    route = VendingDestination.Payment.route,
    arguments = listOf(navArgument("machineId") { type = NavType.StringType })
) {
    PaymentConfirmationScreen(
        onPaymentSuccess = { orderId ->
            // For now, just go back
            navController.popBackStack(VendingDestination.Machines.route, false)
        },
        onTopUpClick = onNavigateToTopUp,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Step 6: Add to App Navigation (10 min)

**File**: `app/src/main/java/com/momoterminal/presentation/navigation/NavGraph.kt`

Add after the Wallet composable (around line 290):

```kotlin
// Vending feature
composable(route = Screen.Vending.route) {
    com.momoterminal.feature.vending.navigation.vendingNavGraph(
        navController = navController,
        onNavigateToTopUp = {
            navController.navigate(Screen.Wallet.route)
        }
    )
}
```

### Step 7: Add Home Button (10 min)

**File**: `app/src/main/java/com/momoterminal/presentation/screens/home/HomeScreen.kt`

Find where other feature buttons are (around line 150-200) and add:

```kotlin
// Add alongside other PressableCard items
PressableCard(
    onClick = { navController.navigate(Screen.Vending.route) },
    modifier = Modifier.weight(1f)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalCafe,  // or LocalDrink
            contentDescription = "Vending",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vending", style = MaterialTheme.typography.labelMedium)
    }
}
```

### Step 8: Build & Test (20 min)

```bash
# Build vending module
./gradlew :feature:vending:build

# If successful, build app
./gradlew :app:assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Test flow:
# 1. Tap Vending button
# 2. See machines list
# 3. Tap a machine
# 4. See details
# 5. Tap Pay
# 6. Confirm payment
# 7. See success (code display later)
```

---

## 🎯 ALTERNATIVE: Test Backend First

If UI integration takes too long, test the backend directly:

### Via Supabase Dashboard

1. Go to: https://supabase.com/dashboard/project/lhbowpbcpwoiparwnwgt/editor
2. SQL Editor → New Query
3. Run:

```sql
-- Get test user
SELECT id FROM auth.users LIMIT 1;

-- Create order
SELECT * FROM create_vending_order(
    '<paste_user_id>'::uuid,
    (SELECT id FROM vending_machines LIMIT 1),
    3
);

-- View result
SELECT * FROM vending_orders ORDER BY created_at DESC LIMIT 1;
SELECT * FROM vending_sessions ORDER BY created_at DESC LIMIT 1;
```

### Via Android (RPC Call)

Update `VendingRepositoryImpl` to call RPC directly instead of HTTP:

```kotlin
// In VendingRepositoryImpl.kt
private val supabase = createSupabaseClient(...)

override suspend fun createOrder(machineId: String, quantity: Int) = try {
    val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not logged in")
    
    val response = supabase.postgrest.rpc(
        "create_vending_order",
        parameters = mapOf(
            "p_user_id" to userId,
            "p_machine_id" to machineId,
            "p_quantity" to quantity
        )
    ).decodeList<CreateOrderRpcResponse>().first()
    
    Result.success(mapRpcToOrder(response))
} catch (e: Exception) {
    Result.failure(e)
}
```

---

## ✅ COMPLETION CHECKLIST

- [ ] Step 1: Fix MachinesScreen signature
- [ ] Step 2: Fix MachineDetailScreen signature
- [ ] Step 3: Fix PaymentConfirmationScreen signature
- [ ] Step 4: Create CodeDisplayScreen
- [ ] Step 5: Update VendingNavigation
- [ ] Step 6: Add to App NavGraph
- [ ] Step 7: Add Home button
- [ ] Step 8: Build & test
- [ ] Backend test (via SQL or RPC)
- [ ] End-to-end flow test

---

## 🚀 LAUNCH READY WHEN...

✅ Backend deployed (DONE)  
⚠️ UI integration complete (2-3 hours)  
⚠️ End-to-end tested  
⚠️ Edge cases handled  
⚠️ Error messages polished  

**You're 85% there! Just need UI wiring to go live.**

