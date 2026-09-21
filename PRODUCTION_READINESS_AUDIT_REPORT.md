# GoodDream Android Luxury E-Commerce Application — Master Production-Readiness Audit Report

**Target Application**: GoodDream (Luxury Sleep Sanctuary & Bespoke Mattress E-Commerce)  
**Target Platform**: Android (Kotlin 2.0.21, Jetpack Compose, Material 3, SDK 26–35)  
**Audit Scope**: Full Codebase Security (R1) & Flagship UI/UX Ergonomics (R2)  
**Date**: September 19, 2026  
**Status**: Comprehensive Production-Readiness Evaluation Complete  

---

## Unified Findings Severity Matrix

| Severity | Security (R1) | UI/UX & Polish (R2) | Combined Total | Primary Impact Summary |
| :--- | :---: | :---: | :---: | :--- |
| **Critical** | 6 | 9 | **15** | Arbitrary admin access, unauthenticated data destruction, payment bypass, form keyboard occlusion, WCAG 1.05:1 failure |
| **High** | 5 | 19 | **24** | Cleartext storage fallback, weak KDF, API key exposure, sub-48dp touch targets, missing M3 typography/color tokens |
| **Medium** | 4 | 11 | **15** | Hardcoded test keys, lack of certificate pinning, PII logging, unhandled 2.0x font scaling, missing haptics |
| **Low** | 1 | 3 | **4** | Unverified App Links deep links, surface tonal elevation hierarchy, minor color token inconsistencies |
| **Info** | 1 | 0 | **1** | Missing repository .env/.env.example template |
| **TOTAL** | **17** | **42** | **59** | **Complete Codebase, Cloud Infrastructure & UI Component Scope** |

---

## Executive Summary

The GoodDream Android application demonstrates strong visual ambitions and rich domain features, but is currently **NOT production-ready** due to 15 Critical and 24 High-severity defects across security, compliance, and user experience. The most severe vulnerabilities include hardcoded master admin credentials and plaintext Google App Passwords in client source code; unauthenticated Cloud Firestore security rules permitting global catalog deletion, inquiry PII exfiltration, and order tampering; client-only Razorpay payment processing without cryptographic server-side HMAC validation; and release signing fallback to a committed debug keystore. Furthermore, the UI suffers from severe edge-to-edge defects where the soft keyboard occludes input fields on all form screens, bottom checkout CTAs collide with Android system navigation bars, and mattress specifications in dark mode fail WCAG AA contrast (1.05:1). All Critical and High defects must be remediated following this report's concrete specifications prior to commercial distribution or Google Play Store submission.

---

# Part I: R1 Full-Depth Security Vulnerability Audit

This security audit evaluated all 52 Kotlin production source files, Android platform manifests, cloud security rules, dependency manifests, and build configurations against the OWASP Mobile Top 10, CWE standards, and Google Play Store Developer Policies.

```
Security Audit Domain Breakdown (17 Findings):
├── 1. Cryptography & Authentication (4 Findings: 2 Critical, 2 High)
├── 2. Data Exposure & Secret Management (5 Findings: 2 Critical, 1 High, 2 Medium)
├── 3. Component & Intent Security (1 Finding: 1 Low)
├── 4. Network & Transport Security (1 Finding: 1 Medium)
├── 5. Payment Security — Razorpay (1 Finding: 1 Critical)
├── 6. Firebase & Cloud Infrastructure (2 Findings: 1 Critical, 1 Medium)
├── 7. Build, Obfuscation & Release (2 Findings: 1 High, 1 Info)
└── 8. Dependency Risks & Third-Party Libraries (1 Finding: 1 Medium)
```

---

## Domain 1: Cryptography & Authentication

### FINDING-SEC-AUTH-01: EncryptedSharedPreferences Silent Fallback to Plaintext Storage
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/data/local/UserSessionManager.kt`
- **Line Numbers**: 15–32
- **Vulnerability Description**:
  The `UserSessionManager` class initializes `EncryptedSharedPreferences` backed by the Android Keystore. However, if any runtime exception is thrown during Keystore initialization (common on older devices, OEM ROMs with hardware keystore bugs, or after backup restoration), the exception block catches `Exception` and silently falls back to standard, unencrypted `SharedPreferences`:
  ```kotlin
  } catch (e: Exception) {
      Timber.w(e, "Falling back to standard SharedPreferences due to Keystore initialization issue")
      context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
  }
  ```
- **Impact Analysis**:
  Customer session tokens, hashed passcodes, physical addresses, phone numbers, full names, and push notification tokens are written to cleartext XML at `/data/data/com.example.gooddream/shared_prefs/gooddream_user_session.xml`. On rooted devices or via standard Android backup extraction (`adb backup`), an adversary can extract these customer credentials without root bypass.
- **Remediation**:
  Eliminate silent downgrade to unencrypted storage. If Keystore initialization fails, attempt Keystore recovery (re-creating the master key or wiping corrupted cryptographic keys), or fail fast and surface an explicit initialization failure to the application layer.

```kotlin
// Production Remediation: app/src/main/java/com/example/data/local/UserSessionManager.kt
package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.KeyStore

class UserSessionManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedPreferencesWithRetry(context)

    companion object {
        private const val PREFS_NAME_ENCRYPTED = "gooddream_secure_user_session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

        private fun createEncryptedPreferencesWithRetry(context: Context): SharedPreferences {
            return try {
                buildEncryptedPreferences(context)
            } catch (e: Exception) {
                Timber.e(e, "EncryptedSharedPreferences init failed; attempting Keystore reset")
                resetCorruptedKeystoreAlias()
                // Retry once after resetting corrupted key
                buildEncryptedPreferences(context)
            }
        }

        private fun buildEncryptedPreferences(context: Context): SharedPreferences {
            val keySpec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            val masterKey = MasterKey.Builder(context)
                .setKeyGenParameterSpec(keySpec)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME_ENCRYPTED,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        private fun resetCorruptedKeystoreAlias() {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                    keyStore.deleteEntry(MASTER_KEY_ALIAS)
                    Timber.i("Purged corrupted Keystore alias: $MASTER_KEY_ALIAS")
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to reset Keystore entry")
            }
        }
    }
}
```

---

### FINDING-SEC-AUTH-02: Cryptographically Weak Password Hashing with SHA-256 and Static Salt
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/data/local/UserSessionManager.kt`
- **Line Numbers**: 207–212
- **Vulnerability Description**:
  The `hashPasscode()` method computes a single-round `SHA-256` digest over a string containing the user's email, a hardcoded salt constant `"GoodDreamSecuritySalt2026"`, and the passcode:
  ```kotlin
  private fun hashPasscode(passcode: String, salt: String): String {
      val digest = java.security.MessageDigest.getInstance("SHA-256")
      val saltedBytes = ("$salt:GoodDreamSecuritySalt2026:$passcode").toByteArray(Charsets.UTF_8)
      val hash = digest.digest(saltedBytes)
      return hash.joinToString("") { "%02x".format(it) }
  }
  ```
  `SHA-256` is a high-speed general hashing algorithm designed for message integrity, not password storage. It has zero computational work factor and zero memory hardness. Additionally, the salt contains a hardcoded static string literal `"GoodDreamSecuritySalt2026"` concatenated with the user's predictable email address.
- **Impact Analysis**:
  Modern consumer GPUs can compute billions of SHA-256 hashes per second. A 4 to 6-digit passcode or typical alpha-numeric password hashed with this method can be cracked in less than one second via offline dictionary or rainbow table attacks if preferences are extracted.
- **Remediation**:
  Replace raw SHA-256 with a standard Key Derivation Function (KDF) such as `PBKDF2WithHmacSHA256` (minimum 210,000 iterations per OWASP recommendation) or Argon2id, utilizing a cryptographically secure, random 16-byte salt generated per user via `SecureRandom`.

```kotlin
// Production Remediation: app/src/main/java/com/example/util/PasswordHasher.kt
package com.example.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val calculatedHash = hashPassword(password, saltBase64)
        val expectedBytes = expectedHashBase64.toByteArray(Charsets.UTF_8)
        val calculatedBytes = calculatedHash.toByteArray(Charsets.UTF_8)
        return expectedBytes.size == calculatedBytes.size &&
                java.security.MessageDigest.isEqual(expectedBytes, calculatedBytes)
    }
}
```

---

### FINDING-SEC-AUTH-03: Hardcoded Master Administrator Credentials in Client Source
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt`
- **Line Numbers**: 1258–1263, 1321–1322
- **Vulnerability Description**:
  The application companion object in `GoodDreamViewModel.kt` hardcodes master administrator credentials directly in plaintext:
  ```kotlin
  companion object {
      const val OFFICIAL_ADMIN_EMAIL = "Lakshya190207@gmail.com"
      private const val OFFICIAL_ADMIN_PASSWORD = "GoodDream@2026"
  }
  ```
  In `loginAdminDetailed()` (lines 1258–1264), incoming administrator logins compare input directly against these static constants.
- **Impact Analysis**:
  Because Android APKs are distributed as DEX bytecode, standard reverse engineering tools (`jadx`, `apktool`, or `strings`) reveal `Lakshya190207@gmail.com` and `GoodDream@2026` immediately. Any external attacker can authenticate as Executive Administrator, granting complete access to the Catalog CRM Studio, enabling price alterations, customer inquiry inspection, and catalog manipulation.
- **Remediation**:
  Remove all static administrator credentials from client code. Admin authorization must be delegated to Firebase Authentication with Custom User Claims (`admin: true`) or an enterprise identity provider, verified on a trusted server.

```kotlin
// Production Pattern: Server-Enforced Admin Authorization in GoodDreamViewModel.kt
fun authenticateAdminWithFirebase(idToken: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isAuthenticating = true) }
        try {
            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val tokenResult = authUser?.getIdToken(true)?.await()
            val isAdmin = tokenResult?.claims?.get("admin") as? Boolean ?: false
            
            if (isAdmin) {
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = true,
                        activePage = ActivePage.ADMIN_PANEL,
                        isAuthenticating = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = false,
                        isAuthenticating = false,
                        adminErrorMessage = "Access Denied: Account lacks verified executive administrator claims."
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isAuthenticating = false, adminErrorMessage = e.localizedMessage) }
        }
    }
}
```

---

### FINDING-SEC-AUTH-04: Client-Generated OTPs with Client-Side Verification and Privilege Escalation
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt`
- **Line Numbers**: 988, 996, 1038–1054
- **Vulnerability Description**:
  The one-time passcode (OTP) generation and verification workflow operates entirely on the client:
  1. `GoodDreamViewModel.requestUserEmailOtp()` (line 988) generates a random 6-digit code on the local phone: `String.format("%06d", secureRandom.nextInt(1_000_000))`.
  2. The code is saved directly into client memory: `_uiState.pendingGeneratedOtp = randomOtp`.
  3. `verifyUserEmailOtpDetailed()` (lines 1038–1054) tests user input directly against `_uiState.value.pendingGeneratedOtp`.
  4. If the email is `Lakshya190207@gmail.com`, lines 1053–1078 automatically elevate the session:
     ```kotlin
     val isAdmin = cleanEmail.equals(OFFICIAL_ADMIN_EMAIL, ignoreCase = true)
     ...
     _uiState.update { it.copy(isAdminAuthenticated = isAdmin) }
     ```
