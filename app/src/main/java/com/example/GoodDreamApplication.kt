package com.example

import android.app.Application
import android.util.Log
import com.example.data.config.AppConfigProvider
import com.example.data.config.FirebaseRemoteConfigHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import android.os.StrictMode
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import timber.log.Timber

@HiltAndroidApp
class GoodDreamApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
        initializeLogging()
        initializeFirebaseAppCheck()
        AppConfigProvider.fetchAtStartup(this)
        FirebaseRemoteConfigHelper.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 2, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .crossfade(100)
            .respectCacheHeaders(false)
            .build()
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                        return
                    }
                    val crashlytics = FirebaseCrashlytics.getInstance()
                    val sanitized = sanitizeLogMessage(message)
                    crashlytics.log("[${tag ?: "APP"}] $sanitized")
                    if (t != null) {
                        crashlytics.recordException(t)
                    }
                }
            })
        }
    }

    private fun sanitizeLogMessage(message: String): String {
        var sanitized = message.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")) { mr ->
            val email = mr.value
            val at = email.indexOf('@')
            if (at <= 1) "***@***" else "${email.take(2)}***${email.substring(at)}"
        }
        sanitized = sanitized.replace(Regex("(?i)(code|otp|passcode)[:\\s]+(\\d{6})")) { mr ->
            "${mr.groupValues[1]}: ******"
        }
        sanitized = sanitized.replace(Regex("(?i)(token|key)[:\\s=]+([A-Za-z0-9_\\-]{20,})")) { mr ->
            "${mr.groupValues[1]}: [REDACTED]"
        }
        return sanitized
    }

    /**
     * Configures Firebase App Check with ReCAPTCHA integration:
     * - In production builds, uses RecaptchaAppCheckProviderFactory with the configured reCAPTCHA site key.
     * - In debug/development builds or local JVM tests, uses DebugAppCheckProviderFactory.
     * - Safe error handling ensures the app functions cleanly even if Firebase credentials or network are deferred.
     */
    private fun initializeFirebaseAppCheck() {
        try {
            val firebaseApp = if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            } else {
                FirebaseApp.getInstance()
            }

            if (firebaseApp != null) {
                val appCheck = FirebaseAppCheck.getInstance(firebaseApp)

                // Retrieve reCAPTCHA Enterprise Site Key
                val recaptchaSiteKey = try {
                    val field = BuildConfig::class.java.getField("RECAPTCHA_SITE_KEY")
                    field.get(null) as? String
                } catch (_: Exception) {
                    null
                } ?: getString(R.string.recaptcha_site_key)

                val providerFactory = if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Configuring Firebase App Check with DebugAppCheckProviderFactory")
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    Log.i(TAG, "Configuring Firebase App Check with RecaptchaAppCheckProviderFactory")
                    RecaptchaAppCheckProviderFactory.getInstance(recaptchaSiteKey)
                }

                appCheck.installAppCheckProviderFactory(providerFactory)
                appCheck.setTokenAutoRefreshEnabled(true)
                isAppCheckConfigured = true
                activeProviderName = if (BuildConfig.DEBUG) "Debug Provider" else "reCAPTCHA Enterprise"
                Log.i(TAG, "Firebase App Check successfully configured with $activeProviderName")
            } else {
                Log.w(TAG, "FirebaseApp instance not found. App Check initialization deferred.")
            }
        } catch (e: Exception) {
            // Gracefully log so that preview and development continue uninterrupted without crash
            Log.w(TAG, "Firebase App Check initialization deferred: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GoodDreamAppCheck"
        var isAppCheckConfigured: Boolean = false
            private set
        var activeProviderName: String = "reCAPTCHA Enterprise"
            private set
    }
}
