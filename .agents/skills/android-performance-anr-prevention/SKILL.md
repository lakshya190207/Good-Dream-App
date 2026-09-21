---
name: android-performance-anr-prevention
description: >-
  Fail-safe guide for eliminating Application Not Responding (ANR) dialogs, memory leaks with LeakCanary, StrictMode violations, and generating Baseline Profiles for smooth 60/120fps Compose performance.
---

# Android Performance, ANR Prevention & Memory Leak Elimination

Use this skill when auditing app responsiveness, eliminating UI freeze/jank, detecting memory leaks, configuring StrictMode, or creating Baseline Profiles to keep Play Store Core Vitals in the top tier (preventing algorithmic demotion on the Play Store).

---

## 1. Zero ANR Guarantee: The Main Thread Contract

Google Play tracks your app's **Bad Behavior Rate** (ANRs > 0.47% triggers Play Store search rank demotion).

### A. StrictMode in Debug Builds
Detect accidentally running disk reads, SQLite queries, or network calls on the Main Thread:
```kotlin
// In Application.onCreate() during DEBUG:
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .penaltyFlashScreen()
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
```

### B. Coroutine Dispatcher Safety Rule
Any database, shared preferences, cryptography, bitmap decoding, or network call MUST explicitly switch to `Dispatchers.IO` or `Dispatchers.Default`:
```kotlin
// WRONG (Will cause micro-stutters and eventual ANR under load):
fun calculateCartTotal(items: List<CartItem>): Double {
    return items.sumOf { it.price * it.quantity } // Main thread!
}

// PRODUCTION-SAFE:
suspend fun calculateCartTotal(items: List<CartItem>): Double = withContext(Dispatchers.Default) {
    items.sumOf { it.price * it.quantity }
}
```

---

## 2. LeakCanary Memory Leak Detection

Memory leaks occur when long-lived objects (Singletons, static variables, background coroutines) hold references to short-lived objects (Activities, Fragments, View hierarchy Contexts).

### A. LeakCanary Setup
In `app/build.gradle.kts`:
```kotlin
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
```
*(No code needed: LeakCanary installs itself automatically in debug builds).*

### B. Most Common Compose Memory Leaks & Fixes
1. **Passing Activity Context instead of Application Context to singletons**:
   ```kotlin
   // LEAK:
   class AnalyticsManager(private val context: Context) // Activity context passed!

   // FIX:
   class AnalyticsManager(context: Context) {
       private val appContext = context.applicationContext
   }
   ```
2. **Uncollected Coroutine Jobs in ViewModels**:
   Always launch in `viewModelScope` so jobs are cancelled when the ViewModel clears.
3. **Static or Singleton Listeners without Unregistration**:
   Use `DisposableEffect` in Compose to register and unregister callbacks/sensors/broadcast receivers safely.

---

## 3. Baseline Profiles for 30%+ App Startup Boost

Baseline Profiles pre-compile critical user paths into machine code during app installation, eliminating JIT compilation stutters.

### A. Add Macrobenchmark Module
```powershell
# Generated via Android Studio -> New Module -> Baseline Profile Generator
```

### B. Baseline Profile Rule
```kotlin
@OptIn(ExperimentalBaselineProfilesApi::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = "com.aistudio.gooddream.kxmpzq",
            includeInStartupProfile = true
        ) {
            // Cold start flow
            pressHome()
            startActivityAndWait()

            // Scroll catalog
            device.wait(Until.hasObject(By.res("catalog_list")), 5_000)
            val catalog = device.findObject(By.res("catalog_list"))
            catalog.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }
}
```