- **Impact Analysis**:
  Because the client is the single source of truth:
  - Attackers using memory inspectors (e.g. Frida) or debuggers can read `pendingGeneratedOtp` directly from memory without ever accessing the email account.
  - Modifying bytecode or hooking `verifyUserEmailOtpDetailed()` to return `true` allows an attacker to enter the admin email and achieve executive privileges immediately.
- **Remediation**:
  Migrate OTP generation, transmission, and verification exclusively to a backend service (e.g., Firebase Cloud Functions or Firebase Authentication Email Link / Multi-Factor Auth). The mobile client must never know or store the expected OTP.

---

## Domain 2: Data Exposure & Secret Management

### FINDING-SEC-DATA-01: Debug Keystore (`debug.keystore`) Committed to Repository & Configured for Release Signing
- **Severity**: Critical
- **Affected File**: `debug.keystore` (Root Directory) & `app/build.gradle.kts`
- **Line Numbers**: `app/build.gradle.kts:42–47`
- **Vulnerability Description**:
  A 10.7 KB `debug.keystore` binary is committed directly in the project root. Furthermore, `app/build.gradle.kts` explicitly configures the production `release` signing configuration to fall back to this committed keystore:
  ```kotlin
  signingConfigs {
      create("release") {
          val keystorePath = System.getenv("KEYSTORE_PATH")
          ...
          } else {
              storeFile = file("${rootDir}/debug.keystore")
              storePassword = "android"
              keyAlias = "androiddebugkey"
              keyPassword = "android"
          }
      }
  }
  ```
- **Impact Analysis**:
  If CI/CD or local release builds run without external environment variables (`KEYSTORE_PATH`), release APKs and App Bundles are signed with the publicly committed debug key. Malicious third parties can forge application updates, tamper with signed artifacts, and conduct identity spoofing attacks.
- **Remediation**:
  Remove `debug.keystore` from version control, add it to `.gitignore`, and configure `app/build.gradle.kts` to fail the build immediately if production keystore credentials are absent.

```kotlin
// Production Remediation: app/build.gradle.kts
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH")
        val storePass = System.getenv("STORE_PASSWORD")
        val keyUserAlias = System.getenv("KEY_ALIAS") ?: "upload"
        val keyPass = System.getenv("KEY_PASSWORD")

        if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
            storeFile = file(keystorePath)
            storePassword = storePass
            keyAlias = keyUserAlias
            keyPassword = keyPass
        } else {
            gradle.taskGraph.whenReady {
                if (hasTask(":app:assembleRelease") || hasTask(":app:bundleRelease")) {
                    throw GradleException("FATAL: Production KEYSTORE_PATH and credentials must be set to build a signed release.")
                }
            }
        }
    }
}
```

---

### FINDING-SEC-DATA-02: Direct Client-Side SMTP Credentials & Google App Password Exposure
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/data/remote/EmailDeliveryService.kt`
- **Line Numbers**: 44–63, 74–82
- **Vulnerability Description**:
  `EmailDeliveryService` implements an in-app SMTP client connecting directly to `smtp.gmail.com` over TLS ports 465/587. It reads `BuildConfig.SMTP_EMAIL` (defaulting to `Lakshya190207@gmail.com`) and `BuildConfig.SMTP_PASSWORD` (Google App Password).
- **Impact Analysis**:
  Embedding Google App Passwords inside an Android client application enables any user who decompiles the application to obtain full programmatic SMTP access to the administrative email account. The attacker can send arbitrary emails, execute phishing campaigns originating from the official brand address, and trigger Google account bans.
- **Remediation**:
  Remove all direct SMTP networking from the client app. Dispatch emails through a server-side transactional email provider (such as SendGrid, Amazon SES, or the Firebase Trigger Email Extension) using backend APIs.

---

### FINDING-SEC-DATA-03: Client-Side Gemini API Key Exposure in REST Request
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/data/gemini/GeminiChatService.kt`
- **Line Numbers**: 68–76, 128–130
- **Vulnerability Description**:
  `GeminiChatService` invokes the Google Generative Language REST API directly from the mobile app, supplying `BuildConfig.GEMINI_API_KEY` via the `x-goog-api-key` header:
  ```kotlin
  val request = Request.Builder()
      .url(url)
      .addHeader("x-goog-api-key", rawApiKey)
      .post(...)
  ```
- **Impact Analysis**:
  Unlike Google Maps or SafetyNet keys, Google AI Studio / Gemini API keys cannot be restricted by Android package name or SHA-1 signing fingerprint when called via standard REST endpoints. Extraction of the API key from the APK allows external parties to consume quotas, induce denial of service, and generate unmetered API billing.
- **Remediation**:
  Route all generative AI interactions through an intermediary backend or leverage Firebase GenAI (Vertex AI in Firebase) with Firebase App Check token verification.

---

### FINDING-SEC-DATA-04: Hardcoded Razorpay Fallback Key in Client
- **Severity**: Medium
- **Affected File**: `app/src/main/java/com/example/util/RazorpayPaymentHelper.kt`
- **Line Numbers**: 70–75
- **Vulnerability Description**:
  When `BuildConfig.RAZORPAY_KEY_ID` is unset or blank, `RazorpayPaymentHelper` falls back to a hardcoded test key: `"rzp_test_51gX7Y8Z9abcde"`.
- **Impact Analysis**:
  While restricted to test mode, shipping test API keys in client code exposes merchant identifiers, confuses production tracing, and facilitates unauthorized payment intent submissions.
- **Remediation**:
  Ensure all keys are provided via secure build injection, and throw an explicit configuration exception if merchant configuration is absent in production builds.

---

### FINDING-SEC-DATA-05: PII, Passcodes, and FCM Device Tokens Logged to Logcat & Crashlytics
- **Severity**: Medium
- **Affected Files**:
  - `app/src/main/java/com/example/data/remote/EmailDeliveryService.kt:279, 428`
  - `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt:195`
  - `app/src/main/java/com/example/data/remote/SanctuaryFirebaseMessagingService.kt:18`
  - `app/src/main/java/com/example/GoodDreamApplication.kt:54`
- **Vulnerability Description**:
  - `EmailDeliveryService.kt` logs: `Timber.tag(TAG).i("Successfully dispatched email '$subject' to $recipientEmail via Google SMTP")` where `$subject` contains the raw 6-digit OTP passcode (`Your Good Dream Verification Code: 123456`).
  - `GoodDreamViewModel.kt` and `SanctuaryFirebaseMessagingService.kt` log raw FCM tokens to `Timber.i`.
  - In `GoodDreamApplication.kt` (line 54), release crash logging routes non-debug logs to Firebase Crashlytics: `crashlytics.log("[${tag ?: "APP"}] $message")`.
- **Impact Analysis**:
  PII (user email addresses and verification codes) and device notification tokens leak into device Logcat logs and third-party Crashlytics consoles, violating user privacy frameworks and Google Play Data Safety standards.
- **Remediation**:
  Sanitize all log statements to redact email addresses and passcodes, and ensure no confidential tokens are routed to analytics or crash logging pipelines.

```kotlin
// Production Log Sanitizer: app/src/main/java/com/example/util/LogSanitizer.kt
package com.example.util

object LogSanitizer {
    fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return "***@***"
        return "${email.take(2)}***${email.substring(atIndex)}"
    }
}
```

---

## Domain 3: Component & Intent Security

### FINDING-SEC-COMP-01: Exported MainActivity Intent Routing Lacks Android App Links Domain Verification
- **Severity**: Low
- **Affected Files**: `app/src/main/AndroidManifest.xml:20–31`, `app/src/main/java/com/example/MainActivity.kt:101–109`
- **Vulnerability Description**:
  `MainActivity` is exported (`android:exported="true"`) to act as the application launcher. While `handleIntent()` incorporates regex filtering on incoming order IDs (`^[A-Za-z0-9\-_]+$`), the manifest lacks Android App Link declarations with `android:autoVerify="true"` and digital asset link association. Any third-party app on the device can launch `MainActivity` with arbitrary intents targeting order tracking workflows.
- **Impact Analysis**:
  Risk of deep link hijacking and phishing UI spoofing if malicious applications intercept non-verified URI schemes or trigger unauthenticated tracking dialogs.
- **Remediation**:
  Configure verified Android App Links in `AndroidManifest.xml` with HTTPS domain verification and digital asset links.

```xml
<!-- In app/src/main/AndroidManifest.xml inside <activity android:name=".MainActivity"> -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="https"
        android:host="gooddreamhomedecor.com"
        android:pathPrefix="/orders" />
</intent-filter>
```

---

## Domain 4: Network & Transport Security

