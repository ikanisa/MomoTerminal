import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.baselineprofile)
    id("jacoco")
}

// Load version properties for automated version management
val versionProps = Properties().apply {
    val versionFile = rootProject.file("version.properties")
    if (versionFile.exists()) {
        load(versionFile.inputStream())
    }
}

// Load local properties for certificate pins and signing config
val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

val versionMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val versionMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val versionPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val buildNumber = System.getenv("BUILD_NUMBER")?.toIntOrNull() 
    ?: versionProps.getProperty("BUILD_NUMBER", "0").toIntOrNull() 
    ?: 0

// Calculate version code: MAJOR * 10000 + MINOR * 100 + PATCH + BUILD_NUMBER
val calculatedVersionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch + buildNumber
val calculatedVersionName = "$versionMajor.$versionMinor.$versionPatch" + 
    if (buildNumber > 0) ".$buildNumber" else ""

// Certificate pin configuration - loaded from gradle.properties or local.properties
// These are used by CertificatePinnerConfig for SSL pinning
// To generate pins, run:
//   openssl s_client -connect api.momoterminal.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
val certPinPrimary = localProps.getProperty("CERT_PIN_PRIMARY")
    ?: project.findProperty("CERT_PIN_PRIMARY")?.toString()
    ?: "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

val certPinBackup = localProps.getProperty("CERT_PIN_BACKUP")
    ?: project.findProperty("CERT_PIN_BACKUP")?.toString()
    ?: "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="

val certPinRootCa = localProps.getProperty("CERT_PIN_ROOT_CA")
    ?: project.findProperty("CERT_PIN_ROOT_CA")?.toString()
    ?: "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="

// Allow placeholder pins in release builds (for testing only - NEVER use in production!)
val allowPlaceholderPins = (localProps.getProperty("ALLOW_PLACEHOLDER_PINS")
    ?: project.findProperty("ALLOW_PLACEHOLDER_PINS")?.toString()
    ?: "false").toBoolean()

