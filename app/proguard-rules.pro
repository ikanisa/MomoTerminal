# ============================================
# MomoTerminal ProGuard Rules
# ============================================

# Keep application class
-keep class com.momoterminal.MomoTerminalApp { *; }

# -------- Retrofit --------
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# -------- OkHttp --------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# -------- Gson --------
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# -------- Room --------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# -------- Hilt --------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class *

# -------- Data Classes (keep for serialization) --------
-keep class com.momoterminal.data.remote.dto.** { *; }
-keep class com.momoterminal.data.local.entity.** { *; }
-keep class com.momoterminal.domain.model.** { *; }
-keep class com.momoterminal.api.** { *; }

# -------- NFC HCE Service --------
-keep class com.momoterminal.nfc.** { *; }
-keep class * extends android.nfc.cardemulation.HostApduService { *; }

# -------- SMS Receiver --------
-keep class com.momoterminal.sms.** { *; }
-keep class com.momoterminal.SmsReceiver { *; }

# ============================================
# Security Hardening Rules
# ============================================

# -------- Security Package Protection --------
# Keep security-related classes but allow obfuscation of internals
-keep class com.momoterminal.security.** { *; }
-keep class com.momoterminal.data.local.SecureDataStore { *; }
-keep class com.momoterminal.ui.base.SecureActivity { *; }

# -------- Certificate Pinning --------
# Keep certificate pinner configuration
-keep class com.momoterminal.security.CertificatePinnerConfig { *; }
-keepclassmembers class com.momoterminal.security.CertificatePinnerConfig {
    public <methods>;
}

# -------- Play Integrity API --------
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.momoterminal.security.PlayIntegrityManager { *; }
-keep class com.momoterminal.security.PlayIntegrityManager$* { *; }

# -------- Device Security Manager --------
-keep class com.momoterminal.security.DeviceSecurityManager { *; }
-keep class com.momoterminal.security.DeviceSecurityManager$* { *; }

# -------- App Security Initializer --------
-keep class com.momoterminal.security.AppSecurityInitializer { *; }
-keep class com.momoterminal.security.AppSecurityInitializer$* { *; }

# -------- Screen Security Manager --------
-keep class com.momoterminal.security.ScreenSecurityManager { *; }

# -------- WorkManager --------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# -------- Coroutines --------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# -------- Kotlin --------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# -------- Enums --------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -------- Parcelable --------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# -------- Serializable --------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# -------- Firebase Crashlytics --------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# -------- Firebase Analytics --------
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

# -------- Firebase Performance --------
-keep class com.google.firebase.perf.** { *; }
-dontwarn com.google.firebase.perf.**

# -------- Firebase Common --------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# -------- Google Play Services --------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# -------- Timber --------
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# ============================================
# Aggressive Debug Log Removal
# ============================================

# Remove all Android Log calls in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove all Timber debug/verbose/info logs in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove Timber$Tree debug/verbose/info logs
-assumenosideeffects class timber.log.Timber$Tree {
    public *** d(...);
    public *** v(...);
    public *** i(...);
    public *** w(...);
}

# Remove println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# -------- Keep line numbers for crash reports --------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Crypto and Security Libraries
# ============================================

# -------- AndroidX Security Crypto --------
-keep class androidx.security.crypto.** { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.MasterKey$* { *; }

# -------- Google Tink Crypto --------
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.integration.android.** { *; }

# -------- SQLCipher Database Encryption --------
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# -------- Java Crypto --------
-keep class javax.crypto.** { *; }
-keep class javax.crypto.spec.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**

# -------- DataStore --------
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# -------- Compose --------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -------- Monitoring Module --------
-keep class com.momoterminal.monitoring.** { *; }

# ============================================
# Obfuscation Optimization
# ============================================

# Enable aggressive obfuscation
-repackageclasses 'com.momoterminal.o'
-allowaccessmodification

# Optimize code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# Remove unused code
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*

# ============================================
# R8 Full Mode Compatibility
# ============================================

# Keep continuation for coroutines
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep KotlinExtensions for Retrofit
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Gson SerializedName annotations
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# R8 full mode - keep all annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault,EnclosingMethod

# NFC HCE Service - Must not be obfuscated for system to find it
-keep class com.momoterminal.NfcHceService { *; }

# -------- User Preferences DataStore --------
-keep class com.momoterminal.data.preferences.** { *; }