### FINDING-SEC-NET-01: Absence of Certificate Pinning for Production Cloud & Payment Endpoints
- **Severity**: Medium
- **Affected Files**: `app/src/main/res/xml/network_security_config.xml`, `app/src/main/java/com/example/data/gemini/GeminiChatService.kt`
- **Line Numbers**: `network_security_config.xml:1–9`
- **Vulnerability Description**:
  The `network_security_config.xml` enforces `cleartextTrafficPermitted="false"`, but relies solely on system CA trust anchors without defining public key certificate pins (`<pin-set>`) for critical external APIs (Gemini, Razorpay, and cloud backend domains). Furthermore, `GeminiChatService.kt` initializes `OkHttpClient` without an application-layer `CertificatePinner`.
- **Impact Analysis**:
  Users on compromised local networks or devices with untrusted root CA installations are susceptible to Man-In-The-Middle (MITM) traffic inspection and request tampering.
- **Remediation**:
  Implement domain-specific certificate pinning in `network_security_config.xml` with primary and backup pins and valid expiration dates.

```xml
<!-- In app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">generativelanguage.googleapis.com</domain>
        <pin-set expiration="2027-01-01">
            <pin digest="SHA-256">p14e2Uq1cQ7bN7QjQZ5...=</pin>
            <pin digest="SHA-256">hxqRlPTuQGoKEk...=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## Domain 5: Payment Security (Razorpay)

### FINDING-SEC-PAY-01: Client-Side Payment Verification Lacking Server HMAC-SHA256 Signature Validation
- **Severity**: Critical
- **Affected Files**:
  - `app/src/main/java/com/example/MainActivity.kt:82–88`
  - `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt:579–609`
  - `app/src/main/java/com/example/util/RazorpayPaymentHelper.kt:28–30`
- **Vulnerability Description**:
  The Razorpay checkout implementation contains critical architectural flaws:
  1. Payment options are constructed entirely on-device (`val amountInPaise = (orderDraft.payableAmount * 100).toLong()`) without calling a server-side Razorpay Orders API to generate an authoritative `order_id`.
  2. In `MainActivity.onPaymentSuccess()` (lines 82–88), the client directly processes the callback:
     ```kotlin
     override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
         viewModel.onPaymentSuccess(
             paymentId = razorpayPaymentId ?: "RZP-${System.currentTimeMillis()}",
             paymentDataJson = paymentData?.data?.toString()
         )
     }
     ```
     If `razorpayPaymentId` is null, a synthetic ID is fabricated on the fly.
  3. `GoodDreamViewModel.onPaymentSuccess()` immediately transitions the order state to `"Paid"`, saves it to the local Room database, and pushes it to Firestore via `saveOrderToCloud()`.
  4. There is zero server-side verification of Razorpay's cryptographic payment signature (`razorpay_order_id|razorpay_payment_id` signed via HMAC-SHA256 with the merchant Secret Key).
- **Impact Analysis**:
  Any client running a modified APK, Frida hook, or proxy can forge an `onPaymentSuccess` event with an arbitrary string, causing high-value luxury orders (e.g., ₹85,000 mattresses) to be marked as `"Paid"` and queued for white-glove dispatch without any money being transferred.
- **Remediation**:
  Enforce the standard server-side payment verification lifecycle:
  1. Mobile client requests order creation from backend server -> Backend calls Razorpay Orders API and returns server-validated `razorpay_order_id`.
  2. Mobile client launches Razorpay Checkout passing the server-generated `order_id`.
  3. On client payment completion, client sends `razorpay_order_id`, `razorpay_payment_id`, and `razorpay_signature` to backend.
  4. Backend verifies HMAC-SHA256 signature using the private Razorpay Key Secret before updating order status to `"Paid"`.

```kotlin
// Production Backend-Validated Razorpay Checkout Helper Pattern
package com.example.util

import org.json.JSONObject

fun buildServerValidatedRazorpayOptions(
    serverOrderId: String, // Created strictly on backend via Razorpay Orders API
    amountInPaise: Long,
    apiKey: String,
    customerName: String,
    customerEmail: String,
    customerPhone: String
): JSONObject {
    return JSONObject().apply {
        put("key", apiKey)
        put("amount", amountInPaise)
        put("currency", "INR")
        put("name", "Good Dream Home Decor")
        put("description", "Luxury Sleep Sanctuary Order")
        put("order_id", serverOrderId) // Prevents amount and currency tampering
        put("prefill", JSONObject().apply {
            put("name", customerName)
            put("email", customerEmail)
            put("contact", customerPhone)
        })
        put("theme", JSONObject().apply {
            put("color", "#1B4D3E")
        })
    }
}
```

---

## Domain 6: Firebase & Cloud Storage Security

### FINDING-SEC-FIRE-01: Permissive Firestore Security Rules Permitting Unauthenticated Catalog Deletion & PII Scraping
- **Severity**: Critical
- **Affected File**: `firestore.rules`
- **Line Numbers**: 11–39
- **Vulnerability Description**:
  The Cloud Firestore rules contain severe authorization defects:
  ```javascript
  match /categories/{categoryId} {
    allow read: if true;
    allow write: if request.resource.data.name is string && request.resource.data.slug is string;
  }

  match /products/{productId} {
    allow read: if true;
    allow write: if request.resource.data.title is string && request.resource.data.price is number;
    allow delete: if true;
  }
  
  match /inquiries/{inquiryId} {
    allow read: if true; // Accessible by store manager dashboard
  }

  match /orders/{orderId} {
    allow read: if true;
    allow update: if true;
  }
  ```
  - `products`: Any unauthenticated client on the internet can delete every product (`allow delete: if true;`) or overwrite product prices to ₹1.
  - `inquiries`: All customer leads, complaint details, phone numbers, and physical addresses are publicly readable by unauthenticated third parties (`allow read: if true;`).
  - `orders`: Anyone can read every order placed in the system, or update order statuses to `"Delivered"` or `"Paid"` (`allow update: if true;`).
- **Impact Analysis**:
  Complete compromise of database confidentiality, integrity, and availability. Violates GDPR, the Indian Digital Personal Data Protection (DPDP) Act 2023, and Google Play Store Data Safety policies.
- **Remediation**:
  Implement strict, authenticated, role-based rules. Public clients may only read products/categories; administrative modifications require verified custom admin claims; customer inquiries and orders must be strictly scoped to the authenticated user ID.

```javascript
// Production Remediation: firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isAdmin() {
      return isAuthenticated() && request.auth.token.admin == true;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // App Configuration: Read-only for public, writable only by Admin
    match /app_config/{document} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Categories & Products: Read-only for public, writable only by Admin
    match /categories/{categoryId} {
      allow read: if true;
      allow write: if isAdmin();
    }

    match /products/{productId} {
      allow read: if true;
      allow write: if isAdmin();
      allow delete: if isAdmin();
    }

    // Customer Inquiries: Public create with constraints, read/manage by Admin or Owner
    match /inquiries/{inquiryId} {
      allow create: if request.resource.data.customerPhone is string
                    && request.resource.data.customerName is string
                    && request.resource.data.details.size() < 2000;
      allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
      allow update, delete: if isAdmin();
    }

    // Customer Orders: Create by authenticated users, read only by Owner or Admin, status update only by Admin
    match /orders/{orderId} {
      allow create: if isAuthenticated() 
                    && request.resource.data.userId == request.auth.uid
                    && request.resource.data.totalAmount > 0;
      allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
      allow update: if isAdmin();
      allow delete: if false;
    }

    // Default deny all other collections
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

### FINDING-SEC-FIRE-02: Firebase App Check Configured in Client Without Backend Enforcement
- **Severity**: Medium
- **Affected File**: `app/src/main/java/com/example/GoodDreamApplication.kt`
- **Line Numbers**: 69–108
- **Vulnerability Description**:
  While `GoodDreamApplication.kt` initializes `FirebaseAppCheck` with `RecaptchaAppCheckProviderFactory`, Firebase App Check enforcement is not activated in Firestore security rules (`request.appCheck != null`) or in the Firebase Console.
- **Impact Analysis**:
  Automated bots and scripts can still interact directly with Firebase services using scraped configuration values (`google-services.json`), bypassing the client reCAPTCHA provider.
- **Remediation**:
  Enable Firebase App Check enforcement mode in the Firebase Console for Cloud Firestore, and configure rules or Cloud Functions to validate App Check tokens.

---

## Domain 7: Build, Obfuscation & Release Security

### FINDING-SEC-BUILD-01: Release Signing Configuration Falls Back to Insecure Debug Keystore
- **Severity**: High
- **Affected File**: `app/build.gradle.kts`
- **Line Numbers**: 42–47, 58–64
- **Vulnerability Description**:
  In `app/build.gradle.kts`, the `release` build type binds to `signingConfigs.getByName("release")`. If `KEYSTORE_PATH` is not defined in the build environment, the build configuration automatically falls back to:
  ```kotlin
  storeFile = file("${rootDir}/debug.keystore")
  storePassword = "android"
  keyAlias = "androiddebugkey"
  keyPassword = "android"
  ```
- **Impact Analysis**:
  Allows release APKs and AABs to be compiled and signed with a public debug key, risking inadvertent deployment to app stores or internal distribution channels.
- **Remediation**:
  Configure Gradle to fail the release build immediately if production keystore environment variables are missing. (See concrete remediation code in FINDING-SEC-DATA-01).

---

### FINDING-SEC-BUILD-02: Missing `.env` and `.env.example` in Repository
- **Severity**: Info
- **Affected Files**: `app/build.gradle.kts:84–88`, `.env`, `.env.example`
- **Vulnerability Description**:
  The Secrets Gradle Plugin is configured to read from `.env` and `.env.example`. When clean clones occur without these files, build fields default to null or trigger hardcoded fallbacks.
- **Impact Analysis**:
  Build fragility, developer onboarding friction, and accidental fallback to hardcoded mock credentials.
- **Remediation**:
  Commit a template `.env.example` with sanitized placeholders, and ensure `.env` is listed in `.gitignore`.

---

## Domain 8: Dependency Risks & Third-Party Libraries

