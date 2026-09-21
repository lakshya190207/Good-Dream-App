# GoodDream Android App — Full-Depth Security Vulnerability Audit Report

## Executive Summary
This production-readiness security audit evaluated the GoodDream luxury e-commerce Android application across all 50 Kotlin source files, build systems, cloud infrastructure rules, and Android platform configurations. The audit discovered critical vulnerabilities that compromise customer data confidentiality, financial transaction integrity, and administrative control. Most notably: administrative credentials and plain Google App Passwords are hardcoded in client source code; Firestore security rules permit unauthenticated public modification, deletion, and PII exfiltration across all collections; payment transactions are validated solely on the client with fabricated transaction IDs permitted; and local user authentication employs insecure SHA-256 hashing alongside an EncryptedSharedPreferences silent fallback to unencrypted XML. Immediate remediation of the 6 Critical and 5 High-severity issues is required before commercial distribution or Google Play Store submission.

---

## Findings Summary Matrix

| Severity | Count | Domain Breakdown |
| :--- | :---: | :--- |
| **Critical** | 6 | Cryptography & Auth (2), Data Exposure (1), Payment Security (1), Firebase Security (1), Build & Release (1) |
| **High** | 5 | Cryptography & Auth (2), Data Exposure (2), Build & Release (1) |
| **Medium** | 4 | Data Exposure (1), Network & Transport (1), Firebase Security (1), Dependencies (1) |
| **Low / Info** | 2 | Component Security (1), Build & Configuration (1) |
| **Total** | **17** | Full-Depth Codebase & Infrastructure Coverage |

---

## Domain 1: Cryptography & Authentication

### FINDING-AUTH-01: Plaintext SharedPreferences Silent Fallback Exposing Sensitive Credentials
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/data/local/UserSessionManager.kt`
- **Line Numbers**: 15–30
- **Vulnerability Description**:
  The `UserSessionManager` attempts to initialize an `EncryptedSharedPreferences` instance backed by the Android Keystore. When any exception is encountered (such as during early boot, keystore corruption, or hardware abstraction layer inconsistencies), the `catch` block silently falls back to standard, unencrypted `SharedPreferences` (`PREFS_NAME_FALLBACK = "gooddream_user_session"`).
  ```kotlin
  } catch (e: Exception) {
      Timber.w(e, "Falling back to standard SharedPreferences due to Keystore initialization issue")
      context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
  }
  ```
- **Impact Analysis**:
  All user credentials, session tokens, hashed passcodes, full saved physical addresses, customer phone numbers, and FCM notification tokens are stored in cleartext XML at `/data/data/com.aistudio.gooddream.kxmpzq/shared_prefs/gooddream_user_session.xml`. On rooted devices, via Android backup extraction, or through local debugging, this data is readily extractable, violating Android Production Security Rules and Google Play Data Safety requirements.
- **Remediation**:
  Eliminate silent downgrade to unencrypted storage. If Keystore initialization fails, attempt Keystore recovery (re-creating the master key or wiping corrupted cryptographic keys), or fail fast and surface an explicit initialization failure to the application layer.

```kotlin
// Production-Ready Remediation: SecurePreferencesManager.kt
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

### FINDING-AUTH-02: Cryptographically Weak Password Hashing with SHA-256 and Static Salt
- **Severity**: High
- **Affected File**: `app/src/main/java/com/example/data/local/UserSessionManager.kt`
- **Line Numbers**: 207–212
- **Vulnerability Description**:
  The `hashPasscode()` method computes a raw `SHA-256` digest over a string containing the user's email, a hardcoded salt constant `"GoodDreamSecuritySalt2026"`, and the passcode:
  ```kotlin
  private fun hashPasscode(passcode: String, salt: String): String {
      val digest = java.security.MessageDigest.getInstance("SHA-256")
      val saltedBytes = ("$salt:GoodDreamSecuritySalt2026:$passcode").toByteArray(Charsets.UTF_8)
      val hash = digest.digest(saltedBytes)
      return hash.joinToString("") { "%02x".format(it) }
  }
  ```
  `SHA-256` is a high-speed collision-resistant digest algorithm designed for message integrity, not password hashing. It lacks any computational work factor or memory hardness. Furthermore, the salt is constructed using a fixed compile-time literal concatenated with the user's predictable email address.
