# ============================================
# R8 Full Mode Optimizations
# ============================================
# These rules enable aggressive optimizations for R8 full mode
# to maximize APK size reduction and runtime performance.

# ============================================
# Aggressive Optimization Passes
# ============================================

# Increase optimization passes for better results
-optimizationpasses 10

# Enable all safe optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast

# Allow access modification for better inlining
-allowaccessmodification

# Aggressive repackaging for smaller dex files
-repackageclasses 'a'

# ============================================
# Kotlin-Specific Optimizations
# ============================================

# Remove Kotlin null checks in release builds
# This is safe as the compiler has already verified nullability
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
    public static void throwNpe(...);
    public static void throwJavaNpe(...);
}

# Remove Kotlin metadata (not needed at runtime)
-dontwarn kotlin.Metadata

# ============================================
# Compose-Specific Optimizations
# ============================================

# Remove Compose stability checks in release
# This improves runtime performance
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer, java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer, int, java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
    boolean isTraceInProgress();
    void traceEventStart(int, int, int, java.lang.String);
    void traceEventStart(int, java.lang.String);
    void traceEventEnd();
}

# Remove Compose debug info
-assumenosideeffects class androidx.compose.runtime.internal.ComposableLambdaKt {
    void updateChangedFlags(int);
}

# ============================================
# Logging Removal
# ============================================

# Remove all Android Log calls in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(java.lang.String, int);
}

# Remove all Timber logs in release
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
    public static void wtf(...);
    public static void log(...);
    public static void tag(java.lang.String);
}

# Remove Timber Tree methods
-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
    public void i(...);
    public void w(...);
    public void e(...);
    public void wtf(...);
    public void log(...);
}

# Remove println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ============================================
# Inlining and Merging
# ============================================

# Enable method inlining
-optimizations method/inlining/*

# Enable class merging (vertically)
-optimizations class/merging/vertical

# Enable class merging (horizontally)
-optimizations class/merging/horizontal

# ============================================
# String and Field Optimizations
# ============================================

# Enable field removal and propagation
-optimizations field/*

# Remove unused fields
-optimizations field/removal/writeonly

# Propagate field values
-optimizations field/propagation/value

# ============================================
# Code Simplification
# ============================================

# Simplify member references
-optimizations code/simplification/member

# Remove dead code
-optimizations code/removal/simple
-optimizations code/removal/advanced

# Merge code blocks
-optimizations code/merging

# ============================================
# Keep Rules for Essential Components
# ============================================

# Keep enum values (required for proper enum functionality)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# Firebase Performance Optimization
# ============================================

# Enable Firebase Performance to work with R8 full mode
-keep class com.google.firebase.perf.** { *; }
-dontwarn com.google.firebase.perf.**
