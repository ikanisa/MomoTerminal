plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.superapp.core.network"
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
    implementation(Libs.Retrofit.core)
    implementation(Libs.Retrofit.gsonConverter)
    implementation(Libs.Retrofit.loggingInterceptor)
    implementation(Libs.Hilt.android)
    kapt(Libs.Hilt.compiler)
}
