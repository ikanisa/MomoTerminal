plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.momoterminal.core.osintegration"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.activity.compose)
    
    // Widgets (Glance)
    implementation("androidx.glance:glance-appwidget:1.0.0")
    
    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Camera
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    
    // Biometric
    implementation(libs.biometric)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
