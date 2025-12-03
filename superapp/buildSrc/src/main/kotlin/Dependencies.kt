object Versions {
    const val kotlin = "1.9.22"
    const val compose = "1.6.0"
    const val composeMaterial3 = "1.2.0"
    const val composeCompiler = "1.5.8"
    const val hilt = "2.50"
    const val retrofit = "2.9.0"
    const val room = "2.6.1"
    const val coroutines = "1.7.3"
}

object Libs {
    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:1.12.0"
        const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
        const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
        const val lifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
        const val datastore = "androidx.datastore:datastore-preferences:1.0.0"
    }

    object Compose {
        const val ui = "androidx.compose.ui:ui:${Versions.compose}"
        const val material3 = "androidx.compose.material3:material3:${Versions.composeMaterial3}"
        const val preview = "androidx.compose.ui:ui-tooling-preview:${Versions.compose}"
        const val tooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
        const val activity = "androidx.activity:activity-compose:1.8.2"
        const val navigation = "androidx.navigation:navigation-compose:2.7.6"
    }

    object Hilt {
        const val android = "com.google.dagger:hilt-android:${Versions.hilt}"
        const val compiler = "com.google.dagger:hilt-compiler:${Versions.hilt}"
        const val navigation = "androidx.hilt:hilt-navigation-compose:1.1.0"
    }

    object Retrofit {
        const val core = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val gsonConverter = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
        const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:4.12.0"
    }

    object Room {
        const val runtime = "androidx.room:room-runtime:${Versions.room}"
        const val ktx = "androidx.room:room-ktx:${Versions.room}"
        const val compiler = "androidx.room:room-compiler:${Versions.room}"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    }

    object Coil {
        const val compose = "io.coil-kt:coil-compose:2.5.0"
    }
}
