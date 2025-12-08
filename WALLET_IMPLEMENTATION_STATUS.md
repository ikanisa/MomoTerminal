# Navigation & Wallet Implementation - Status & Next Steps

**Date:** December 8, 2025, 8:00 PM EAT  
**Status:** 🔄 IN PROGRESS - Navigation updated, Wallet UI needs implementation

---

## ✅ COMPLETED

### 1. Navigation Structure Updated

**File:** `app/src/main/java/com/momoterminal/presentation/navigation/Screen.kt`

**Changes Made:**
- ✅ Added Wallet icons to navigation
- ✅ Changed bottom nav from `[Home, Transactions, Settings]` to `[Home, Wallet, Settings]`
- ✅ History/Transactions removed from bottom bar (will be accessible from Settings or Wallet)

**Code:**
```kotlin
// Added wallet icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountBalanceWallet

// Updated Wallet screen with nav icons
data object Wallet : Screen(
    route = "wallet",
    title = "Wallet",
    selectedIcon = Icons.Filled.AccountBalanceWallet,
    unselectedIcon = Icons.Outlined.AccountBalanceWallet
)

// Updated bottom nav items
val bottomNavItems = listOf(Home, Wallet, Settings)
```

---

## 📋 REMAINING TASKS

### 2. Create WalletViewModel (CRITICAL)

**File to create:** `app/src/main/java/com/momoterminal/presentation/screens/wallet/WalletViewModel.kt`

```kotlin
package com.momoterminal.presentation.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momoterminal.core.common.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    data class WalletUiState(
        val balance: Long = 0,
        val currency: String = "FRW",
        val recentTransactions: List<WalletTransaction> = emptyList(),
        val isLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadWalletBalance()
        loadRecentTransactions()
    }

    private fun loadWalletBalance() {
        viewModelScope.launch {
            // TODO: Load from Room database
            _uiState.update { it.copy(balance = 0) }
        }
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            // TODO: Load from Room database
            _uiState.update { it.copy(recentTransactions = emptyList()) }
        }
    }

    fun initiateTopUp(amount: Long) {
        viewModelScope.launch {
            // TODO: Create pending transaction in database
            // TODO: Listen for SMS confirmation
        }
    }

    fun generateTopUpUssd(amount: Long): String {
        // Generate MTN MoMo USSD code for top-up
        // Format: *182*8*1*PHONE*AMOUNT#
        // This will prompt user for PIN
        return "*182*8*1*250782123456*$amount#"  // TODO: Get merchant phone from prefs
    }
}
```

---

### 3. Replace Wallet Screen UI

The comprehensive Wallet UI with top-up has been designed but needs to replace the old stub at:  
`app/src/main/java/com/momoterminal/presentation/screens/wallet/WalletScreen.kt`

**Features in new design:**
- ✅ Animated balance card with shimmer effect
- ✅ Top-up dialog with amount validation (100-4000 FRW)
- ✅ Quick select buttons (500, 1K, 2K, 4K)
- ✅ USSD dialer integration
- ✅ Recent transactions list
- ✅ Empty state handling
- ✅ History button linking to transactions

---

### 4. Add Wallet Route to NavGraph

**File:** `app/src/main/java/com/momoterminal/presentation/navigation/NavGraph.kt`

**Add after Settings screen (around line 250):**
```kotlin
// Wallet screen
composable(route = Screen.Wallet.route) {
    com.momoterminal.presentation.screens.wallet.WalletScreen(
        onNavigateToTransactions = {
            navController.navigate(Screen.Transactions.route)
        }
    )
}
```

---

### 5. Add Transaction History Link in Settings

**File:** `app/src/main/java/com/momoterminal/presentation/screens/settings/SettingsScreen.kt`

**Add after "About" section:**
```kotlin
// Transaction History
Spacer(modifier = Modifier.height(16.dp))
TextButton(
    onClick = { /* Navigate to transactions */ },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.History, null)
    Spacer(Modifier.width(8.dp))
    Text("Transaction History", modifier = Modifier.weight(1f))
    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
}
```

---

### 6. Create Wallet Database Schema

**File:** `core/database/src/main/kotlin/com/momoterminal/core/database/entity/WalletTransactionEntity.kt`

```kotlin
@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: String, // "TOP_UP" or "PAYMENT"
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "status") val status: String // "PENDING", "COMPLETED", "FAILED"
)
```