android {
    namespace = "com.momoterminal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.momoterminal"
        minSdk = 24
        targetSdk = 35
        versionCode = calculatedVersionCode
        versionName = calculatedVersionName

        testInstrumentationRunner = "com.momoterminal.HiltTestRunner"

        // BuildConfig fields for environment configuration
        buildConfigField("String", "BASE_URL", "\"https://lhbowpbcpwoiparwnwgt.supabase.co/\"")
        
        // Supabase configuration
        val supabaseUrl = localProps.getProperty("SUPABASE_URL")
            ?: project.findProperty("SUPABASE_URL")?.toString()
            ?: "https://lhbowpbcpwoiparwnwgt.supabase.co"
        val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY")
            ?: project.findProperty("SUPABASE_ANON_KEY")?.toString()
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxoYm93cGJjcHdvaXBhcndud2d0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NTgxMjcsImV4cCI6MjA3NjEzNDEyN30.egf4IDQpkHCpDKeyF63G72jQmIBcgWMHmj7FVt5xgAA"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        
        // Gemini AI configuration
        val geminiApiKey = localProps.getProperty("GEMINI_API_KEY")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        
        // AI parsing feature flag (enabled by default when API key is present)
        val aiParsingEnabled = localProps.getProperty("AI_PARSING_ENABLED")?.toBoolean()
            ?: geminiApiKey.isNotBlank()
        buildConfigField("boolean", "AI_PARSING_ENABLED", "$aiParsingEnabled")
        
        // Certificate pinning configuration
        // These are loaded from local.properties or gradle.properties at build time
        // IMPORTANT: Replace placeholder pins with real pins before production deployment!
        buildConfigField("String", "CERT_PIN_PRIMARY", "\"$certPinPrimary\"")
        buildConfigField("String", "CERT_PIN_BACKUP", "\"$certPinBackup\"")
        buildConfigField("String", "CERT_PIN_ROOT_CA", "\"$certPinRootCa\"")
        buildConfigField("boolean", "ALLOW_PLACEHOLDER_PINS", "$allowPlaceholderPins")
    }

    // Release signing configuration
    // NOTE: For production, provide these values via environment variables or local.properties
    // Do NOT commit actual keystore credentials to version control
    signingConfigs {
        create("release") {
            // Load signing config from environment variables or local.properties
            val keystoreFile = System.getenv("MOMO_KEYSTORE_FILE")
                ?: localProps.getProperty("MOMO_KEYSTORE_FILE")
            val keystorePassword = System.getenv("MOMO_KEYSTORE_PASSWORD")
                ?: localProps.getProperty("MOMO_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("MOMO_KEY_ALIAS")
                ?: localProps.getProperty("MOMO_KEY_ALIAS")
            val keyPassword = System.getenv("MOMO_KEY_PASSWORD")
                ?: localProps.getProperty("MOMO_KEY_PASSWORD")

            if (keystoreFile != null && File(keystoreFile).exists()) {
                storeFile = File(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Disable Crashlytics in debug builds
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            buildConfigField("boolean", "STRICT_MODE_ENABLED", "true")
            buildConfigField("boolean", "LEAK_CANARY_ENABLED", "true")
            buildConfigField("String", "BASE_URL", "\"https://lhbowpbcpwoiparwnwgt.supabase.co/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-r8-full.pro"
            )
            buildConfigField("boolean", "STRICT_MODE_ENABLED", "false")
            buildConfigField("boolean", "LEAK_CANARY_ENABLED", "false")
            
            // Use release signing config if available, otherwise fall back to debug
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            proguardFiles("benchmark-rules.pro")
        }
    }
    
    baselineProfile {
        automaticGenerationDuringBuild = false
        saveInSrc = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    // App bundle optimization
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Build-time validation: Fail release builds with placeholder certificate pins
// Note: Validation happens at configuration time to be configuration cache compatible
run {
    val placeholderPatterns = listOf("AAAA", "BBBB", "CCCC", "YOUR_", "PLACEHOLDER")
    val pins = listOf(certPinPrimary, certPinBackup, certPinRootCa)
    val isReleaseBuild = gradle.startParameter.taskNames.any { 
        it.contains("Release", ignoreCase = true) && it.contains("assemble", ignoreCase = true)
    }
    
    if (isReleaseBuild) {
        pins.forEach { pin ->
            placeholderPatterns.forEach { pattern ->
                if (pin.contains(pattern)) {
                    throw GradleException(
                        "SECURITY ERROR: Placeholder certificate pin detected in release build.\n" +
                        "Pin: $pin\n" +
                        "Generate real pins with: openssl s_client -connect lhbowpbcpwoiparwnwgt.supabase.co:443 | " +
                        "openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64"
                    )
                }
            }
        }
        if (allowPlaceholderPins) {
            throw GradleException("SECURITY ERROR: ALLOW_PLACEHOLDER_PINS=true in release build. Set to false for production.")
        }
    }
}

// JaCoCo configuration for code coverage
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/di/**",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/Dagger*Component*.*",
        "**/*Module_*Factory.*",
        "**/Hilt_*.*"
    )
    
    val debugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")))
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/di/**",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/Dagger*Component*.*",
        "**/*Module_*Factory.*",
        "**/Hilt_*.*"
    )
    
    val debugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")))
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:os-integration"))
    implementation(project(":core:performance"))
    implementation(project(":core:i18n"))
    
    // Feature modules
    implementation(project(":feature:payment"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:settings"))
    
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // Material Design
    implementation(libs.material)

    // Jetpack Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowSizeClass)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Lottie Animations
    implementation(libs.lottie.compose)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    // Accompanist utilities
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Window (for adaptive layouts)
    implementation(libs.androidx.window)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.androidx.compiler)

    // Security - EncryptedSharedPreferences
    // Note: Using alpha version as it's the most recent with API 34 compatibility
    // The stable 1.0.0 version has known issues with some devices
    implementation(libs.security.crypto)

    // Biometric authentication
    implementation(libs.biometric)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // SQLCipher - Database Encryption for financial data protection
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.ktx)

    // Paging 3
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // WorkManager (reliable background uploading)
    implementation(libs.work.runtime)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Retrofit for API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Coroutines
    implementation(libs.coroutines.android)

    // Kotlinx Collections Immutable (for Compose stability)
    implementation(libs.kotlinx.collections.immutable)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Serialization
    implementation(libs.gson)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // Google Play Services - SMS Retriever
    implementation(libs.play.services.auth)
    implementation(libs.play.services.auth.api.phone)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)

    // Logging
    implementation(libs.timber)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.gotrue)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.utils)

    // Google Generative AI (Gemini)
    implementation(libs.generativeai)

    // DataStore
    implementation(libs.datastore.preferences)

    // Play Integrity API
    implementation(libs.play.integrity)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Vico Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Play App Update
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Play Review
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Play Install Referrer API - for tracking install attribution
    implementation(libs.play.install.referrer)

    // Play Services Ads Identifier - for Advertising ID
    implementation(libs.play.services.ads.identifier)

    // Coil Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Testing - Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.paging.testing)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    kspTest(libs.hilt.compiler)
    
    // Testing - Android Instrumented Tests
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.navigation.testing)

    // Performance Optimization
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.leakcanary)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.tracing.ktx)
    debugImplementation(libs.compose.runtime.tracing)
}