- **Impact Analysis**:
  Modern GPUs can compute over 10 billion SHA-256 operations per second. A 4 to 6-digit passcode or typical alpha-numeric password hashed with this method can be cracked in less than one second via offline dictionary or rainbow table attacks if preferences are extracted.
- **Remediation**:
  Replace raw SHA-256 with a standard Key Derivation Function (KDF) such as `PBKDF2WithHmacSHA256` (minimum 210,000 iterations per OWASP recommendation) or Argon2id, utilizing a cryptographically secure, random 16-byte salt generated per user via `SecureRandom`.

```kotlin
// Production-Ready Remediation: PasswordHasher.kt
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

### FINDING-AUTH-03: Hardcoded Master Administrator Credentials in Client Source
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
  In `loginAdminDetailed()` (lines 1258–1264), incoming administrator logins compare input against these static constants.
- **Impact Analysis**:
  Because Android APKs are distributed as DEX bytecode, any reverse engineering tool (`jadx`, `apktool`, or `strings`) exposes `Lakshya190207@gmail.com` and `GoodDream@2026` within seconds. Any rogue actor can authenticate as Executive Administrator, granting complete access to the Catalog CRM Studio, enabling price alterations, customer inquiry inspection, and catalog manipulation.
- **Remediation**:
  Remove all static administrator credentials from the client repository. Admin authorization must be delegated to Firebase Authentication with Custom User Claims (`admin: true`) or an enterprise identity provider, verified on a trusted server.

```kotlin
// Production-Ready Pattern: Server-Enforced Admin Authorization
// In GoodDreamViewModel.kt:
fun authenticateAdminWithFirebase(idToken: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isAuthenticating = true) }
        try {
            // Validate Firebase Auth token custom claims
            val authUser = FirebaseAuth.getInstance().currentUser
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
                        adminErrorMessage = "Access Denied: Account lacks executive administrator claims."
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

### FINDING-AUTH-04: Client-Generated OTPs with Client-Side Verification and Privilege Escalation
- **Severity**: Critical
- **Affected File**: `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt`
- **Line Numbers**: 988, 996, 1038–1054
- **Vulnerability Description**:
  The one-time passcode (OTP) generation and verification workflow operates entirely on the client:
  1. `GoodDreamViewModel.requestUserEmailOtp()` (line 988) generates a random 6-digit code on the local phone: `String.format("%06d", secureRandom.nextInt(1_000_000))`.
  2. The code is saved to client memory: `_uiState.pendingGeneratedOtp = randomOtp`.
  3. `verifyUserEmailOtpDetailed()` (lines 1038–1054) tests user input directly against `_uiState.value.pendingGeneratedOtp`.
  4. If the email is `Lakshya190207@gmail.com`, lines 1053–1078 automatically elevate the session:
     ```kotlin
     val isAdmin = cleanEmail.equals(OFFICIAL_ADMIN_EMAIL, ignoreCase = true)
     ...
     _uiState.update { it.copy(isAdminAuthenticated = isAdmin) }
     ```
- **Impact Analysis**:
  Because the client is the single source of truth for generating, verifying, and granting session access:
  - Attackers using memory inspectors (e.g. Frida) or debuggers can read `pendingGeneratedOtp` directly from memory without ever accessing the email account.
  - Modifying bytecode or hooking `verifyUserEmailOtpDetailed()` to return `true` allows an attacker to enter the admin email and achieve executive privileges immediately.
- **Remediation**:
  Migrate OTP generation, transmission, and verification exclusively to a backend service (e.g., Firebase Cloud Functions or Firebase Authentication Email Link / Multi-Factor Auth). The mobile client must never know the expected OTP.

---

## Domain 2: Data Exposure & Secret Management

### FINDING-DATA-01: Debug Keystore (`debug.keystore`) Committed to Root Repository
- **Severity**: Critical
- **Affected File**: `debug.keystore` (Root Directory) & `app/build.gradle.kts`
- **Line Numbers**: `app/build.gradle.kts:42–47`
- **Vulnerability Description**:
  A 10.7 KB `debug.keystore` binary is committed directly in the project root. Furthermore, `app/build.gradle.kts` explicitly configures the production `release` signing configuration to fall back to this committed keystore:
  ```kotlin
  signingConfigs {
      create("release") {
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
// Production-Ready Remediation: app/build.gradle.kts
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
            // Do NOT fall back to debug.keystore in release signing!
            println("WARN: Production signing environment variables missing. Release build cannot be signed.")
        }
    }
}
```

---

### FINDING-DATA-02: Direct Client-Side SMTP Credentials Exposure in `EmailDeliveryService.kt`
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

### FINDING-DATA-03: Client-Side Gemini API Key Exposure in `GeminiChatService.kt`
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

### FINDING-DATA-04: Hardcoded Razorpay Fallback Key in `RazorpayPaymentHelper.kt`
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

### FINDING-DATA-05: PII and Verification Codes Logged to Logcat & Crashlytics
- **Severity**: Medium
- **Affected Files**:
  - `app/src/main/java/com/example/data/remote/EmailDeliveryService.kt:279, 428`
  - `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt:195`
  - `app/src/main/java/com/example/data/remote/SanctuaryFirebaseMessagingService.kt:18`
- **Vulnerability Description**:
  - `EmailDeliveryService.kt` logs: `Timber.tag(TAG).i("Successfully dispatched email '$subject' to $recipientEmail via Google SMTP")` where `$subject` contains the raw 6-digit OTP passcode (`Your Good Dream Verification Code: 123456`).
  - `GoodDreamViewModel.kt` and `SanctuaryFirebaseMessagingService.kt` log raw FCM tokens to `Timber.i`.
  - In `GoodDreamApplication.kt` (line 54), release crash logging routes non-debug logs to Firebase Crashlytics: `crashlytics.log("[${tag ?: "APP"}] $message")`.
- **Impact Analysis**:
  PII (user email addresses and verification codes) and device notification tokens leak into device Logcat logs and third-party Crashlytics consoles, violating user privacy frameworks and Google Play Data Safety standards.
- **Remediation**:
  Sanitize all log statements to redact email addresses and passcodes, and ensure no confidential tokens are routed to analytics or crash logging pipelines.

```kotlin
// Production-Ready Logging Sanitizer
fun sanitizeEmailForLog(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex <= 1) return "***@***"
    return "${email.take(2)}***${email.substring(atIndex)}"
}
```

---

## Domain 3: Component & Intent Security

### FINDING-COMP-01: Deep Link Intent Handling Lacks Android App Links Domain Verification
- **Severity**: Low
- **Affected Files**: `app/src/main/AndroidManifest.xml:20–31`, `app/src/main/java/com/example/MainActivity.kt:101–109`
- **Vulnerability Description**:
  `MainActivity` is exported (`android:exported="true"`) to act as the application launcher. While `handleIntent()` incorporates regex filtering on incoming order IDs (`^[A-Za-z0-9\-_]+$`), the manifest lacks Android App Link declarations with `android:autoVerify="true"` and digital asset link association. Any third-party app on the device can launch `MainActivity` with arbitrary intents targeting order tracking workflows.
- **Impact Analysis**:
  Risk of deep link hijacking and phishing UI spoofing if malicious applications intercept non-verified URI schemes or trigger unauthenticated tracking dialogs.
- **Remediation**:
  Configure verified Android App Links in `AndroidManifest.xml` with HTTPS domain verification.

```xml
<!-- In AndroidManifest.xml inside <activity android:name=".MainActivity"> -->
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

### FINDING-NET-01: Absence of Certificate Pinning for Production Endpoints
- **Severity**: Medium
- **Affected Files**: `app/src/main/res/xml/network_security_config.xml`, `app/src/main/java/com/example/data/gemini/GeminiChatService.kt`
- **Line Numbers**: `network_security_config.xml:1–9`
- **Vulnerability Description**:
  The `network_security_config.xml` correctly enforces `cleartextTrafficPermitted="false"`, but relies solely on system CA trust anchors without defining public key certificate pins (`<pin-set>`) for critical external APIs (Gemini, Razorpay, and cloud backend domains). Furthermore, `GeminiChatService.kt` initializes `OkHttpClient` without an application-layer `CertificatePinner`.
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
            <!-- Google Trust Services Primary Root CA Pin -->
            <pin digest="SHA-256">p14e2Uq1cQ7bN...=</pin>
            <!-- Backup Root Pin -->
            <pin digest="SHA-256">hxqRlPTuQGoKE...=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## Domain 5: Payment Security

### FINDING-PAY-01: Client-Side Payment Flow with Zero Cryptographic Signature Verification
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
// Production-Ready Backend Verification Architecture Pattern
// In RazorpayPaymentHelper.kt:
fun buildServerValidatedOptions(
    orderDraft: PendingPaymentOrderDraft,
    razorpayOrderId: String, // Issued strictly by authenticated backend server!
    apiKey: String
): JSONObject {
    val amountInPaise = (orderDraft.payableAmount * 100).toLong()
    return JSONObject().apply {
        put("key", apiKey)
        put("amount", amountInPaise)
        put("currency", "INR")
        put("name", "Good Dream Home Decor")
        put("order_id", razorpayOrderId) // Binding order_id prevents amount tampering!
        ...
    }
}
```

---

## Domain 6: Firebase & Cloud Storage Security

### FINDING-FIRE-01: Permissive Firestore Security Rules Permitting Catalog Destruction & PII Scraping
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
// Production-Ready Remediation: firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
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

    // Customer Inquiries: Public create with rate-limiting constraints, read/manage by Admin or Owner
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

    // Default deny
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

### FINDING-FIRE-02: App Check Configured in Client Without Backend Enforcement
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

### FINDING-BUILD-01: Release Signing Configuration Falls Back to Insecure Debug Keystore
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
  Configure Gradle to fail the release build immediately if production keystore environment variables are missing.

```kotlin
// Production-Ready Remediation: app/build.gradle.kts
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH")
        if (keystorePath != null && file(keystorePath).exists()) {
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        } else {
            // Throw exception to prevent release compilation with debug signing
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

### FINDING-BUILD-02: Missing `.env` and `.env.example` in Repository
- **Severity**: Low / Info
- **Affected Files**: `app/build.gradle.kts:84–88`, `.env`, `.env.example`
- **Vulnerability Description**:
  The Secrets Gradle Plugin is configured to read from `.env` and `.env.example`:
  ```kotlin
  secrets {
      propertiesFileName = ".env"
      defaultPropertiesFileName = ".env.example"
      ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  }
  ```
  Neither `.env` nor `.env.example` exists in the codebase, leading to unresolved build fields (e.g. `BuildConfig.RAZORPAY_KEY_ID`, `BuildConfig.GEMINI_API_KEY`) defaulting to null or triggering hardcoded test fallbacks.
- **Impact Analysis**:
  Build fragility, developer onboarding confusion, and accidental fallback to hardcoded mock credentials.
- **Remediation**:
  Commit a template `.env.example` with sanitized placeholders, and ensure `.env` is listed in `.gitignore`.

---

## Domain 8: Dependency & Third-Party Library Risks

### FINDING-DEP-01: Outdated and Unstable Alpha Dependencies in `libs.versions.toml`
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
  - `securityCrypto = "1.0.0"` (or robustly guarded `1.1.0-alpha06`)
  - `razorpay = "1.6.40"`
  - `okhttp = "4.12.0"`
  - `loggingInterceptor = "4.12.0"`

---

## Action Plan & Remediation Roadmap

1. **Immediate P0 Action (Pre-Release Blocker)**:
   - Purge hardcoded admin credentials from `GoodDreamViewModel.kt` (lines 1321–1322).
   - Delete `debug.keystore` from version control and update `app/build.gradle.kts` release signing.
   - Deploy hardened `firestore.rules` preventing unauthenticated writes, catalog deletes, and inquiry/order data exposure.
   - Remove client-side direct SMTP email dispatcher in `EmailDeliveryService.kt` to prevent Google App Password extraction.
   - Enforce server-side payment verification for Razorpay orders.

2. **P1 Action (Security Hardening)**:
   - Remove plaintext SharedPreferences fallback in `UserSessionManager.kt`.
   - Upgrade passcode hashing to `PBKDF2WithHmacSHA256` with random per-user salt.
   - Migrate OTP generation and verification to backend Cloud Functions.
   - Redact PII (emails, OTPs) from logging statements.

3. **P2 Action (Platform Hygiene & Compliance)**:
   - Configure Android App Links with domain verification in `AndroidManifest.xml`.
   - Implement certificate pinning in `network_security_config.xml`.
   - Update `gradle/libs.versions.toml` dependencies to latest stable versions.
