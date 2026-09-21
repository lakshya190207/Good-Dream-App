# Android Production Engineering Guidelines

This repository follows production-grade Android development standards for Jetpack Compose, Kotlin Coroutines, Clean Architecture, and Google Play Store compliance.

---

## 1. Architectural Blueprint
- **Pattern**: Clean Architecture with MVI/MVVM and Unidirectional Data Flow (UDF).
- **Layers**:
  - `data/`: Local storage (Room), remote APIs (Retrofit/OkHttp), Repositories (single source of truth).
  - `domain/` (when applicable): Pure Kotlin Use Cases encapsulating business logic.
  - `ui/`: Jetpack Compose screens, components, and ViewModels.
- **State Management**:
  - Expose immutable `StateFlow<UiState>` from ViewModels.
  - Consume state in Compose using `collectAsStateWithLifecycle()`.
  - Represent UI states as `sealed interface` (e.g., `Loading`, `Success`, `Error`, `Empty`).
  - Never expose `MutableStateFlow` outside ViewModel classes.

---

## 2. Jetpack Compose Performance
- **Recomposition Optimization**:
  - Mark immutable data classes with `@Immutable` or use primitive/stable properties.
  - Use `key` inside `items()` for `LazyColumn` and `LazyRow`.
  - Wrap high-frequency calculations or object allocations with `remember` or `derivedStateOf`.
  - Hoist state up and pass lambdas down (`onItemClick: (String) -> Unit`).
- **Edge-to-Edge & System Bars**:
  - Utilize `WindowInsets` (`safeDrawingPadding`, `systemBarsPadding`) properly across all screens.
  - Set dynamic status bar icons matching background luminance.

---

## 3. Concurrency & Asynchronous Operations
- **Coroutines**:
  - Use `viewModelScope` for UI-triggered operations.
  - Switch to `Dispatchers.IO` for database, file, or network I/O.
  - Always handle exceptions using `runCatching` or `CoroutineExceptionHandler` to prevent unhandled crashes.
  - Never use `GlobalScope`.

---

## 4. Security & Data Protection
- **Secrets**:
  - Never hardcode API keys, secrets, or bearer tokens in code. Use `BuildConfig` populated from `.env` via secrets plugin.
- **Data at Rest**:
  - Use `EncryptedSharedPreferences` via `androidx.security:security-crypto` for auth tokens and user credentials.
- **Networking**:
  - Cleartext traffic is disabled in production. Enforce HTTPS across all Retrofit endpoints.
- **Component Security**:
  - Keep `android:exported="false"` for all Activities, Services, and Receivers unless explicitly required.

---

## 5. Observability & Logging
- **Logging**:
  - Use `Timber` for structured logging.
  - In `Application.onCreate()`, plant `Timber.DebugTree()` ONLY in `BuildConfig.DEBUG`.
  - In release builds, plant a Crashlytics reporting tree for warnings and errors. Never log PII (Personally Identifiable Information).
- **Crash Reporting**:
  - Log non-fatal handled exceptions via `FirebaseCrashlytics.getInstance().recordException(e)`.

---

## 6. Build, Obfuscation & R8
- Ensure all data models used with Moshi / Retrofit are annotated with `@JsonClass(generateAdapter = true)` or covered in `proguard-rules.pro`.
- Release builds must have `isMinifyEnabled = true` and `isShrinkResources = true`.

---

## 7. Statutory E-Commerce & Consumer Protection Compliance
- **Legal Metrology Disclosures**:
  - Every product detail view (`ProductDetailScreen`) MUST prominently disclose:
    1. **Country of Origin** (e.g., *India*).
    2. **Manufacturer & Packer Identity**: Registered legal entity name and complete physical address.
    3. **Net Quantity & Physical Dimensions** (e.g., *1 Unit Mattress — 72" x 78"*).
    4. **Customer Care Contacts**: Dedicated email and phone number for consumer queries.
- **Consumer Protection (E-Commerce) Rules 2020**:
  - The app MUST display a designated **Grievance Redressal Officer** with:
    - Officer Name & Designation (*Nodal Grievance Officer*).
    - Official contact email and phone.
    - Explicit statutory timeline: *Acknowledgment within 48 hours, resolution within 30 days*.
- **DPDP Act 2023 & Contractual Consent**:
  - The Checkout screen MUST require or explicitly state affirmative agreement before payment:
    *"By placing your order, you agree to Good Dream's Terms of Service and Privacy Policy."* with clickable links opening the policy sheets.
- **Payment & Card Data (RBI PA/PG Guidelines)**:
  - The app must NEVER collect, log, or persist raw credit/debit card numbers or CVVs. All card processing must be delegated to PCI-DSS Level 1 compliant gateways (e.g. Razorpay).
