---
name: android-play-store-compliance
description: >-
  Pre-submission Google Play Store compliance runbook to prevent app rejections, account suspensions, Data Safety form mismatches, and permission policy violations.
---

# Google Play Store Compliance & Rejection Prevention

Use this skill before uploading or updating any app on Google Play Console to guarantee 100% policy compliance across Data Safety, permissions hygiene, in-app account deletion, and Android 14/15/16 platform restrictions.

---

## 1. The Top 6 Reasons Google Rejects Production Apps

### 1. Missing In-App Account Deletion Requirement (Mandatory)
If your app allows users to create an account, Google Play Policy **strictly mandates**:
- An easy-to-find in-app option to delete their account and associated data (e.g. under Settings > Account > Delete Account).
- A public web link/URL where users can request account and data deletion without reinstalling the app.

### 2. Runtime Permission Violations & Modern Media Access
- **Never request** `READ_EXTERNAL_STORAGE` or broad `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` unless your app is a core Gallery/File Manager app.
- **Always use the Photo Picker API** (`ActivityResultContracts.PickVisualMedia()`) which requires **zero permissions** and is compliant by default:
  ```kotlin
  val pickMedia = rememberLauncherForActivityResult(
      ActivityResultContracts.PickVisualMedia()
  ) { uri ->
      if (uri != null) handleSelectedImage(uri)
  }
  // Launch:
  pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
  ```

### 3. Foreground Service Types (Android 14+)
If declaring `<service android:name="..." android:foregroundServiceType="...">`, you MUST:
- Specify an explicit type in `AndroidManifest.xml` (e.g. `dataSync`, `mediaPlayback`, `location`).
- Request the matching permission (`FOREGROUND_SERVICE_DATA_SYNC`).
- Provide a clear justification in the Play Console declaration form.

### 4. Target SDK 35/36 & 64-bit Architecture
- Check Google Play minimum target SDK deadlines.
- Always ensure 64-bit architectures (`arm64-v8a`, `x86_64`) are included in your App Bundle.

### 5. Accurate Data Safety Form
Every SDK in your project (Firebase, Analytics, Crashlytics, AdMob, Coil) collects data that MUST be disclosed:
- Crashlytics: Disclose "Diagnostics & Crash logs", Purpose: "App functionality, Analytics".
- Device identifiers: Disclose "Device or other IDs".
- Declare whether data is encrypted in transit (Yes, HTTPS used throughout).

### 6. Accurate Privacy Policy URL
- Must be a live, public, non-gated URL (no Google Drive links or login gates).
- Must explicitly state the entity name matching your Play Console Developer Name.

### 7. Application ID (`package_name`) & Keystore Lock
- **Application ID Finalization**:
  - The `applicationId` declared in `app/build.gradle.kts` is permanently locked once the first App Bundle is uploaded to Google Play Console. It can NEVER be changed afterwards.
  - Verify and confirm branding (e.g. `com.gooddream.luxury` vs default sandbox IDs) before initial submission.
- **Upload Keystore Verification**:
  - Never upload builds signed with `debug.keystore` (Google Play Console will reject them).
  - Generate a secure RSA upload key (`my-upload-key.jks`) using `keytool` and securely store offline backups of passwords.
- **AAB Generation**:
  - Google Play Console mandates `.aab` (Android App Bundle). Always verify with `./gradlew.bat bundleRelease`.

---

## 2. Pre-Submission Verification Checklist

Run through this checklist before every release:
- [ ] Application ID verified and finalized for production branding
- [ ] Signed with custom RSA upload keystore (`my-upload-key.jks`), NOT debug key
- [ ] Release AAB generated (`./gradlew.bat bundleRelease`) and size verified
- [ ] Target SDK updated in `app/build.gradle.kts`
- [ ] Photo Picker used instead of broad media storage permissions
- [ ] Settings screen includes an explicit "Delete Account" flow
- [ ] All HTTP traffic disabled (`cleartextTrafficPermitted="false"`)
- [ ] Google Play Data Safety form answers match all bundled SDKs
- [ ] Privacy Policy URL accessible and HTTPS secured
- [ ] Legal Metrology and Country of Origin displayed on all product detail pages
- [ ] Statutory Grievance Redressal Officer published in legal/support screens
- [ ] Explicit Terms & Privacy consent present at Checkout
