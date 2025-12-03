# OS Integration - Usage Examples

## Quick Start Guide

### 1. Add Dependency

In your feature module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":core:os-integration"))
}
```

### 2. Inject Providers

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val notificationManager: AppNotificationManager,
    private val deepLinkHandler: DeepLinkHandler,
    private val shortcutManager: AppShortcutManager,
    private val locationProvider: LocationProvider,
    private val cameraProvider: CameraProvider,
    private val biometricProvider: BiometricProvider
) : ViewModel()
```

## Real-World Examples

### Example 1: Send Notification with Deep Link

```kotlin
// In any ViewModel
fun notifyUser(itemId: String, itemTitle: String) {
    notificationManager.show(
        NotificationModel(
            id = itemId.hashCode(),
            channelType = NotificationChannelType.UPDATES,
            title = "New Item Available",
            message = itemTitle,
            deepLink = "app://feature/item/$itemId",
            actions = listOf(
                NotificationAction("view", "View Now", "app://feature/item/$itemId"),
                NotificationAction("later", "Later", "app://dismiss")
            )
        )
    )
}
```

### Example 2: Handle Deep Link in MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var deepLinkHandler: DeepLinkHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            
            // Handle deep link from intent
            LaunchedEffect(intent) {
                intent.data?.let { uri ->
                    deepLinkHandler.handleDeepLink(navController, uri)
                }
            }
            
            AppNavHost(navController)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Trigger re-composition to handle new deep link
    }
}
```

### Example 3: Update Dynamic Shortcuts

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val shortcutManager: AppShortcutManager,
    private val repository: ItemRepository
) : ViewModel() {
    
    init {
        updateShortcuts()
    }
    
    private fun updateShortcuts() {
        viewModelScope.launch {
            val recentItems = repository.getRecentItems(limit = 4)
            
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

### Example 4: Location-Aware Feature

```kotlin
@Composable
fun NearbyItemsScreen(
    viewModel: NearbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }
    
    if (!hasPermission) {
        RequestLocationPermission(
            onPermissionGranted = { 
                hasPermission = true
                viewModel.onEvent(NearbyEvent.LoadNearbyItems)
            },
            onPermissionDenied = { 
                // Show rationale or fallback UI
            }
        )
    } else {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.items.isNotEmpty() -> ItemList(uiState.items)
            else -> EmptyState()
        }
    }
}

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val repository: ItemRepository
) : BaseViewModel<NearbyUiState, NearbyEvent, NearbyEffect>(
    initialState = NearbyUiState()
) {
    override fun onEvent(event: NearbyEvent) {
        when (event) {
            is NearbyEvent.LoadNearbyItems -> loadNearbyItems()
        }
    }
    
    private fun loadNearbyItems() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            
            when (val result = locationProvider.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val location = result.location
                    val items = repository.getNearbyItems(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusKm = 5.0
                    )
                    updateState { copy(items = items, isLoading = false) }
                }
                is LocationResult.Error -> {
                    updateState { copy(error = result.message, isLoading = false) }
                }
                is LocationResult.PermissionDenied -> {
                    updateState { copy(error = "Location permission denied", isLoading = false) }
                }
            }
        }
    }
}
```

### Example 5: Camera Capture

```kotlin
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(false) }
    
    if (!hasPermission) {
        RequestCameraPermission(
            onPermissionGranted = { hasPermission = true },
            onPermissionDenied = { /* Handle denial */ }
        )
    } else {
        Column {
            Button(onClick = {
                scope.launch {
                    viewModel.captureImage(activity)
                }
            }) {
                Text("Scan Item")
            }
        }
    }
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val cameraProvider: CameraProvider,
    private val repository: ItemRepository
) : ViewModel() {
    
    suspend fun captureImage(activity: FragmentActivity) {
        when (val result = cameraProvider.captureImage(activity)) {
            is CameraResult.Success -> {
                // Process bitmap (OCR, upload, etc.)
                val bitmap = result.bitmap
                processImage(bitmap)
            }
            is CameraResult.Error -> {
                // Handle error
            }
            else -> { /* Handle other cases */ }
        }
    }
    
    private suspend fun processImage(bitmap: Bitmap) {
        // Domain-agnostic image processing
        // Could be: receipt scan, QR code, document, etc.
    }
}
```

### Example 6: Biometric Authentication

```kotlin
@Composable
fun SecureActionScreen(
    viewModel: SecureViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    
    Button(onClick = {
        scope.launch {
            viewModel.performSecureAction(activity)
        }
    }) {
        Text("Perform Secure Action")
    }
}

