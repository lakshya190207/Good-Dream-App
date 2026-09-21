# Original User Request

## 2026-09-18T19:21:10Z

This is a single self-contained audit; keep it small and focused.

Production-readiness audit of "GoodDream" — a Jetpack Compose Android e-commerce app for luxury sleep products. The app uses Firebase (Firestore, FCM, Crashlytics, Performance, App Check, AI/Gemini), Razorpay payments, Room database, Moshi/Retrofit, OkHttp, and EncryptedSharedPreferences. The codebase has ~50 Kotlin files across `data/`, `ui/`, `util/` layers. The audit must produce a single comprehensive findings report covering both security vulnerabilities and UI/UX flagship polish.

Working directory: d:\Avi\app
Integrity mode: development

## Requirements

### R1. Full-Depth Security Vulnerability Audit

Analyze every source file in `app/src/main/java/com/example/` and all configuration files (`AndroidManifest.xml`, `network_security_config.xml`, `build.gradle.kts`, `.env`, `.env.example`, `proguard-rules.pro`, `firestore.rules`) for security vulnerabilities. The analysis must cover:

- **Cryptography & Authentication**: Password hashing strength (SHA-256 usage in `UserSessionManager.kt`), salt quality, EncryptedSharedPreferences fallback to plaintext (`UserSessionManager.kt` line 29), session management weaknesses, admin login security (`AdminLoginScreen.kt`).
- **Data Exposure**: Hardcoded secrets, API keys in `.env` or `BuildConfig`, debug keystores shipped in the repo (`debug.keystore`), Gemini API key handling, Razorpay key handling, PII in logs.
- **Component Security**: Exported activities/services/receivers in `AndroidManifest.xml`, intent handling in `MainActivity.kt`, deep link validation.
- **Network & Transport**: `network_security_config.xml` strictness, certificate pinning presence, OkHttp interceptor configuration, Retrofit base URL handling.
- **Payment Security**: Razorpay integration in `RazorpayPaymentHelper.kt` and `CheckoutScreen.kt` — key exposure, payment verification flow, server-side validation presence.
- **Firebase Security**: Firestore rules (`firestore.rules`), App Check configuration, FCM token storage, Crashlytics PII leakage.
- **Build & Release**: R8/ProGuard rule coverage, `isMinifyEnabled`/`isShrinkResources` settings, signing config security, debug artifacts in release builds.
- **Dependency Risks**: Known CVEs in declared dependencies, outdated library versions.

### R2. Flagship UI/UX Polish Audit

Audit every screen and component in `ui/screens/` and `ui/components/` for flagship-quality visual polish, targeting a premium luxury brand feel. The analysis must cover:

- **Material 3 Compliance**: Correct use of `MaterialTheme` color roles (primary, secondary, surface, etc.), typography scale consistency, shape system usage, elevation/tonal surface hierarchy. Check `Theme.kt`, `Color.kt`, `Type.kt` for proper M3 color scheme construction.
- **Edge-to-Edge & Insets**: Verify `enableEdgeToEdge()` usage, `WindowInsets` handling across all screens (status bar, navigation bar, IME/keyboard), content not clipped behind system bars, `imePadding()` on screens with text fields (`CustomerLoginScreen.kt`, `CheckoutScreen.kt`, `AdminLoginScreen.kt`, form screens).
- **Touch Ergonomics**: Minimum 48dp touch targets on all interactive elements, proper hit area expansion with `Modifier.padding`, bottom-anchored primary actions reachable by thumb, adequate spacing between tap targets.
- **Micro-interactions & Animation**: Transitions between screens (`AnimatedContent`, `Crossfade`), loading states and shimmer skeletons (`ShimmerSkeletons.kt`), haptic feedback usage (`LuxuryHaptics.kt`), enter/exit animations on modals and sheets, scroll performance in `LazyColumn`/`LazyRow` (use of `key` in `items()`).
- **Dark Mode Parity**: Verify dark theme color scheme contrast ratios meet WCAG AA (4.5:1 for text), consistent styling across light/dark modes, no hardcoded colors bypassing `MaterialTheme`.
- **Responsive Layout**: Proper use of `fillMaxWidth`, `weight`, padding for different screen sizes, text overflow handling (`TextOverflow.Ellipsis`), image loading states with Coil.

## Acceptance Criteria

### Security Report Quality
- [ ] Every finding includes: severity (Critical/High/Medium/Low/Info), affected file path and line number(s), description of the vulnerability, concrete remediation recommendation with code snippet
- [ ] At minimum, the following known issues are identified and assessed: EncryptedSharedPreferences plaintext fallback in `UserSessionManager.kt`, SHA-256 password hashing without proper KDF (e.g., bcrypt/Argon2), debug keystore committed to repo, hardcoded salt string in `hashPasscode()`
- [ ] No false positives that would waste developer time — each finding must be a genuine issue or a defensible concern with reasoning

### UI/UX Report Quality
- [ ] Every finding includes: severity (Critical/High/Medium/Low), affected file path and line number(s), description of the issue, specific remediation recommendation with Compose code snippet
- [ ] Edge-to-edge and IME inset handling is verified for every screen that contains text input fields
- [ ] Touch target sizes are measured against the 48dp minimum for every Button, IconButton, and clickable surface across all screens
- [ ] At least 3 specific animation or micro-interaction improvement recommendations with Compose animation API code examples

### Report Structure
- [ ] Findings are organized by category (Security, then UI/UX) with a summary table at the top listing total counts by severity
- [ ] An executive summary of no more than 10 lines captures the most critical issues for immediate action
- [ ] The report is a single markdown document

## Verification Resources

The codebase itself is the primary verification resource. Key files for the auditor to cross-reference:

- `app/src/main/java/com/example/data/local/UserSessionManager.kt` — session/crypto
- `app/src/main/java/com/example/util/RazorpayPaymentHelper.kt` — payment integration
- `app/src/main/java/com/example/MainActivity.kt` — edge-to-edge, navigation, intent handling
- `app/src/main/java/com/example/ui/theme/Theme.kt` — M3 theme setup
- `app/src/main/java/com/example/ui/theme/Color.kt` — color definitions
- `app/src/main/java/com/example/ui/theme/LuxuryHaptics.kt` — haptic feedback
- `app/src/main/java/com/example/ui/components/ShimmerSkeletons.kt` — loading states
- `app/src/main/AndroidManifest.xml` — component security
- `app/src/main/res/xml/network_security_config.xml` — transport security
- `firestore.rules` — database security rules
- `app/build.gradle.kts` — build config, dependencies, signing
- `.env` / `.env.example` — secrets management
