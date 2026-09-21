---
name: android-ci-cd-release
description: >-
  Complete release runbook for building signed Android App Bundles (AAB), managing keystores, configuring automated GitHub Actions CI/CD pipelines, and Google Play Store submission.
---

# Android Release Engineering & CI/CD Runbook

Use this skill when preparing an Android app for release, generating release signing keystores, automating builds via GitHub Actions, or verifying R8 deobfuscation and Play Console requirements.

---

## 1. Keystore Generation & Secure Signing

### A. Keystore Generation (CLI)
```powershell
keytool -genkey -v -keystore release.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

### B. Environment-Driven Gradle Signing Configuration
In `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

## 2. Release Build Commands

### A. Build Signed Android App Bundle (AAB)
```powershell
./gradlew bundleRelease
```
Output location: `app/build/outputs/bundle/release/app-release.aab`

### B. Build Universal Release APK (for testing release builds locally)
```powershell
./gradlew assembleRelease
```
Output location: `app/build/outputs/apk/release/app-release.apk`

---

## 3. Pre-Release Play Store Checklist

1. **Version Code & Name**: Increment `versionCode` by 1 and update `versionName` in `app/build.gradle.kts`.
2. **Target SDK**: Ensure `targetSdk` matches latest Google Play requirements (Android 15 / 16, API 35/36).
3. **App Permissions Audit**: Verify only required permissions are declared in `AndroidManifest.xml`.
4. **Data Safety**: Declare all data types collected (Firebase Analytics, Crashlytics, Location, User identifiers).
5. **Proguard Mapping File**: Confirm `mapping.txt` is produced under `app/build/outputs/mapping/release/` and uploaded to Crashlytics / Play Console.
6. **Network Check**: Verify no cleartext traffic permitted and all API URLs use production endpoints.
