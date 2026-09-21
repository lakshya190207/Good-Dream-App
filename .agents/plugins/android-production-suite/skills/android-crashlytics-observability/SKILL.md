---
name: android-crashlytics-observability
description: >-
  Production observability runbook for Android apps, integrating Timber structured logging, Firebase Crashlytics non-fatal exception reporting, and Firebase Performance monitoring.
---

# Android Observability, Logging & Crash Reporting

Use this skill when setting up app telemetry, diagnosing production exceptions, implementing custom logging trees, monitoring network latency, or tracking app startup performance.

---

## 1. Timber Structured Logging Setup

Never use `android.util.Log` directly in production code. Use `Timber` to ensure debug logs are automatically stripped from release APKs.

### A. Custom Crashlytics Timber Tree
```kotlin
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return // Never log debug or verbose logs in production release
        }

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log("[${tag ?: "APP"}] $message")

        if (t != null) {
            crashlytics.recordException(t)
        }
    }
}
```

### B. Application Initialization
In `GoodDreamApplication.kt`:
```kotlin
class GoodDreamApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }
}
```

---

## 2. Non-Fatal Exception Reporting & Breadcrumbs

### A. Setting User Identifier
```kotlin
fun setCrashlyticsUser(userId: String) {
    FirebaseCrashlytics.getInstance().setUserId(userId)
}
```

### B. Logging Non-Fatal Exceptions
```kotlin
suspend fun fetchCatalogSafely(): Result<List<ProductItem>> {
    return try {
        val result = apiService.getCatalog()
        Result.success(result)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch catalog from remote service")
        FirebaseCrashlytics.getInstance().recordException(e)
        Result.failure(e)
    }
}
```

### C. Adding Contextual Custom Keys
```kotlin
fun recordCheckoutFailure(cartId: String, amount: Double, errorReason: String) {
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("cart_id", cartId)
        setCustomKey("checkout_amount", amount)
        setCustomKey("checkout_error", errorReason)
        log("Checkout failed during payment gateway step")
    }
}
```

---

## 3. Firebase Performance Monitoring

Track critical flows such as App Cold Start, Catalog Load, or Checkout.

### A. Tracing Custom Operations
```kotlin
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

inline fun <T> traceOperation(traceName: String, block: () -> T): T {
    val trace: Trace = FirebasePerformance.getInstance().newTrace(traceName)
    trace.start()
    return try {
        block()
    } finally {
        trace.stop()
    }
}
```
