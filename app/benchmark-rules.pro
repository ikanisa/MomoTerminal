# ============================================
# Benchmark Build ProGuard Rules
# ============================================
# These rules are used for benchmark builds to allow profiling
# while still maintaining release-like optimizations.

# Disable obfuscation for benchmark builds
# This allows method names to be readable in profiling tools
-dontobfuscate

# Keep method names for profiling
-keepnames class * {
    *;
}

# Keep line numbers for debugging
-keepattributes LineNumberTable,SourceFile

# Keep all annotations (useful for benchmarking tools)
-keepattributes *Annotation*

# Keep benchmark-related classes
-keep class androidx.benchmark.** { *; }
-keep class androidx.test.** { *; }

# Keep application entry points
-keep class com.momoterminal.MomoTerminalApp { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Compose functions for profiling
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep all public methods for profiling visibility
-keepclassmembers class * {
    public *;
}
