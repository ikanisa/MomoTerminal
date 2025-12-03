plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.superapp.core.database"
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
}

dependencies {
    implementation(Libs.Room.runtime)
    implementation(Libs.Room.ktx)
    kapt(Libs.Room.compiler)
    
    implementation(Libs.Hilt.android)
    kapt(Libs.Hilt.compiler)
}
