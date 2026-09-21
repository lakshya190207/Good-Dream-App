# ==============================================================================
# Production R8 & ProGuard Optimization Configuration
# ==============================================================================

# Preserve line numbers and source files for Crashlytics stack trace deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations and generic signatures for reflection / serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# Kotlin Coroutines & Flow
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# Moshi & JSON Data Models
# ------------------------------------------------------------------------------
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
# Keep all generated JsonAdapters
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# ------------------------------------------------------------------------------
# Retrofit & OkHttp
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ------------------------------------------------------------------------------
# AndroidX Room Database & SQLite
# ------------------------------------------------------------------------------
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# ------------------------------------------------------------------------------
# AndroidX Security Crypto & Keystore
# ------------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }

# ------------------------------------------------------------------------------
# Timber Structured Logging
# ------------------------------------------------------------------------------
-dontwarn timber.log.**
-keep class timber.log.** { *; }

# ------------------------------------------------------------------------------
# Firebase Crashlytics & Performance Monitoring
# ------------------------------------------------------------------------------
-keepattributes *Annotation*,SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------------------
# Firebase Cloud Messaging
# ------------------------------------------------------------------------------
-keep class com.example.data.remote.SanctuaryFirebaseMessagingService { <init>(); }

# ------------------------------------------------------------------------------
# Release Logcat Stripping (Eliminate info/debug logging leakage in production)
# ------------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# Razorpay Payment Gateway SDK
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}
-optimizations !class/merging/vertical*,!class/merging/horizontal*
-keepclasseswithmembers class * {
    public void onPaymentSuccess(...);
    public void onPaymentError(...);
}