**DAO:**
```kotlin
@Dao
interface WalletTransactionDao {
    @Query("SELECT SUM(CASE WHEN type = 'TOP_UP' AND status = 'COMPLETED' THEN amount ELSE 0 END) - SUM(CASE WHEN type = 'PAYMENT' AND status = 'COMPLETED' THEN amount ELSE 0 END) FROM wallet_transactions")
    fun getWalletBalance(): Flow<Long?>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC LIMIT 10")
    fun getRecentTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)
}
```

---

## 🎯 IMPLEMENTATION PRIORITY

### Phase 1: Get it Working (1-2 hours)
1. ✅ **Navigation updated** (DONE)
2. ⏳ Create WalletViewModel (30 min)
3. ⏳ Replace WalletScreen.kt with new UI (15 min)
4. ⏳ Add Wallet route to NavGraph (5 min)
5. ⏳ Test navigation flow (10 min)

### Phase 2: Backend Integration (2-3 hours)
6. Create WalletTransactionEntity and DAO
7. Add wallet balance tracking
8. Implement top-up transaction creation
9. Add SMS listener for top-up confirmation
10. Update balance on SMS receive

### Phase 3: Polish (1 hour)
11. Add transaction history to Settings
12. Test USSD flow end-to-end
13. Add error handling
14. Add loading states

---

## 📱 USER FLOW: Wallet Top-Up

```
User opens app
    ↓
Taps "Wallet" in bottom nav
    ↓
Sees balance card (animated)
    ↓
Taps "Top Up" FAB or Quick Action
    ↓
Dialog opens with amount input
    ↓
User enters amount (100-4000 FRW)
OR
User taps quick select (500/1K/2K/4K)
    ↓
Taps "Proceed to Pay"
    ↓
USSD dialer launches: *182*8*1*PHONE*AMOUNT#
    ↓
User enters PIN on USSD screen
    ↓
MTN processes payment
    ↓
SMS confirmation received
    ↓
App detects SMS
    ↓
Wallet balance updated
    ↓
Success toast shown
```

---

## 🔧 FILES TO MODIFY/CREATE

### To Create:
- ✅ `app/.../presentation/screens/wallet/WalletViewModel.kt`
- ✅ `core/database/.../entity/WalletTransactionEntity.kt`
- ✅ `core/database/.../dao/WalletTransactionDao.kt`

### To Modify:
- ✅ `app/.../presentation/navigation/Screen.kt` (DONE)
- ⏳ `app/.../presentation/navigation/NavGraph.kt`
- ⏳ `app/.../presentation/screens/wallet/WalletScreen.kt` (replace)
- ⏳ `app/.../presentation/screens/settings/SettingsScreen.kt` (add history link)
- ⏳ `core/database/.../AppDatabase.kt` (add new entity)

---

## 📊 CURRENT STATUS

| Task | Status | Time Est |
|------|--------|----------|
| Navigation structure | ✅ Done | - |
| Wallet UI design | ✅ Ready | - |
| WalletViewModel | ❌ TODO | 30 min |
| NavGraph integration | ❌ TODO | 5 min |
| Database schema | ❌ TODO | 1 hour |
| SMS integration | ❌ TODO | 1 hour |
| Testing | ❌ TODO | 30 min |

**Overall Progress:** 20% complete  
**Estimated Time Remaining:** 3-4 hours

---

## 🚀 QUICK START COMMANDS

```bash
# 1. Test current build
./gradlew assembleDebug

# 2. Create ViewModel file
# (Use code provided above)

# 3. Build and install
./gradlew installDebug

# 4. Test navigation
# - Open app
# - Check bottom nav shows: Home | Wallet | Settings
# - Tap Wallet (should see basic screen)
```

---

## ⚠️ KNOWN ISSUES

1. **Wallet screen crash** - Missing ViewModel will cause crash
   - Solution: Create WalletViewModel first

2. **History button missing** - Users can't access transaction history
   - Solution: Add to Settings or Wallet screen

3. **No database schema** - Wallet balance not persisted
   - Solution: Create WalletTransactionEntity and DAO

---

## 📝 NEXT SESSION TASKS

1. Create WalletViewModel with USSD generation
2. Replace WalletScreen.kt with new animated UI
3. Add Wallet composable to NavGraph
4. Test navigation flow (Home → Wallet → Settings)
5. Create database schema for wallet transactions
6. Implement top-up flow with SMS confirmation

---

**Status:** Ready for implementation  
**Build:** Should compile after ViewModel created  
**Priority:** HIGH - Core feature for in-app services

