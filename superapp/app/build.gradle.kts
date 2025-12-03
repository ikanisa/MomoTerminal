plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.superapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.superapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    
    implementation(project(":feature:featureA"))

    implementation(Libs.AndroidX.coreKtx)
    implementation(Libs.AndroidX.lifecycleRuntime)
    
    implementation(Libs.Compose.ui)
    implementation(Libs.Compose.material3)
    implementation(Libs.Compose.preview)
    implementation(Libs.Compose.activity)
    implementation(Libs.Compose.navigation)
    debugImplementation(Libs.Compose.tooling)

    implementation(Libs.Hilt.android)
    implementation(Libs.Hilt.navigation)
    kapt(Libs.Hilt.compiler)
}
