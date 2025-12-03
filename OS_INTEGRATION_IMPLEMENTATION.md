# OS Integration Implementation Guide

## Complete Implementation Checklist

### 1. Module Setup

Create `:core:os-integration` module:

```bash
mkdir -p core/os-integration/src/main/kotlin/com/momoterminal/core/osintegration/{notifications,deeplinks,shortcuts,widgets,capabilities}
```

**build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    
    // Notifications
    implementation(libs.androidx.core.ktx)
    
    // Deep Links
    implementation(libs.androidx.navigation.compose)
    
    // Shortcuts
    implementation(libs.androidx.core)
    
    // Widgets
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    
    // Location
    implementation(libs.play.services.location)
    
    // Camera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    
    // Biometric
    implementation(libs.androidx.biometric)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

### 2. AndroidManifest Configuration

**app/src/main/AndroidManifest.xml:**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Permissions -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    
    <application>
        <activity android:name=".MainActivity">
            
            <!-- Deep Links: App Scheme -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="app" />
            </intent-filter>
            
            <!-- App Links: HTTPS (Verified) -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" />
                <data android:host="momoterminal.com" />
                <data android:pathPrefix="/feature" />
            </intent-filter>
            
            <!-- Shortcuts -->
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
        </activity>
        
        <!-- Widgets -->
        <receiver
            android:name=".widgets.QuickActionsWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_info_quick_actions" />
        </receiver>
        
        <receiver
            android:name=".widgets.RecentItemsWidget"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_info_recent_items" />
        </receiver>
        
        <service
            android:name=".widgets.RecentItemsRemoteViewsService"
            android:permission="android.permission.BIND_REMOTEVIEWS"
            android:exported="false" />
    </application>
</manifest>
```

### 3. Navigation Integration

**app/navigation/AppNavHost.kt:**

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    deepLinkHandler: DeepLinkHandler
) {
    // Handle deep links from intent
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        (context as? ComponentActivity)?.intent?.data?.let { uri ->
            deepLinkHandler.handleDeepLink(navController, uri)
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        
        // Feature A with deep link support
        composable(
            route = "featureA/{itemId}",
            arguments = listOf(navArgument("itemId") { 
                type = NavType.StringType
                nullable = true
            }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "app://featureA/item/{itemId}" },
                navDeepLink { uriPattern = "https://momoterminal.com/featureA/item/{itemId}" }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            FeatureAScreen(itemId = itemId)
        }
        
        // Feature B
        composable(
            route = "featureB/{actionId}",
            deepLinks = listOf(
                navDeepLink { uriPattern = "app://featureB/action/{actionId}" }
            )
        ) { backStackEntry ->
            val actionId = backStackEntry.arguments?.getString("actionId")
            FeatureBScreen(actionId = actionId)
        }
        
        // Settings
        composable(
            route = "settings",
            deepLinks = listOf(
                navDeepLink { uriPattern = "app://settings" }
            )
        ) {
            SettingsScreen()
        }
    }
}
```

### 4. Dependency Injection

**core/os-integration/di/OsIntegrationModule.kt:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object OsIntegrationModule {
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): AppNotificationManager = AppNotificationManager(context)
    
    @Provides
    @Singleton
    fun provideDeepLinkHandler(): DeepLinkHandler = DeepLinkHandler()
    
    @Provides
    @Singleton
    fun provideShortcutManager(
        @ApplicationContext context: Context
    ): AppShortcutManager = AppShortcutManager(context)
    
    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider = LocationProvider(context)
    
    @Provides
    @Singleton
    fun provideCameraProvider(
        @ApplicationContext context: Context
    ): CameraProvider = CameraProvider(context)
    
    @Provides
    @Singleton
    fun provideBiometricProvider(
        @ApplicationContext context: Context
    ): BiometricProvider = BiometricProvider(context)
}
```

### 5. Usage Examples

#### Sending a Notification

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val notificationManager: AppNotificationManager
) : ViewModel() {
    
    fun sendUpdateNotification() {
        notificationManager.show(
            NotificationModel(
                id = 1001,
                channelType = NotificationChannelType.UPDATES,
                title = "New Update",
                message = "Check out what's new",
                deepLink = "app://featureA",
                actions = listOf(
                    NotificationAction("view", "View", "app://featureA"),
                    NotificationAction("dismiss", "Later", "app://dismiss")
                )
            )
        )
    }
}
```

#### Handling Deep Links

```kotlin
class MainActivity : ComponentActivity() {
    @Inject lateinit var deepLinkHandler: DeepLinkHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            
            // Handle initial deep link
            LaunchedEffect(intent) {
                intent.data?.let { uri ->
                    deepLinkHandler.handleDeepLink(navController, uri)
                }
            }
            
            AppNavHost(navController, deepLinkHandler)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        intent?.data?.let { uri ->
            // Trigger navigation via event or state
        }
    }
}
```