@HiltViewModel
class SecureViewModel @Inject constructor(
    private val biometricProvider: BiometricProvider,
    private val repository: SecureRepository
) : ViewModel() {
    
    suspend fun performSecureAction(activity: FragmentActivity) {
        when (biometricProvider.authenticate(
            activity = activity,
            title = "Authenticate",
            subtitle = "Verify your identity to continue"
        )) {
            is BiometricResult.Success -> {
                // Perform secure operation
                repository.performSensitiveOperation()
            }
            is BiometricResult.Error -> {
                // Handle error
            }
            is BiometricResult.Cancelled -> {
                // User cancelled
            }
            is BiometricResult.NotAvailable -> {
                // Fallback to PIN/password
            }
        }
    }
}
```

### Example 7: Widget Data Provider

```kotlin
// Implement the interface in your feature module
@Singleton
class TransactionWidgetDataProvider @Inject constructor(
    private val transactionRepository: TransactionRepository
) : WidgetDataProvider {
    
    override suspend fun getRecentItems(limit: Int): List<RecentItem> {
        return transactionRepository.getRecent(limit).map { transaction ->
            RecentItem(
                id = transaction.id,
                title = transaction.title,
                subtitle = "${transaction.amount} ${transaction.currency}"
            )
        }
    }
    
    override suspend fun getQuickActions(): List<QuickAction> {
        return listOf(
            QuickAction("new_payment", "New Payment", "app://payment/new"),
            QuickAction("history", "History", "app://transactions"),
            QuickAction("settings", "Settings", "app://settings")
        )
    }
}

// Bind in DI module
@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetDataProvider(
        impl: TransactionWidgetDataProvider
    ): WidgetDataProvider
}
```

### Example 8: Navigation with Deep Links

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    deepLinkHandler: DeepLinkHandler
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        
        // Feature with deep link support
        composable(
            route = "feature/item/{itemId}",
            arguments = listOf(
                navArgument("itemId") { 
                    type = NavType.StringType
                    nullable = true
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "app://feature/item/{itemId}" },
                navDeepLink { uriPattern = "https://momoterminal.com/feature/item/{itemId}" }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            ItemDetailScreen(itemId = itemId)
        }
        
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

## Testing Examples

### Test Notifications

```kotlin
@HiltAndroidTest
class NotificationTest {
    @Inject lateinit var notificationManager: AppNotificationManager
    
    @Test
    fun `notification is shown with correct content`() {
        notificationManager.show(
            NotificationModel(
                id = 1,
                channelType = NotificationChannelType.GENERAL,
                title = "Test",
                message = "Test message"
            )
        )
        
        // Verify notification is displayed
        onView(withText("Test")).check(matches(isDisplayed()))
    }
}
```

### Test Deep Links

```kotlin
@Test
fun `deep link parses correctly`() {
    val handler = DeepLinkHandler()
    val uri = Uri.parse("app://feature/item/123")
    
    val result = handler.parseDeepLink(uri)
    
    assertThat(result).isInstanceOf(DeepLink.FeatureA::class.java)
    assertThat((result as DeepLink.FeatureA).itemId).isEqualTo("123")
}
```

### Test Location Provider

```kotlin
@Test
fun `location provider returns current location`() = runTest {
    val provider = LocationProvider(context)
    
    val result = provider.getCurrentLocation()
    
    assertThat(result).isInstanceOf(LocationResult.Success::class.java)
}
```

## Best Practices

1. **Notifications**: Use appropriate channels, don't spam users
2. **Deep Links**: Always validate parameters before navigation
3. **Shortcuts**: Update on app launch and when data changes
4. **Widgets**: Keep data fresh, update efficiently
5. **Location**: Request only when needed, explain usage
6. **Camera**: Release resources immediately after use
7. **Biometric**: Always provide fallback authentication

## Performance Tips

1. **Lazy initialization**: Don't initialize providers until needed
2. **Cache results**: Cache location, biometric availability
3. **Background updates**: Use WorkManager for widget/shortcut updates
4. **Efficient queries**: Limit data fetched for widgets/shortcuts
5. **Resource cleanup**: Release camera, location listeners promptly

---

**All examples are domain-agnostic and can be adapted to any business logic!**
