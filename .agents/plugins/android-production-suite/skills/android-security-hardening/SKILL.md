---
name: android-security-hardening
description: >-
  Enterprise-grade security hardening protocols for Android apps, covering Android Keystore, EncryptedSharedPreferences, Network Security Config, ProGuard/R8 obfuscation, and Play Integrity.
---

# Android Security Hardening & Data Protection

Use this skill when auditing or implementing security controls, storing sensitive credentials, configuring SSL pinning, hardening network communication, or preparing for Google Play Data Safety compliance.

---

## 1. Secure Credential Storage (Android Keystore & Crypto)

Never store auth tokens, private API keys, or user credentials in plain XML SharedPreferences.

### A. EncryptedSharedPreferences Implementation
```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferencesManager {

    fun getEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthToken(context: Context, token: String) {
        getEncryptedPreferences(context).edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(context: Context): String? {
        return getEncryptedPreferences(context).getString("auth_token", null)
    }
}
```

---

## 2. Network Security Configuration & SSL Pinning

### A. Manifest Declaration
In `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

### B. `res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Disable cleartext HTTP traffic globally -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Optional: Certificate Pinning for Production Domain -->
    <domain-config>
        <domain includeSubdomains="true">api.gooddream.com</domain>
        <pin-set expiration="2027-01-01">
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <!-- Backup pin -->
            <pin digest="SHA-256">BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## 3. Component & Intent Security Checklist

1. **Explicit Exports**:
   Every Activity, Service, Receiver in `AndroidManifest.xml` must explicitly state `android:exported="false"` unless an Intent Filter exposes it publicly.
2. **PendingIntents**:
   Always specify `PendingIntent.FLAG_IMMUTABLE` (or `FLAG_MUTABLE` only when external modification is strictly necessary).
3. **Deep Links**:
   Validate incoming deep link URI parameters before routing. Use App Links with `assetlinks.json` domain verification.
4. **Tapjacking / Overlay Protection**:
   Protect payment or authorization screens using:
   ```kotlin
   view.filterTouchesWhenObscured = true
   ```
5. **Disable Screen Capture for Sensitive Screens**:
   ```kotlin
   activity.window.setFlags(
       WindowManager.LayoutParams.FLAG_SECURE,
       WindowManager.LayoutParams.FLAG_SECURE
   )
   ```