#### Creating Shortcuts

```kotlin
@HiltViewModel
class ShortcutViewModel @Inject constructor(
    private val shortcutManager: AppShortcutManager,
    private val repository: RecentItemsRepository
) : ViewModel() {
    
    fun updateShortcuts() {
        viewModelScope.launch {
            val recentItems = repository.getRecentItems(limit = 3)
            
            val shortcuts = recentItems.mapIndexed { index, item ->
                AppShortcut(
                    id = "recent_${item.id}",
                    shortLabel = item.title,
                    longLabel = "Open ${item.title}",
                    iconResId = R.drawable.ic_item,
                    deepLink = "app://feature/item/${item.id}",
                    rank = index
                )
            }
            
            shortcutManager.updateShortcuts(shortcuts)
        }
    }
}
```

#### Using Location

```kotlin
@Composable
fun NearbyFeature(
    locationProvider: LocationProvider = hiltViewModel()
) {
    var location by remember { mutableStateOf<Location?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    
    if (!hasPermission) {
        RequestLocationPermission(
            onPermissionGranted = { hasPermission = true },
            onPermissionDenied = { /* Show rationale */ }
        )
    } else {
        LaunchedEffect(Unit) {
            when (val result = locationProvider.getCurrentLocation()) {
                is LocationResult.Success -> {
                    location = result.location
                    // Use location for any domain-specific feature
                }
                is LocationResult.Error -> { /* Handle error */ }
                is LocationResult.PermissionDenied -> { /* Handle denial */ }
            }
        }
    }
}
```

## Domain-Agnostic Design Principles

### 1. Generic Models
All models use generic terminology:
- ✅ `NotificationModel` (not PaymentNotification)
- ✅ `DeepLink` (not TransactionDeepLink)
- ✅ `RecentItem` (not RecentTransaction)

### 2. Feature-Agnostic URIs
Deep links use generic paths:
- ✅ `app://feature/item/{id}`
- ✅ `app://feature/action/{id}`
- ❌ `app://payment/transaction/{id}` (too specific)

### 3. Capability Providers Return Generic Types
- `LocationProvider` → `Location` (not "NearbyMerchants")
- `CameraProvider` → `Bitmap` (not "ScannedReceipt")
- `BiometricProvider` → `AuthResult` (not "PaymentAuth")

### 4. Repository Interfaces
Widgets and shortcuts use generic repository interfaces:
```kotlin
interface RecentItemsRepository {
    suspend fun getRecentItems(limit: Int): List<RecentItem>
}

// Implementation can be domain-specific
class TransactionRecentItemsRepository : RecentItemsRepository {
    override suspend fun getRecentItems(limit: Int): List<RecentItem> {
        return transactionRepository.getRecent(limit).map { tx ->
            RecentItem(tx.id, tx.title, tx.subtitle)
        }
    }
}
```

## Testing

### Unit Tests

```kotlin
class DeepLinkHandlerTest {
    private val handler = DeepLinkHandler()
    
    @Test
    fun `parse feature A deep link`() {
        val uri = Uri.parse("app://featureA/item/123")
        val result = handler.parseDeepLink(uri)
        
        assertThat(result).isInstanceOf(DeepLink.FeatureA::class.java)
        assertThat((result as DeepLink.FeatureA).itemId).isEqualTo("123")
    }
}
```

### Integration Tests

```kotlin
@HiltAndroidTest
class NotificationIntegrationTest {
    @Inject lateinit var notificationManager: AppNotificationManager
    
    @Test
    fun `notification with deep link navigates correctly`() {
        notificationManager.show(
            NotificationModel(
                id = 1,
                channelType = NotificationChannelType.GENERAL,
                title = "Test",
                message = "Test message",
                deepLink = "app://featureA"
            )
        )
        
        // Verify notification is shown
        // Tap notification
        // Verify navigation to featureA
    }
}
```

## Performance Considerations

1. **Widgets**: Update only when data changes, use WorkManager for periodic updates
2. **Shortcuts**: Limit to 4 dynamic shortcuts, update on app launch
3. **Location**: Use last known location first, request new only if stale
4. **Camera**: Release resources immediately after capture
5. **Biometric**: Cache availability check, don't check on every use

## Security Considerations

1. **Deep Links**: Validate all parameters before navigation
2. **Notifications**: Don't include sensitive data in notification content
3. **Widgets**: Don't display sensitive information
4. **Biometric**: Always have fallback authentication method
5. **Location**: Request only when needed, explain usage to user

---

**Result**: A complete, domain-agnostic OS integration layer that makes the super app a first-class Android citizen while remaining reusable across any business domain.
