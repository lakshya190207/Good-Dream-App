# 🌙 Good Dream Sanctuary

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Hilt](https://img.shields.io/badge/Dagger--Hilt-2.59.2-00599C?style=for-the-badge)
![License](https://img.shields.io/badge/License-Proprietary-gold?style=for-the-badge)

**Handcrafted Architectural Sleep Systems & Luxury Home Decor**  
*Comfort for a Better Tomorrow*

</div>

---

## 📖 Overview

**Good Dream Sanctuary** is an enterprise-grade Android e-commerce and sleep-wellness application crafted with modern **Jetpack Compose** and **Clean Architecture**. It delivers a high-touch, bespoke shopping experience for handcrafted mattresses (SpringHaven collection), ergonomic bedding, and luxury furniture. 

The app combines traditional artisan craft with cutting-edge mobile technologies—featuring an on-device/cloud **Gemini AI Sleep Concierge**, interactive **Orthopedic Spine Pressure Visualizer**, **Bespoke Customization Studio**, **Razorpay** payments, and a statutory **25-Year Guarantee Vault**.

---

## ✨ Key Features

### 🛏️ 1. Dynamic Catalog & Architectural Showcase
* **Curated Collections:** Explore SpringHaven Pocket Spring, Royal Orthopedic Latex, Hybrid Comfort, and bespoke headboards.
* **Smart Search & Filters:** Instant multi-attribute search, category filtering, and sorting by comfort firmness ratings.
* **Full Statutory Compliance:** Every product view prominently discloses Indian Legal Metrology specs (Manufacturer, Packer, Country of Origin, Dimensions, and Grievance Officer details).

### 🤖 2. Gemini AI Sleep Concierge
* **On-Demand Sleep Guidance:** Natural conversation with Gemini AI advising on posture, spinal support, and ideal firmness index.
* **Context-Aware Recommendations:** Automatically cross-references client queries against the live mattress inventory.

### 🪡 3. Bespoke Studio & Visualizers
* **Custom Mattress Architect:** Commission bespoke mattress sizes (King, Queen, Custom inches), firmness layers, and fabric choices directly to master artisans.
* **Orthopedic Spine Pressure Visualizer:** Interactive graphical visualizer demonstrating pressure-point relief on cervical and lumbar zones.
* **Bedroom Fit Visualizer:** Preview mattress dimensions and floor clearances for different bedroom layouts.

### 💳 4. Secure Checkout & Payments
* **PCI-DSS Level 1 Delegated Payments:** Fully integrated **Razorpay Checkout** handling UPI, NetBanking, Credit/Debit cards, and Cash on Delivery (COD).
* **Coupon & Milestone Engine:** Real-time promotional codes with delivery milestone progress bars.
* **Address Book & Slots:** Save delivery addresses and select preferred white-glove doorstep delivery windows.

### 🛡️ 5. Enterprise-Grade Security
* **Hardware-Backed Encryption:** Stores sensitive credentials using Android `EncryptedSharedPreferences` (AES-256 GCM).
* **PBKDF2 Password Hashing:** Client passcodes are salted and hashed with PBKDF2 before storage.
* **Hardened Transport:** Network Security Config enforcing HTTPS exclusively (cleartext traffic completely disallowed).
* **Secrets Separation:** Zero hardcoded API keys; all sensitive configurations are resolved securely via the Gradle Secrets Plugin and `.env`.

### 📦 6. Orders, Warranty Vault & Admin CRM
* **Live Order Tracking:** Real-time timeline tracking from artisan tailoring to dispatch.
* **25-Year Guarantee Vault:** Generates authenticated digital guarantee certificates with instant warranty claim dispatch.
* **PDF Invoice Printer:** Native print helper rendering branded tax invoices for thermal and air printers.
* **Administrative CRM Studio:** Built-in protected dashboard allowing administrators to update lead statuses, manage orders, and edit product listings.

---

## 🛠️ Architecture & Tech Stack

* **UI Layer:** 100% Declarative [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (Material You), edge-to-edge layout, and custom haptics.
* **State Management:** MVI / MVVM with Unidirectional Data Flow (UDF), immutable UI state hierarchies, and `StateFlow.collectAsStateWithLifecycle()`.
* **Dependency Injection:** [Dagger-Hilt 2.59.2](https://dagger.dev/hilt/) with KSP compiler.
* **Local Persistence:** [Room Database 2.7](https://developer.android.com/training/data-storage/room) with Coroutine Flow observation.
* **Remote Networking:** [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 4](https://square.github.io/okhttp/) with custom interceptors and [Moshi](https://github.com/square/moshi) JSON codegen.
* **Cloud & Services:** Firebase Firestore, Firebase Cloud Messaging (FCM), Firebase Crashlytics, Firebase Performance Monitoring, App Check with reCAPTCHA.
* **Performance & Observability:**
  - `macrobenchmark` module for AndroidX Baseline Profile generation (improving cold starts by up to 40%).
  - LeakCanary for automated memory leak detection in debug builds.
  - StrictMode enabled in development to catch disk and network violations.
* **Testing:** Robolectric, JUnit 4, Roborazzi Screenshot Testing, MockK, AndroidX Test Runner.

---

## 🚀 How to Run on Another PC (Setup Guide)

Follow these step-by-step instructions to get the application running on any developer machine (Windows, macOS, or Linux).

### 📋 Prerequisites

Ensure you have the following installed on your PC:
1. **Git:** [Download Git](https://git-scm.com/downloads)
2. **Android Studio:** [Download Android Studio Ladybug (2024.2.1+)](https://developer.android.com/studio) or newer.
3. **JDK:** Java Development Kit 17 or 21 (bundled with Android Studio).
4. **Android SDK:**
   - Android SDK Platform 36 (Android 15 / 16 Preview)
   - Android SDK Build-Tools 36
   - Android Emulator (API 31+ recommended) or a physical Android device with **USB Debugging** enabled.

---

### Step 1: Clone the Repository

Open your terminal (PowerShell, Command Prompt, or Terminal on macOS/Linux) and clone the repository:

```bash
git clone https://github.com/lakshya190207/Good-Dream-App.git
cd Good-Dream-App
```

---

### Step 2: Configure the Environment Variables (`.env`)

The project uses the **Secrets Gradle Plugin** to protect API keys. A template file `.env.example` is provided in the root directory.

1. Create a `.env` file from the example:

   **Windows PowerShell:**
   ```powershell
   Copy-Item .env.example .env
   ```

   **macOS / Linux:**
   ```bash
   cp .env.example .env
   ```

2. Open `.env` in any text editor and populate your keys:
   ```properties
   # Gemini AI API Key (Get from https://aistudio.google.com/)
   GEMINI_API_KEY=your_gemini_api_key_here

   # Google reCAPTCHA Enterprise Site Key (Optional for development)
   RECAPTCHA_SITE_KEY=your_recaptcha_site_key

   # Google Gmail SMTP (For email OTP verification)
   SMTP_EMAIL=your_email@gmail.com
   SMTP_PASSWORD=your_16_character_google_app_password

   # Razorpay Payment Gateway (Test key from https://dashboard.razorpay.com/)
   RAZORPAY_KEY_ID=rzp_test_your_test_key_here
   ```

> 💡 **Note:** If you don't have personal keys immediately, default fallback values in `.env.example` allow the app to compile and run locally for UI preview and testing!

---

### Step 3: Open in Android Studio

1. Launch **Android Studio**.
2. Click **Open** (or `File > Open...`).
3. Browse to the folder where you cloned `Good-Dream-App` and click **OK**.
4. Allow Gradle to download dependencies and sync the project (this may take 2–3 minutes on first launch).

---

### Step 4: Run the App

1. Select **`app`** in the run configuration dropdown at the top toolbar.
2. Choose your target device:
   - **Physical Device:** Enable Developer Options & USB Debugging on your phone, then plug it in via USB.
   - **Emulator:** In Android Studio, open **Virtual Device Manager** and launch a device (e.g. Pixel 8 / Pixel 9 with API 34+).
3. Click the green **Run ▶** button (or press `Shift + F10`).

---

### Step 5: Useful Gradle Commands

You can also build and test directly from the command line:

```bash
# Windows
.\gradlew.bat testDebugUnitTest          # Run all unit tests
.\gradlew.bat assembleDebug              # Build debug APK (app/build/outputs/apk/debug/)
.\gradlew.bat assembleRelease            # Build optimized, R8-minified release APK

# macOS / Linux
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## 📂 Project Structure

```text
Good-Dream-App/
├── app/
│   ├── build.gradle.kts          # Dependencies, signing configs, ProGuard/R8
│   ├── google-services.json      # Firebase configuration
│   ├── proguard-rules.pro        # Production R8 keep rules & log stripping
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── GoodDreamApplication.kt   # Application class, Timber, StrictMode
│       │   │   ├── MainActivity.kt           # Edge-to-edge activity & navigation root
│       │   │   ├── data/
│       │   │   │   ├── config/               # App configuration & contact models
│       │   │   │   ├── gemini/               # Gemini AI Chat Client
│       │   │   │   ├── local/                # Room Database, DAOs, UserSessionManager (AES-256)
│       │   │   │   ├── model/                # Data entities (Product, Category, Order, CRM)
│       │   │   │   ├── remote/               # Retrofit, Firestore catalog, SMTP Email Service
│       │   │   │   └── repository/           # GoodDreamRepository (Single Source of Truth)
│       │   │   ├── ui/
│       │   │   │   ├── components/           # Reusable Compose components (TopBar, Modals, Visualizers)
│       │   │   │   ├── screens/              # Full-screen Composable views (Home, PDP, Checkout, Studio)
│       │   │   │   ├── theme/                # Typography, Shapes, Color palettes, Luxury Haptics
│       │   │   │   └── viewmodel/            # GoodDreamViewModel & UI State
│       │   │   └── util/                     # NotificationHelper, RazorpayPaymentHelper, InvoicePrinter
│       │   └── res/                          # XML resources, drawables, strings, network security config
│       └── test/                             # Unit tests, Robolectric tests, screenshot tests
├── macrobenchmark/                       # Baseline Profile generator module
├── gradle/
│   └── libs.versions.toml                # Centralized Gradle Version Catalog
├── .env.example                          # Safe environment variable template
├── .gitignore                            # Comprehensive secret & build file exclusions
└── README.md                             # Documentation
```

---

## 👤 Author & Maintainer

* **Author:** Lakshya
* **Email:** [lakshya190207@gmail.com](mailto:lakshya190207@gmail.com)
* **Organization:** Good Dream Home Decor Private Limited

---

## 📄 License

Copyright © 2026 Good Dream Home Decor Private Limited. All rights reserved.  
Unauthorized copying, distribution, or reproduction of this codebase or its brand assets is strictly prohibited.
