---
name: android-process-death-restoration
description: >-
  Essential guide for surviving Android OS process death, configuration changes, low-memory kills, and restoring state reliably using SavedStateHandle, rememberSaveable, and custom Savers.
---

# Android Process Death & State Restoration Runbook

Use this skill when handling screen orientation changes, background process termination by the Android OS low-memory killer (LMK), maintaining user input across navigation, or diagnosing `NullPointerException` crashes when resuming apps.

---

## 1. The Anatomy of Process Death

When a user backgrounds your app (to take a phone call, open another app, or lock the phone):
1. **Activity is destroyed**, but the process remains in memory.
2. Under memory pressure, the **Android OS terminates your app process silently**.
3. When the user taps the app in Recent Apps, Android creates a **new process** and recreates the top Activity with the saved instance Bundle.
4. **Any in-memory singleton, global state, or standard ViewModel variable is reset to initial null/default values!** If code assumes in-memory state exists, it crashes instantly.

---

## 2. Solution: ViewModel with `SavedStateHandle`

`SavedStateHandle` persists small primitive bundles (key-value pairs) through process death.

### A. Persisting Selected Filters & IDs
```kotlin
class ProductDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    // Automatically reads from SavedStateHandle; survives Process Death!
    val productId: StateFlow<String> = savedStateHandle.getStateFlow(
        key = "product_id", 
        initialValue = ""
    )

    fun setProductId(id: String) {
        savedStateHandle["product_id"] = id
    }
}
```

### B. Compose UI State Restoration with `rememberSaveable`
Use `rememberSaveable` instead of `remember` for user input, scroll states, and modal visibility:
```kotlin
// WRONG: User types message, rotates phone or answers call -> Text is wiped!
var searchQuery by remember { mutableStateOf("") }

// PRODUCTION-SAFE: Survives rotation AND process death!
var searchQuery by rememberSaveable { mutableStateOf("") }
```

### C. Custom Saver for Complex Data Classes
For objects that cannot fit directly into standard Bundle primitives:
```kotlin
data class SearchFilter(val query: String, val minPrice: Double, val maxPrice: Double)

val SearchFilterSaver = mapSaver(
    save = { mapOf("query" to it.query, "min" to it.minPrice, "max" to it.maxPrice) },
    restore = { 
        SearchFilter(
            query = it["query"] as String,
            minPrice = it["min"] as Double,
            maxPrice = it["max"] as Double
        )
    }
)

@Composable
fun FilterableCatalogScreen() {
    var currentFilter by rememberSaveable(stateSaver = SearchFilterSaver) {
        mutableStateOf(SearchFilter("", 0.0, 10000.0))
    }
}
```

---

## 3. How to Test Process Death (Verification Command)

Never release an app without testing Process Death!

```powershell
# 1. Open app and navigate to a detailed screen / type something into form
# 2. Press Home button to put app in background
# 3. Terminate the app process via adb:
adb shell am kill com.aistudio.gooddream.kxmpzq

# 4. Re-open app from Recent Apps
# 5. VERIFY: The app must restore the screen seamlessly without crashing!
```