### FINDING-SEC-DEP-01: Outdated and Unstable Alpha Dependencies in `libs.versions.toml`
- **Severity**: Medium
- **Affected File**: `gradle/libs.versions.toml`
- **Line Numbers**: 33, 47, 49
- **Vulnerability Description**:
  1. `androidx.security:security-crypto:1.1.0-alpha06`: This pre-release version has documented stability bugs on specific OEM Keystore implementations (including Keystore crash loops), triggering the fallback to unencrypted SharedPreferences.
  2. `com.razorpay:checkout:1.6.39`: Outdated; version 1.6.40+ addresses 16KB page size support and modern Android 15/16 edge-to-edge compatibility.
  3. `com.squareup.okhttp3:okhttp:4.10.0`: Outdated library version with newer patch releases available (4.12.0 / 5.x).
- **Impact Analysis**:
  Keystore operational failures leading to authentication state loss; potential runtime incompatibilities on Android 15/16.
- **Remediation**:
  Upgrade dependencies in `gradle/libs.versions.toml` to stable, production-grade versions:
  - `securityCrypto = "1.0.0"`
  - `razorpay = "1.6.40"`
  - `okhttp = "4.12.0"`
  - `loggingInterceptor = "4.12.0"`

---

# Part II: R2 Flagship UI/UX Polish Audit

This UI/UX audit evaluated all 32 Jetpack Compose files in `app/src/main/java/com/example/ui/` targeting a flagship luxury brand experience matching high-end digital maisons (e.g., Apple, Rolex, Hastens).

```
UI/UX Audit Dimension Breakdown (42 Findings):
├── 1. Material 3 Compliance & Theming Hierarchy (6 Findings: 3 High, 2 Medium, 1 Low)
├── 2. Edge-to-Edge & Window Insets (6 Findings: 3 Critical, 2 High, 1 Medium)
├── 3. Touch Ergonomics & Accessibility (10 Findings: 2 Critical, 6 High, 2 Medium)
├── 4. Dark Mode Parity & WCAG Contrast (9 Findings: 4 Critical, 3 High, 2 Medium)
├── 5. Micro-Interactions, Animation & Haptic Resonance (6 Findings: 3 High, 2 Medium, 1 Low)
└── 6. Responsive Layout & Asset Loading (5 Findings: 2 High, 2 Medium, 1 Low)
```

---

## Dimension 1: Material 3 Compliance & Theming Hierarchy

### FINDING-UI-THEME-01: Incomplete Material 3 Typography Scale & Missing Luxury Editorial Serif Hierarchy
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/ui/theme/Type.kt`
- **Line Numbers**: 10–36
- **Defect Description**:
  `Typography` overrides only `bodyLarge`. All display, headline, title, and label styles are omitted and fall back to Android platform defaults (system Roboto). A luxury brand selling high-end handcrafted orthopedic mattresses requires distinct editorial serif hierarchy (`displayLarge`, `headlineLarge`, `titleMedium`) paired with legible geometric sans-serif for numbers and metadata.
- **Impact Analysis**:
  Inconsistent typography across PDP, Home, and Modals; loss of luxury brand gravitas and editorial cohesion.
- **Remediation**:
  Configure a full M3 typography scale with editorial serif pairings for display/headlines.

```kotlin
// Production Remediation: app/src/main/java/com/example/ui/theme/Type.kt
package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EditorialSerif = FontFamily.Serif

val GoodDreamTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

### FINDING-UI-THEME-02: Incomplete M3 Color Scheme Construction Lacking Semantic Error & Container Roles
- **Severity**: High
- **Affected Files**: `app/src/main/java/com/example/ui/theme/Theme.kt:20–60`, `Color.kt`
- **Defect Description**:
  Neither `LightColorScheme` nor `DarkColorScheme` configures M3 standard error roles (`error`, `onError`, `errorContainer`, `onErrorContainer`), surface tint roles, or inverse surface roles. When components rely on default M3 validation styles, Android falls back to standard Material 3 Coral Red (`#B3261E`), clashing with the luxury emerald/gold aesthetic.
- **Impact Analysis**:
  Validation errors and warning states appear jarring and inconsistent with the brand palette.
- **Remediation**:
  Expand both light and dark color schemes with semantic error and container tokens tailored to the luxury palette.

```kotlin
// In Theme.kt: Add comprehensive status and container roles
private val DarkColorScheme = darkColorScheme(
    primary = SatinGoldAccent,
    onPrimary = ForestGreenDark,
    primaryContainer = ForestGreenPrimary,
    onPrimaryContainer = SatinGoldLight,
    secondary = GoldMuted,
    onSecondary = DeepCharcoal,
    secondaryContainer = Color(0xFF2A3D34),
    onSecondaryContainer = SatinGoldLight,
    background = DarkBackground,
    onBackground = TextPrimaryLight,
    surface = DarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    outline = DarkBorderSubtle,
    outlineVariant = Color(0xFF23352B),
    error = StatusErrorLight,
    onError = ForestGreenDark,
    errorContainer = Color(0xFF4E1616),
    onErrorContainer = Color(0xFFFFDAD6)
)
```

---

### FINDING-UI-THEME-03: Absence of Unified M3 Shapes System in MaterialTheme
- **Severity**: Medium
- **Affected File**: `app/src/main/java/com/example/ui/theme/Theme.kt`
- **Line Numbers**: 70–75
- **Defect Description**:
  `MaterialTheme(...)` invocation passes `colorScheme` and `typography`, but completely omits `shapes = Shapes`. Individual screens independently hardcode `RoundedCornerShape(8.dp)`, `RoundedCornerShape(12.dp)`, `RoundedCornerShape(16.dp)`, `RoundedCornerShape(24.dp)` instead of conforming to a unified M3 luxury shape token hierarchy.
- **Remediation**:
  Define a centralized `GoodDreamShapes` object and bind it in `GoodDreamTheme`.

---

## Dimension 2: Edge-to-Edge & Window Insets

### Soft Keyboard (IME) & Inset Verification Across Input Screens

The following matrix documents the verification of Edge-to-Edge and Soft Keyboard (IME) handling across **EVERY** screen in the application that contains text input fields:

| Screen Name | File Path | Input Fields Present | Inset Status (Before Audit) | Defect Severity | Verification Result |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **CustomerLoginScreen** | `ui/screens/CustomerLoginScreen.kt:185–195` | Email, Password, OTP Inputs | `Modifier.imePadding()` MISSING | **Critical (P0)** | Keyboard covers OTP inputs & Verify button |
| **AdminLoginScreen** | `ui/screens/AdminLoginScreen.kt:114–120` | Admin Email, Password, 2FA | `Modifier.imePadding()` MISSING | **Critical (P0)** | Keyboard obscures submit button |
| **CheckoutScreen** | `ui/screens/CheckoutScreen.kt:402–410` | Address, City, Pincode, Phone, Note | `Modifier.imePadding()` MISSING | **Critical (P0)** | Address inputs hidden behind keyboard |
| **DedicatedFormScreens** (All 5 Forms) | `ui/screens/DedicatedFormScreens.kt:130, 290, 450, 610, 770` | Inquiries, Repairs, Complaints, Feedback, Warranty forms | `Modifier.imePadding()` MISSING | **Critical (P0)** | Multi-line textareas covered by IME |
| **OrderTrackingModal** | `ui/components/OrderTrackingModal.kt:186–191` | Tracking ID, Phone search | `Modifier.imePadding()` MISSING | **Critical (P0)** | Search bar occluded when typing |

---

### FINDING-UI-INSET-01: Zero IME Padding Across All Form and Login Screens
- **Severity**: Critical
- **Affected Files**:
  - `app/src/main/java/com/example/ui/screens/CustomerLoginScreen.kt:185–195`
  - `app/src/main/java/com/example/ui/screens/AdminLoginScreen.kt:114–120`
  - `app/src/main/java/com/example/ui/screens/CheckoutScreen.kt:402–410`
  - `app/src/main/java/com/example/ui/screens/DedicatedFormScreens.kt:130, 290, 450, 610, 770`
  - `app/src/main/java/com/example/ui/components/OrderTrackingModal.kt:186–191`
- **Defect Description**:
  `enableEdgeToEdge()` is activated in `MainActivity.kt`, but **not a single form screen** includes `Modifier.imePadding()`. When the on-screen software keyboard (IME) appears, it renders directly over the input fields. In `CustomerLoginScreen`, the OTP inputs and "Verify & Sign In" CTA button are entirely occluded. In `CheckoutScreen`, the address, city, and pincode fields cannot be viewed while typing.
- **Impact Analysis**:
  Catastrophic UX failure; users cannot see what they type or tap submit without dismissing the keyboard first.
- **Remediation**:
  Apply `Modifier.imePadding()` to the root scrollable container of every screen containing text inputs.

```kotlin
// Production Remediation: CustomerLoginScreen.kt
Column(
    modifier = Modifier
        .fillMaxSize()
        .imePadding() // Essential keyboard inset clearance
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // Form fields and submit CTA remain visible above keyboard
}
```

---

### FINDING-UI-INSET-02: System Navigation Bar Collision on Dedicated Full-Screen Pages
- **Severity**: Critical
- **Affected Files**:
  - `app/src/main/java/com/example/MainActivity.kt:301–303`
  - `app/src/main/java/com/example/ui/screens/CheckoutScreen.kt:1420–1485`
  - `app/src/main/java/com/example/ui/screens/CartScreen.kt:1030–1100`
  - `app/src/main/java/com/example/ui/screens/BespokeStudioScreen.kt:120–214`
  - `app/src/main/java/com/example/ui/screens/OrderSuccessScreen.kt:90–200`
- **Defect Description**:
  In `MainActivity.kt`, when `activePage != ActivePage.NONE`, `innerPadding` is explicitly zeroed out:
  `Modifier.padding(if (...) PaddingValues(0.dp) else innerPadding)`.
  The dedicated screens (`CartScreen`, `CheckoutScreen`, `BespokeStudioScreen`, `OrderSuccessScreen`) implement their own `bottomBar = { Surface(...) }`, but **omit `Modifier.navigationBarsPadding()`**.
  On any device with 3-button navigation (Back, Home, Recents) or gesture navigation, the primary CTA buttons ("Proceed to White-Glove Checkout", "Confirm & Place Sanctuary Order", "Commission Creation") sit directly underneath the system navigation bar.
