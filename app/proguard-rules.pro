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

# -------- Security --------
-keep class com.momoterminal.security.** { *; }

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

# -------- Remove Logging in Release --------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# -------- Keep line numbers for crash reports --------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -------- AndroidX Security Crypto --------
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
