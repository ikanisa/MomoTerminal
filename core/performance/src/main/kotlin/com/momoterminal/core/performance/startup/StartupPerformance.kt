package com.momoterminal.core.performance.startup

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 1. APP INITIALIZER - Critical path only
@Singleton
class AppInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    fun initializeCritical() {
        // Only critical components on main thread
        // Everything else deferred
    }
    
    fun initializeDeferred() {
        appScope.launch {
            // Analytics
            initializeAnalytics()
            
            // Crashlytics
            initializeCrashlytics()
            
            // WorkManager periodic tasks
            initializeBackgroundWork()
            
            // Preload critical data
            preloadCriticalData()
        }
    }
    
    private suspend fun initializeAnalytics() {
        // Initialize analytics SDK
    }
    
    private suspend fun initializeCrashlytics() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
    
    private suspend fun initializeBackgroundWork() {
        // Schedule periodic sync, cleanup, etc.
    }
    
    private suspend fun preloadCriticalData() {
        // Preload frequently accessed data
    }
}

// 2. DEFERRED INITIALIZER - Using AndroidX Startup
class DeferredInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Runs automatically, off main thread
        // Use for non-critical initialization
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

// 3. APPLICATION CLASS
class SuperApp : Application() {
    @Inject lateinit var appInitializer: AppInitializer
    
    override fun onCreate() {
        super.onCreate()
        
        // Critical initialization only
        appInitializer.initializeCritical()
        
        // Defer everything else
        appInitializer.initializeDeferred()
    }
}

// 4. SPLASH SCREEN STRATEGY
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var isReady by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Wait for critical data
        delay(500) // Max wait time
        isReady = true
        onTimeout()
    }
    
    // Fast first paint - simple logo
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null
        )
    }
}

// 5. BASELINE PROFILE GENERATOR
// In :baselineprofile module
@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()
    
    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        packageName = "com.momoterminal",
        maxIterations = 15,
        stableIterations = 3
    ) {
        // Critical user journeys
        startActivityAndWait()
        
        // Navigate to main screens
        device.findObject(By.text("Home")).click()
        device.waitForIdle()
        
        device.findObject(By.text("Feature A")).click()
        device.waitForIdle()
        
        // Scroll lists
        val recycler = device.findObject(By.scrollable(true))
        recycler?.setGestureMargin(device.displayWidth / 5)
        recycler?.fling(Direction.DOWN)
        device.waitForIdle()
    }
}

// GRADLE CONFIGURATION
/*
// app/build.gradle.kts
android {
    buildTypes {
        release {
            // Enable baseline profiles
            baselineProfile {
                automaticGenerationDuringBuild = true
            }
        }
    }
}

dependencies {
    // Baseline profiles
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
}

// baselineprofile/build.gradle.kts
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.momoterminal.baselineprofile"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
*/

// 6. STARTUP METRICS
@Singleton
class StartupMetrics @Inject constructor() {
    private val startTime = System.currentTimeMillis()
    
    fun recordFirstFrame() {
        val duration = System.currentTimeMillis() - startTime
        // Log to analytics
        logMetric("cold_start_ms", duration)
    }
    
    fun recordTimeToInteractive() {
        val duration = System.currentTimeMillis() - startTime
        logMetric("time_to_interactive_ms", duration)
    }
    
    private fun logMetric(name: String, value: Long) {
        // Send to Firebase Performance or custom analytics
    }
}

// 7. LAZY INITIALIZATION PATTERN
@Singleton
class HeavyComponent @Inject constructor() {
    // Lazy initialization
    private val expensiveResource by lazy {
        // Only initialized when first accessed
        createExpensiveResource()
    }
    
    private fun createExpensiveResource(): Any {
        // Heavy initialization
        return Any()
    }
}