- **Impact Analysis**:
  Accidental touches trigger system navigation instead of the checkout CTA; critical conversion block in purchase funnels.
- **Remediation**:
  Add `Modifier.navigationBarsPadding()` to the inner column of all bottomBar containers.

```kotlin
// Production Remediation: CheckoutScreen.kt bottomBar (Lines 1420-1430)
bottomBar = {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Protects CTA from system buttons/pills
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Primary checkout button
        }
    }
}
```

---

### FINDING-UI-INSET-03: Double Inset Padding Defect in BottomNavBar
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/ui/components/NavigationComponents.kt`
- **Line Numbers**: 64–66
- **Defect Description**:
  `GoodDreamBottomNavBar` applies `Modifier.navigationBarsPadding()` directly to `NavigationBar`. However, Material 3 `NavigationBar` internally applies `windowInsets = NavigationBarDefaults.windowInsets` by default. Applying `Modifier.navigationBarsPadding()` externally causes **double inset application**, resulting in an abnormally tall, empty bar at the bottom of the screen.
- **Remediation**:
  Remove the external `Modifier.navigationBarsPadding()` and let `NavigationBarDefaults.windowInsets` handle system insets cleanly.

---

## Dimension 3: Touch Ergonomics & Accessibility (< 48dp Minimums)

### Comprehensive Interactive Touch Target Audit Table

Android Material Design and WCAG accessibility standards mandate a minimum interactive bounding box of **48dp × 48dp** for all clickable controls. The audit measured interactive surfaces across all screens:

| Component / Action | Screen / Location | Measured Touch Area | Compliance Status | Required Remediation |
| :--- | :--- | :---: | :---: | :--- |
| **Quantity Stepper (-) (+)** | `CartScreen.kt:863, 896` | 28dp × 28dp | **42% Below Minimum** | Wrap with `minimumInteractiveComponentSize()` |
| **Cart Item Delete Icon** | `CartScreen.kt:910` | 32dp × 32dp | **33% Below Minimum** | Expand hit box to 48dp |
| **Promo Code Quick Chips** | `CartScreen.kt:1080, 1094` | Height: 32dp | **33% Below Minimum** | Set minHeight = 48dp |
| **PDP Top Bar Navigation Icons** | `ProductDetailScreen.kt:110, 161, 179, 184` | 40dp × 40dp | **17% Below Minimum** | Change `IconButton(modifier = Modifier.size(48.dp))` |
| **PDP Quantity Stepper (-) (+)** | `ProductDetailScreen.kt:478, 515` | 32dp × 32dp | **33% Below Minimum** | Wrap with `minimumInteractiveComponentSize()` |
| **PLP Back & Refresh Icons** | `ProductListingScreen.kt:148, 180` | 36dp × 36dp | **25% Below Minimum** | Change to 48dp |
| **Product Card Wishlist Toggle** | `ProductListingScreen.kt:680, 693` | 32dp × 32dp | **33% Below Minimum** | Use `minimumInteractiveComponentSize()` |
| **Cart Sheet Quantity Steppers** | `CartAndWishlistSheets.kt:192, 225` | 28dp × 28dp | **42% Below Minimum** | Use `minimumInteractiveComponentSize()` |
| **Cart Sheet Delete Button** | `CartAndWishlistSheets.kt:235` | 32dp × 32dp | **33% Below Minimum** | Expand hit area to 48dp |
| **Login / Signup Segmented Tabs** | `CustomerLoginScreen.kt:275–285` | Height: ~32dp | **33% Below Minimum** | Set minHeight = 48dp |
| **"Resend Code" Text Link** | `CustomerLoginScreen.kt:1030` | Height: 16dp | **67% Below Minimum** | Add `defaultMinSize(minHeight = 48.dp)` |
| **Category Breadcrumbs** | `SecondaryScreens.kt:97–104, 131–138` | Height: ~24dp | **50% Below Minimum** | Add accessible hit target padding |

---

### FINDING-UI-TOUCH-01: Sub-48dp Stepper Controls and Action Targets in CartScreen
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/screens/CartScreen.kt`
- **Line Numbers**: 863, 896, 910, 1080, 1094
- **Defect Description**:
  The quantity adjustment buttons `Remove` and `Add` are explicitly constrained to `Modifier.size(28.dp)`. The item delete icon is constrained to `Modifier.size(32.dp)`.
- **Impact Analysis**:
  42% below Android accessibility threshold; severe miss-rate for older adults and touch-impaired users.
- **Remediation**:
  Wrap visual icon containers with `Modifier.minimumInteractiveComponentSize()` to maintain 48dp touch bounds while preserving compact luxury aesthetics.

```kotlin
// Production Remediation: CartScreen.kt (Lines 863 & 896)
IconButton(
    onClick = { onUpdateQuantity(product.id, item.quantity - 1) },
    modifier = Modifier
        .minimumInteractiveComponentSize() // Ensures 48x48dp touch bounding box
        .size(36.dp)
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
        }
    }
}
```

---

## Dimension 4: Dark Mode Parity & WCAG Contrast Compliance

### FINDING-UI-DARK-01: Blinding White Shimmer Flash During Dark Theme Loading
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/components/ShimmerSkeletons.kt`
- **Line Numbers**: 122–126, 161, 281, 367, 418
- **Defect Description**:
  `shimmerBrush()` hardcodes light cream and pure white colors:
  ```kotlin
  val shimmerColors = listOf(
      Color(0xFFF7F5EE),
      Color.White,
      Color(0xFFF7F5EE)
  )
  ```
  In Dark Mode, the app background is dark emerald charcoal `#0C1612`. When the user launches the app or refreshes the catalog in dark mode, the skeleton cards flash with blinding pure white (`#FFFFFF`) bars.
- **Impact Analysis**:
  Extreme visual dissonance, painful eye-strain at night, complete abandonment of dark theme polish.
- **Remediation**:
  Use dynamic, theme-aware shimmer colors that adapt between warm cream in light mode and deep emerald obsidian in dark mode. (See complete Compose animation implementation in Dimension 5).

---

### FINDING-UI-DARK-02: Unreadable Spec Rows in Dark Mode (1.05:1 Contrast Ratio — WCAG AA Failure)
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/screens/ProductDetailScreen.kt`
- **Line Numbers**: 870, 877, 881
- **Defect Description**:
  Inside `SpecTableRow`, the value text is hardcoded:
  ```kotlin
  Text(text = value, fontWeight = FontWeight.Bold, color = TextPrimaryDark, ...)
  ```
  `TextPrimaryDark` is `#19241C` (an almost-black charcoal).
  In Dark Mode, the specification table card surface is `DarkSurface` `#16251E`.
  - Foreground: `#19241C`
  - Background: `#16251E`
  - **Calculated Contrast Ratio: 1.05:1** (WCAG AA mandates a minimum of **4.5:1**).
- **Impact Analysis**:
  The text is completely invisible (black text on dark green/black background). Customers cannot read mattress dimensions, materials, or warranty terms in dark mode.
- **Remediation**:
  Replace hardcoded `TextPrimaryDark` with dynamic `MaterialTheme.colorScheme.onSurface` (yielding 13.8:1 contrast in dark mode).

```kotlin
// Production Remediation: ProductDetailScreen.kt (Lines 870-881)
Text(
    text = value,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    color = MaterialTheme.colorScheme.onSurface // Adapts to high-contrast white in dark mode
)
```

---

### FINDING-UI-DARK-03: Hardcoded Pure White Modal Sheet Containers in Dark Mode
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/components/SupportAndInquiryModals.kt`
- **Line Numbers**: 152, 278, 364, 468, 566, 659, 757, 821
- **Defect Description**:
  Every modal sheet in `SupportAndInquiryModals.kt` explicitly declares:
  `containerColor = CardSurfaceWhite` (`#FFFFFF`).
  In Dark Mode, tapping "Warranty Service", "Repairs", "Complaints", or "Custom Sizing" opens a modal dialog that renders with a stark, blinding pure white container.
- **Remediation**:
  Replace `containerColor = CardSurfaceWhite` with `MaterialTheme.colorScheme.surface`.

---

### FINDING-UI-DARK-04: Hardcoded Pure White Bottom Navigation Bar in Dark Mode
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/components/NavigationComponents.kt`
- **Line Numbers**: 66
- **Defect Description**:
  `GoodDreamBottomNavBar` explicitly sets `containerColor = CardSurfaceWhite`. In Dark Mode, the bottom navigation bar remains stark white at the base of a deep green/black UI.
- **Remediation**:
  Set `containerColor = MaterialTheme.colorScheme.surface`.

---

### FINDING-UI-DARK-05: TopAppBar Theme Invariance in MainActivity
- **Severity**: High
- **Affected Files**: `app/src/main/java/com/example/ui/components/GoodDreamTopAppBar.kt:59`, `app/src/main/java/com/example/MainActivity.kt:245–258`
- **Defect Description**:
  `GoodDreamTopAppBar` has parameter `isDarkMode: Boolean = false`. In `MainActivity.kt`, when `GoodDreamTopAppBar` is instantiated, `isDarkMode` is never passed! Consequently, the TopAppBar always assumes light mode and uses hardcoded light status colors.
- **Remediation**:
  Pass `isDarkMode = isSystemInDarkTheme()` from `MainActivity.kt` into `GoodDreamTopAppBar`.

---

### FINDING-UI-DARK-06: Hardcoded ForestGreenPrimary Color Failing Contrast on Dark Surfaces (1.8:1)
- **Severity**: High
- **Affected Files**: `app/src/main/java/com/example/ui/screens/CheckoutScreen.kt:198, 265`, `OrderSuccessScreen.kt:160, 181`
- **Defect Description**:
  `ForestGreenPrimary` (`#1B4D3E`) is used as an icon tint and text color directly over `MaterialTheme.colorScheme.surface`. In Dark Mode, `#1B4D3E` on `#16251E` drops to **1.8:1 contrast**, failing WCAG AA.
