# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK installation.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Retrofit models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.momoterminal.api.** { *; }

# Keep NFC service
-keep class com.momoterminal.nfc.** { *; }
