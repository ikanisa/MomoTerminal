plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.superapp.feature.featurea"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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
    implementation(project(":core:ui"))

    implementation(Libs.Compose.ui)
    implementation(Libs.Compose.material3)
    implementation(Libs.Compose.preview)
    debugImplementation(Libs.Compose.tooling)

    implementation(Libs.AndroidX.lifecycleViewModelCompose)
    implementation(Libs.Compose.navigation)
    
    implementation(Libs.Hilt.android)
    implementation(Libs.Hilt.navigation)
    kapt(Libs.Hilt.compiler)
}