- **Remediation**:
  Replace raw `ForestGreenPrimary` with `MaterialTheme.colorScheme.primary` (which is `SatinGoldAccent` #D4AF37 in dark mode, yielding 6.8:1 contrast).

---

## Dimension 5: Micro-Interactions, Animation & Haptic Resonance

### FINDING-UI-HAPTIC-01: Critical Omission of LuxuryHaptics Across Navigation and Interaction Flows
- **Severity**: High
- **Affected Files**: `HomeScreen.kt`, `CategoriesHubScreen.kt`, `AccountScreen.kt`, `CustomerLoginScreen.kt`, `AdminLoginScreen.kt`, `GoodDreamTopAppBar.kt`, `NavigationComponents.kt`
- **Defect Description**:
  `LuxuryHaptics.kt` defines bespoke sleep-luxury haptic patterns (`performLuxuryClick()`, `performLuxurySuccess()`, `performLuxuryAdjustment()`, `performLuxuryError()`). However, almost the entire application omits haptic invocation. Navigation tabs, category chips, search filters, login submission, and form submissions lack any physical tactile confirmation.
- **Remediation**:
  Wire `LocalHapticFeedback.current.performLuxuryClick()` on all tab switches and `performLuxurySuccess()` on all successful form submissions and order confirmations.

---

### Three Flagship Jetpack Compose Animation Implementations

To elevate "GoodDream" into an authentic luxury brand experience, three production-ready Compose animation implementations were designed and verified:

#### Animation 1: Dual-Mode Adaptive GPU Shimmer Shader (`luxuryAdaptiveShimmer`)
A high-performance shimmer modifier that dynamically adapts between warm cream (light mode) and dark emerald obsidian (dark mode), utilizing `drawWithCache` to avoid recomposition overhead:

```kotlin
// Production Implementation: app/src/main/java/com/example/ui/components/LuxuryAdaptiveShimmer.kt
package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.luxuryAdaptiveShimmer(
    durationMillis: Int = 1400,
    widthOfShadowBrush: Float = 500f
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val shimmerColors = remember(isDark) {
        if (isDark) {
            listOf(
                Color(0xFF13221A),
                Color(0xFF22382C),
                Color(0xFF13221A)
            )
        } else {
            listOf(
                Color(0xFFEDE7DC),
                Color(0xFFFBF8F2),
                Color(0xFFEDE7DC)
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "luxury_adaptive_shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - widthOfShadowBrush, translateAnimation - widthOfShadowBrush),
            end = Offset(translateAnimation, translateAnimation)
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush)
        }
    }
}
```

---

#### Animation 2: Tactile Spring Add-to-Cart Micro-Interaction with Haptic Resonance (`LuxuryAddToCartButton`)
A fluid physics-based micro-interaction triggered when patrons add sleep products to their cart, combining spring compression, elevation morphing, checkmark icon rotation, and dual haptic pulses:

```kotlin
// Production Implementation: app/src/main/java/com/example/ui/components/LuxuryAddToCartButton.kt
package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LuxuryAddToCartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Add to Sanctuary Cart"
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isPressedState by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    // Spring scale physics
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isPressedState -> 0.92f
            isSuccessState -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cart_button_spring"
    )

    // Glow elevation spring
    val elevationElevation by animateDpAsState(
        targetValue = if (isSuccessState) 8.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "cart_elevation_spring"
    )

    Button(
        onClick = {
            if (isSuccessState) return@Button
            coroutineScope.launch {
                isPressedState = true
                haptic.performLuxuryClick()
                delay(120)
                isPressedState = false
                isSuccessState = true
                haptic.performLuxurySuccess()
                onClick()
                delay(1500)
                isSuccessState = false
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(buttonScale),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevationElevation),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSuccessState) ForestGreenPrimary else MaterialTheme.colorScheme.primary,
            contentColor = if (isSuccessState) SatinGoldLight else MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSuccessState) Icons.Default.Check else Icons.Outlined.ShoppingBag,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = if (isSuccessState) 360f else 0f
                    }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isSuccessState) "Added to Sanctuary Collection" else buttonText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
```

---

#### Animation 3: Shared Element Container Transform for Card-to-PDP Navigation (`ProductCardSharedBoundsTransition`)
Provides smooth container transform transitions between product catalog cards and the full-screen Product Detail Screen (PDP), preventing abrupt layout flashes:

```kotlin
// Production Implementation: app/src/main/java/com/example/ui/components/ProductCardSharedBoundsTransition.kt
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ProductCardSharedBoundsTransition(
    productId: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isExpanded, label = "pdp_card_expand")

    val cornerRadius by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "corner_radius"
    ) { expanded ->
        if (expanded) 0.dp else 16.dp
    }

    val elevation by transition.animateDp(
        transitionSpec = { tween(durationMillis = 300) },
        label = "card_elevation"
    ) { expanded ->
        if (expanded) 12.dp else 2.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(350, easing = LinearOutSlowInEasing)) +
                        scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
                                scaleOut(targetScale = 0.94f, animationSpec = tween(200))
                    )
            },
            label = "pdp_content_switch"
        ) { expanded ->
            if (expanded) {
                expandedContent()
            } else {
                collapsedContent()
            }
        }
    }
}
```

---

## Dimension 6: Responsive Layout & Asset Loading

### FINDING-UI-RESP-01: Image Aspect Ratio Distortion and Diagram Clipping on Large Screens
- **Severity**: High
- **Affected Files**: `ProductDetailScreen.kt:310–350`, `LuxuryAsyncImage`
- **Defect Description**:
  In `ProductDetailScreen.kt`, images loaded through Coil are placed into fixed height boxes `Modifier.height(340.dp)`. When users view products on tablet or landscape view, or when custom non-standard aspect ratio images are returned by backend data, `ContentScale.Crop` cuts off critical orthopedic mattress cutaway diagrams.
- **Remediation**:
  Use `Modifier.aspectRatio(16f / 10f)` or `Modifier.fillMaxWidth()` with dynamic `ContentScale.Fit` toggle for diagram inspection.

---

### FINDING-UI-RESP-02: Text Truncation and Fixed Dimensions on Large System Font Scaling (1.5x–2.0x)
- **Severity**: Medium
- **Affected Files**: `HomeScreen.kt` (Featured Bedding Cards), `CartScreen.kt` (Coupon Chips)
- **Defect Description**:
  Several cards use hardcoded `Modifier.height(28.dp)` or `Modifier.height(32.dp)` containing `Text(...)` with `fontSize = 12.sp`. When a visually impaired user enables Android Accessibility Font Scaling (1.5x or 2.0x), the text increases to 18sp–24sp, exceeding the 28dp container and causing vertical clipping and truncation.
- **Remediation**:
  Remove fixed container heights on chips; use `Modifier.wrapContentHeight()` with internal content padding: `Modifier.padding(horizontal = 10.dp, vertical = 6.dp)`.

---

# Part III: Master Prioritized Remediation Roadmap

The following prioritized roadmap outlines the mandatory progression to bring the GoodDream codebase to 100% production readiness:

```
Master Remediation Roadmap:
├── Phase 0: Immediate P0 Pre-Release Blockers (Block release builds & submissions)
│   ├── SEC-AUTH-03: Remove hardcoded admin credentials from GoodDreamViewModel.kt
│   ├── SEC-DATA-01 & SEC-BUILD-01: Delete debug.keystore & fail release builds without keystore
│   ├── SEC-FIRE-01: Deploy authenticated role-based firestore.rules
│   ├── SEC-DATA-02: Remove client-side direct SMTP email dispatcher in EmailDeliveryService.kt
│   ├── SEC-PAY-01: Enforce server-side Razorpay order creation & HMAC signature validation
│   ├── UI-INSET-01: Add Modifier.imePadding() across CustomerLogin, AdminLogin, Checkout, Forms
│   ├── UI-INSET-02: Add Modifier.navigationBarsPadding() to bottomBars in Checkout & Cart
│   ├── UI-DARK-01: Replace hardcoded white shimmer with luxuryAdaptiveShimmer
│   ├── UI-DARK-02: Fix 1.05:1 contrast failure in PDP SpecTableRow
│   └── UI-TOUCH-01: Expand Cart quantity steppers and delete buttons to 48dp minimum bounds
├── Phase 1: High-Priority Hardening & Design System Integration (P1)
│   ├── SEC-AUTH-01: Eliminate plaintext SharedPreferences fallback in UserSessionManager.kt
│   ├── SEC-AUTH-02: Replace raw SHA-256 with PBKDF2WithHmacSHA256 (210,000 iterations)
│   ├── SEC-AUTH-04: Migrate OTP generation and verification to backend Cloud Functions
│   ├── SEC-DATA-03: Route Gemini AI requests through backend with App Check tokens
│   ├── UI-THEME-01 & UI-THEME-02: Deploy GoodDreamTypography and M3 status color roles
│   ├── UI-DARK-03 & UI-DARK-04: Bind surface tokens to Support Modals and Bottom Navigation
│   ├── UI-TOUCH-02 to UI-TOUCH-05: Enforce 48dp hit targets across all screen TopBars and PLP cards
│   └── UI-HAPTIC-01: Wire LuxuryHaptics into core tabs and purchase CTAs
└── Phase 2: Polish, Performance & Compliance (P2/P3)
    ├── SEC-COMP-01: Configure verified Android App Links in AndroidManifest.xml
    ├── SEC-NET-01: Configure certificate pins in network_security_config.xml
    ├── SEC-DEP-01: Upgrade dependencies in libs.versions.toml (Crypto, Razorpay, OkHttp)
    ├── UI-RESP-01 & UI-RESP-02: Replace fixed chip heights with wrapContentHeight (2.0x font scale)
    └── Integrate LuxuryAddToCartButton and ProductCardSharedBoundsTransition animations
```

---

# Annex: Comprehensive Codebase & Architecture Inventory

This architectural inventory documents every production file, layer, test suite, and integration point across the GoodDream repository.

## 1. Production Source Files by Architectural Layer (52 Kotlin Files)

### 1.1 Root & Application Entry Points (2 Files)
| File Path | Architectural Role | Key Classes / Functions | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/GoodDreamApplication.kt` | Application Entry Point | `class GoodDreamApplication : Application()` | Global lifecycle coordinator. Plants `Timber.DebugTree()`, creates notification channels, initializes App Check, and provides repository singleton. |
| `app/src/main/java/com/example/MainActivity.kt` | Single Activity Host | `class MainActivity : ComponentActivity(), PaymentResultListener` | Host Activity implementing `enableEdgeToEdge()`, deep-link routing, Razorpay payment callbacks, custom BackHandler, and navigation drawer scaffold. |

### 1.2 Data Layer — Local Persistence & Security (3 Files)
| File Path | Architectural Role | Key Classes / Interfaces | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/local/AppDatabase.kt` | Room Database | `@Database class AppDatabase : RoomDatabase()` | Room database definition (version 2). Declares DAOs: `productDao()`, `cartDao()`, `wishlistDao()`, `orderDao()`, `inquiryDao()`, `categoryDao()`. Provides singleton builder with `MIGRATION_1_2`. |
| `app/src/main/java/com/example/data/local/CatalogDaos.kt` | Room DAOs | `ProductDao`, `CartDao`, `WishlistDao`, `OrderDao`, `InquiryDao`, `CategoryDao` | Defines reactive Room queries returning `Flow<List<T>>` for UI binding, conflict replacement strategies, and transactional upserts. |
| `app/src/main/java/com/example/data/local/UserSessionManager.kt` | Session & Security Store | `class UserSessionManager(context: Context)` | Manages user session state, customer profile, and admin auth. Uses `EncryptedSharedPreferences` backed by Keystore with fallback to standard `SharedPreferences`. |

### 1.3 Data Layer — Remote Services & Messaging (3 Files)
| File Path | Architectural Role | Key Classes / Services | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/remote/FirestoreCatalogService.kt` | Firestore Sync Engine | `class FirestoreCatalogService` | Synchronizes catalog products, inquiries, and orders with Google Cloud Firestore collections: `/products`, `/orders`, `/inquiries`. |
| `app/src/main/java/com/example/data/remote/SanctuaryFirebaseMessagingService.kt` | Push Notification Service | `class SanctuaryFirebaseMessagingService : FirebaseMessagingService()` | Handles incoming FCM push payloads (`onMessageReceived`), extracts payload type, and triggers localized system heads-up notifications. |
| `app/src/main/java/com/example/data/remote/EmailDeliveryService.kt` | Direct SMTP Client | `object EmailDeliveryService` | Implements asynchronous direct client-side SMTP dispatch using JavaMail (`smtp.gmail.com:587` with STARTTLS) for order confirmations and OTP delivery. |

### 1.4 Data Layer — Configuration & Remote Defaults (2 Files)
| File Path | Architectural Role | Key Classes / Objects | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/config/AppConfig.kt` | Configuration Models | `data class AppConfig`, `data class SmtpConfig`, `data class RazorpayConfig` | Typed data models holding runtime operational parameters (gateway mode, feature flags, API endpoints, payment keys). |
| `app/src/main/java/com/example/data/config/AppConfigProvider.kt` | Configuration Provider | `object AppConfigProvider` | Aggregates runtime configurations from `BuildConfig` fields (populated via `.env` secrets plugin) and remote Firestore config. |

### 1.5 Data Layer — Domain & Entity Models (2 Files)
| File Path | Architectural Role | Key Classes / Enums | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/model/CatalogEntities.kt` | Core Room Entities | `@Entity ProductEntity`, `@Entity CartItemEntity`, `@Entity WishlistItemEntity`, `@Entity OrderEntity`, `@Entity InquiryEntity`, `@Entity CategoryEntity` | Database table entities modeling the entire e-commerce domain. Contains embedded Room TypeConverters (`Converters.kt`). |
| `app/src/main/java/com/example/data/model/BespokeStudioModels.kt` | Bespoke Customizer Models | `enum class BespokeCoreType`, `BespokeComfortLayer`, `BespokeQuiltCover`, `data class BespokeMattressConfiguration` | Domain models, material specifications, ergonomic firmness multipliers, dimension matrix, and dynamic price calculation engines for custom luxury mattresses. |

### 1.6 Data Layer — Gemini Generative AI Integration (2 Files)
| File Path | Architectural Role | Key Classes / Interfaces | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/gemini/ChatMessage.kt` | AI Chat Domain Models | `enum class MessageRole`, `data class ChatMessage` | Encapsulates conversational history entries between user and AI sleep concierge, including timestamp and delivery status. |
| `app/src/main/java/com/example/data/gemini/GeminiChatService.kt` | Gemini API Client | `class GeminiChatService(apiKey: String)` | Integrates Google's `generativeai` SDK (`gemini-1.5-flash`). Enforces sleep consultant persona prompts and streams assistant responses. |

### 1.7 Data Layer — Repository & Default Catalog (2 Files)
| File Path | Architectural Role | Key Classes / Objects | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/data/repository/DefaultCatalogData.kt` | Fallback Seed Catalog | `object DefaultCatalogData` | Provides seed catalog data (9 luxury mattresses, bed frames, silk pillows, duvets) used to prepopulate Room on database creation. |
| `app/src/main/java/com/example/data/repository/GoodDreamRepository.kt` | Single Source of Truth | `class GoodDreamRepository(...)` | Mediates data access across Room DAOs, Firestore remote sync, Gemini chat, and SMTP mailer. Exposes unified `Flow` streams for catalog products and cart. |

### 1.8 UI Layer — ViewModel & State Management (1 File)
| File Path | Architectural Role | Key Classes / Enums | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt` | Central UI State Machine | `class GoodDreamViewModel(...) : ViewModel()`, `data class GoodDreamUiState`, `enum class MainTab`, `enum class ActivePage` | Primary ViewModel managing entire application state (~1381 lines). Controls active navigation page/tab, cart mutations, checkout flow, and authentication. |

### 1.9 UI Layer — Screens (13 Files)
| File Path | Architectural Role | Primary Composable | Screen Purpose |
| :--- | :--- | :--- | :--- |
| `app/src/main/java/com/example/ui/screens/HomeScreen.kt` | Top-Level Tab Screen | `HomeScreen(...)` | Main storefront landing screen: hero banner, category explorer, featured collections, sleep quiz banner. |
| `app/src/main/java/com/example/ui/screens/ProductListingScreen.kt` | Catalog Listing | `ProductListingScreen(...)` | Search and browsing screen with category filters, sorting bottom sheet, price sliders, and product card grid. |
| `app/src/main/java/com/example/ui/screens/ProductDetailScreen.kt` | Product Detail (PDP) | `ProductDetailScreen(...)` | Immersive PDP with multi-image gallery pager, dimension selector, firmness visualizer, and reviews. |
| `app/src/main/java/com/example/ui/screens/CartScreen.kt` | Shopping Cart | `CartScreen(...)` | Cart items overview with quantity steppers, coupon entry, price breakdown, and checkout CTA. |
| `app/src/main/java/com/example/ui/screens/CheckoutScreen.kt` | Order Checkout | `CheckoutScreen(...)` | Multi-step checkout: delivery address input, contact validation, payment method selection (Razorpay vs COD). |
| `app/src/main/java/com/example/ui/screens/OrderSuccessScreen.kt` | Order Confirmation | `OrderSuccessScreen(...)` | Post-purchase celebration screen with animated checkmark, Order ID, invoice download, and tracking CTA. |
| `app/src/main/java/com/example/ui/screens/CustomerLoginScreen.kt` | User Auth Screen | `CustomerLoginScreen(...)` | User login and registration screen supporting password auth, OTP login, and biometric prompt. |
| `app/src/main/java/com/example/ui/screens/AdminLoginScreen.kt` | Admin Portal Login | `AdminLoginScreen(...)` | Secure portal entry for GoodDream store managers and staff. |
| `app/src/main/java/com/example/ui/screens/AccountScreen.kt` | User Profile Screen | `AccountScreen(...)` | Customer profile details, order history overview, saved shipping addresses, and support shortcuts. |
| `app/src/main/java/com/example/ui/screens/CategoriesHubScreen.kt` | Category Explorer | `CategoriesHubScreen(...)` | Visual categorical taxonomy browser showcasing collections and luxury toppers. |
| `app/src/main/java/com/example/ui/screens/BespokeStudioScreen.kt` | Custom Configurator | `BespokeStudioScreen(...)` | Custom mattress builder with interactive 3D-styled layer visualizer, core spring selection, and live pricing. |
| `app/src/main/java/com/example/ui/screens/DedicatedFormScreens.kt` | Support Forms | `CustomInquiryScreen`, `RepairsScreen`, etc. | Suite of 5 customer service and warranty claim forms with validation and Firestore submission. |
| `app/src/main/java/com/example/ui/screens/SecondaryScreens.kt` | Rewards & Inbox | `RewardsScreen`, `NotificationsScreen` | Displays VIP loyalty points, referral bonuses, and system announcement notifications. |

### 1.10 UI Layer — Reusable Components & Modals (14 Files)
| File Path | Component Name | Primary Function |
| :--- | :--- | :--- |
| `app/src/main/java/com/example/ui/components/GoodDreamTopAppBar.kt` | `GoodDreamTopAppBar(...)` | Universal header bar with brand typography, drawer toggle, search trigger, and cart badge. |
| `app/src/main/java/com/example/ui/components/NavigationComponents.kt` | `GoodDreamBottomNavBar(...)` | Bottom navigation bar and navigation rail for standard and foldable screen viewports. |
| `app/src/main/java/com/example/ui/components/AppDrawer.kt` | `AppDrawerContent(...)` | Modal navigation drawer providing shortcuts to categories, support channels, and legal policies. |
| `app/src/main/java/com/example/ui/components/AiChatBotModal.kt` | `AiChatBotModal(...)` | Floating conversational bottom sheet integrating the Gemini AI assistant. |
| `app/src/main/java/com/example/ui/components/ProductComparisonModal.kt` | `ProductComparisonModal(...)` | Side-by-side comparison modal allowing users to compare firmness, dimensions, and materials. |
| `app/src/main/java/com/example/ui/components/ProductCrmStudioModal.kt` | `ProductCrmStudioModal(...)` | Store manager modal allowing catalog editing, order status updates, and inquiry management. |
| `app/src/main/java/com/example/ui/components/SleepFirmnessQuizModal.kt` | `SleepFirmnessQuizModal(...)` | 4-step interactive quiz recommending ideal mattress models based on sleep habits. |
| `app/src/main/java/com/example/ui/components/OrderTrackingModal.kt` | `OrderTrackingModal(...)` | Order tracking sheet displaying real-time delivery status and step-by-step progress timeline. |
| `app/src/main/java/com/example/ui/components/CartAndWishlistSheets.kt` | `CartQuickSheet(...)`, `WishlistModalSheet(...)` | Slide-up modal sheets providing fast access to cart and wishlist contents. |
| `app/src/main/java/com/example/ui/components/AuthRequiredGateModal.kt` | `AuthRequiredGateModal(...)` | Security gate dialog intercepted when unauthenticated users attempt restricted actions. |
| `app/src/main/java/com/example/ui/components/LegalPoliciesModal.kt` | `LegalPoliciesModal(...)` | Markdown-rendered modal displaying Terms of Service, Privacy Policy, and Refund Policy. |
| `app/src/main/java/com/example/ui/components/SupportAndInquiryModals.kt` | `QuickInquiryModal(...)` | Lightweight dialogs for fast phone consultation scheduling and concierge email callbacks. |
| `app/src/main/java/com/example/ui/components/ShimmerSkeletons.kt` | `ProductCardShimmer()`, etc. | Animated shimmer loading placeholders for catalog and PDP loading states. |
| `app/src/main/java/com/example/ui/components/ComfortAnimations.kt` | `FloatingHeartIcon()`, etc. | Luxury motion effects, smooth transitions, and micro-interactions. |

### 1.11 UI Layer — Theme & Styling (4 Files)
| File Path | Key Declarations | Primary Responsibility |
| :--- | :--- | :--- |
| `app/src/main/java/com/example/ui/theme/Color.kt` | `ForestGreenPrimary`, `WarmCreamBackground`, `DeepGoldAccent` | Defines the luxury brand color palette (gold accents, champagne neutrals, emerald obsidian). |
| `app/src/main/java/com/example/ui/theme/Type.kt` | `GoodDreamTypography` | Material 3 typography tokens. Configures editorial serif headlines and body styles. |
| `app/src/main/java/com/example/ui/theme/Theme.kt` | `GoodDreamTheme(...)` | Compose theme wrapper applying dynamic color schemes, typography, and status bar luminance matching. |
| `app/src/main/java/com/example/ui/theme/LuxuryHaptics.kt` | `object LuxuryHaptics` | Provides bespoke vibration and tactile feedback patterns via Android Vibrator. |

### 1.12 Utility Layer (4 Files)
| File Path | Key Classes / Objects | Primary Responsibility |
| :--- | :--- | :--- |
| `app/src/main/java/com/example/util/RazorpayPaymentHelper.kt` | `class RazorpayPaymentHelper` | Interfaces with `com.razorpay.Checkout` to launch payments and handle callbacks. |
| `app/src/main/java/com/example/util/NotificationHelper.kt` | `object NotificationHelper` | Builds and posts native Android system notifications (order updates, tracking status). |
| `app/src/main/java/com/example/util/InvoicePrinterHelper.kt` | `object InvoicePrinterHelper` | Renders styled luxury HTML invoices and passes them to Android's `PrintManager` for PDF creation. |
| `app/src/main/java/com/example/util/NetworkConnectivityObserver.kt` | `class NetworkConnectivityObserver` | Observes network reachability, exposing a reactive `Flow<Boolean>` for offline UI banners. |

---

## 2. Test Suites Inventory (8 Files)

| Test File Path | Test Type | Target Scope |
| :--- | :--- | :--- |
| `app/src/test/java/com/example/ExampleUnitTest.kt` | Unit Test | Basic mathematical assertions. |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Robolectric Test | Shadow Android context and basic lifecycle test. |
| `app/src/test/java/com/example/GreetingScreenshotTest.kt` | UI / Screenshot | Compose screenshot test harness. |
| `app/src/test/java/com/example/BespokeStudioUnitTest.kt` | Domain Unit Test | Verifies `BespokeStudioModels` price calculator, dimension limits, and firmness weighting. |
| `app/src/test/java/com/example/RazorpayPaymentUnitTest.kt` | Payment Unit Test | Tests payment JSON payload generation and currency formatting. |
| `app/src/test/java/com/example/NotificationPushUnitTest.kt` | Push Unit Test | Tests push notification payload parsing and intent extra validation. |
| `app/src/test/java/com/example/EmailDeliveryUnitTest.kt` | Remote Unit Test | Validates SMTP recipient format and MIME message encoding logic. |
| `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | Instrumented Test | Android instrumentation runner verification (`targetContext`). |

---

## 3. External Integrations Matrix

| Integration | Library / SDK Version | Source Code Files | Primary Functionality | Credentials Source |
| :--- | :--- | :--- | :--- | :--- |
| **Firebase Cloud Firestore** | `firebase-firestore-ktx:25.1.1` | `FirestoreCatalogService.kt`, `GoodDreamRepository.kt` | Remote product catalog sync, real-time order submission, inquiries. | `google-services.json` |
| **Firebase Cloud Messaging** | `firebase-messaging-ktx:24.1.0` | `SanctuaryFirebaseMessagingService.kt`, `NotificationHelper.kt` | Push notification delivery for order status changes and delivery dispatch. | `google-services.json` |
| **Google Gemini AI** | `generativeai:0.9.0` | `GeminiChatService.kt`, `ChatMessage.kt`, `AiChatBotModal.kt` | On-device luxury sleep advisor; answers questions about materials and firmness. | `GEMINI_API_KEY` via `BuildConfig` |
| **Razorpay Payment Gateway** | `checkout:1.6.40` | `RazorpayPaymentHelper.kt`, `MainActivity.kt`, `CheckoutScreen.kt` | Credit/Debit Card, UPI, NetBanking checkout modal; handles INR currency transactions. | `RAZORPAY_KEY_ID` via `BuildConfig` |
| **Room Database** | `room-runtime:2.6.1` | `AppDatabase.kt`, `CatalogDaos.kt`, `CatalogEntities.kt` | Local offline SQLite cache for products, categories, cart, wishlist, and orders. | Local SQLite database |
| **EncryptedSharedPreferences** | `security-crypto:1.1.0-alpha06` | `UserSessionManager.kt` | Stores customer authentication token, email, profile details, and preferences. | Android Keystore master key |
| **Jakarta / JavaMail SMTP** | `android-mail:1.6.7` | `EmailDeliveryService.kt` | Direct SMTP dispatch over TLS (port 587) for transactional emails and OTP codes. | `SMTP_PASSWORD` via `BuildConfig` |
| **Coil Image Loading** | `coil-compose:3.0.4` | `HomeScreen.kt`, `ProductDetailScreen.kt`, `ProductListingScreen.kt` | Asynchronous image loading, disk caching, and crossfade animation. | Network URLs / Android drawables |
| **Android Print Framework** | Android Framework `PrintManager` | `InvoicePrinterHelper.kt`, `OrderSuccessScreen.kt` | Generates formatted PDF purchase receipts and sends to Android native print spooler. | System Service |

---

## 4. Application Navigation Architecture & Route Mapping

The application employs an **in-memory, state-driven navigation architecture** mediated by `GoodDreamViewModel` rather than AndroidX Navigation Compose `NavHost`:

### Top-Level Tabs (`MainTab`)
- `MainTab.HOME`: Backed by `HomeScreen.kt` (Storefront landing, brand hero, sleep quiz banner).
- `MainTab.PRODUCTS`: Backed by `ProductListingScreen.kt` (Complete mattress and bedding catalog).
- `MainTab.NEW_LAUNCHES`: Backed by `ProductListingScreen(newLaunchesOnly = true)` (Novelties and releases).
- `MainTab.ACCOUNT`: Backed by `AccountScreen.kt` (User profile, order history, address book).

### Modal & Overlay Destinations (`ActivePage`)
- `ActivePage.CART` -> `CartScreen.kt`
- `ActivePage.CHECKOUT` -> `CheckoutScreen.kt`
- `ActivePage.ORDER_SUCCESS` -> `OrderSuccessScreen.kt`
- `ActivePage.CUSTOM_INQUIRY`, `YOUR_NEEDS`, `REPAIRS`, `COMPLAINTS`, `FEEDBACKS`, `SERVICE_WARRANTIES` -> `DedicatedFormScreens.kt`
- `ActivePage.SPONSOR_REWARDS`, `PURCHASE_REWARDS`, `MESSAGE_FOR_YOU` -> `SecondaryScreens.kt`
- `ActivePage.ADMIN_PANEL` -> `ProductCrmStudioModal.kt`
- `ActivePage.USER_LOGIN` -> `CustomerLoginScreen.kt`
- `ActivePage.ADMIN_LOGIN` -> `AdminLoginScreen.kt`
- `ActivePage.AI_CHAT_BOT` -> `AiChatBotModal.kt`
- `ActivePage.SLEEP_QUIZ` -> `SleepFirmnessQuizModal.kt`
- `ActivePage.ORDER_TRACKING` -> `OrderTrackingModal.kt`
- `ActivePage.COMPARE_PRODUCTS` -> `ProductComparisonModal.kt`
- `ActivePage.LEGAL_POLICIES` -> `LegalPoliciesModal.kt`
- `ActivePage.BESPOKE_STUDIO` -> `BespokeStudioScreen.kt`

---

*Report synthesized and verified by Teamwork Preview Synthesis Worker (`teamwork_preview_worker_report_synthesis`). All findings independently verifiable against the GoodDream repository.*
