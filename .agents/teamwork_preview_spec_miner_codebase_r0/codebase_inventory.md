# GoodDream Android Application — Comprehensive Codebase Inventory & Architectural Survey

**Survey Date**: September 19, 2026  
**Investigator**: `teamwork_preview_spec_miner_codebase_r0` (Specification Miner & Codebase Inventory Specialist)  
**Target Repository**: `d:\Avi\app`  
**Application Name**: GoodDream (Luxury Bedding & Sleep Sanctuary E-Commerce Mobile App)  
**Target Platform**: Android (MinSdk 26, TargetSdk 35, CompileSdk 35)  
**Primary Language / Framework**: Kotlin 2.0.21, Jetpack Compose, Material Design 3, Kotlin Coroutines & Flow  

---

## Executive Summary

The GoodDream Android application is a luxury e-commerce and mattress customization mobile application. The architecture implements an offline-first single-activity Compose pattern with Room as a local cache and Firebase Cloud Firestore as the remote catalog sync engine. It incorporates generative AI via the Google Generative AI Client SDK (Gemini), native payment processing via the Razorpay Android SDK, client-side SMTP dispatch via Jakarta/JavaMail, and interactive mattress customization via the bespoke studio 3D-styled configurator.

This inventory provides a file-level mapping of all 52 production Kotlin source files, 8 test files, 10 resource directories, build configurations, security artifacts, and external integration points.

---

## 1. Architectural Layer Directory & File Inventory

### 1.1 Root & Application Entry Points (2 Files)

| # | File Path | Architectural Role | Key Classes / Functions | Primary Responsibility |
|---|-----------|--------------------|-------------------------|------------------------|
| 1 | `app/src/main/java/com/example/GoodDreamApplication.kt` | Application Entry Point | `class GoodDreamApplication : Application()` | Global application lifecycle coordinator. Plants `Timber.DebugTree()`, creates notification channel `gooddream_luxury_channel`, instantiates `GoodDreamRepository` singleton, and checks network connectivity. |
| 2 | `app/src/main/java/com/example/MainActivity.kt` | Single Activity Host | `class MainActivity : ComponentActivity(), PaymentResultListener` | Host Activity implementing edge-to-edge UI (`enableEdgeToEdge`), deep-link intent routing (order tracking notifications), Razorpay payment callbacks (`onPaymentSuccess`, `onPaymentError`), custom `BackHandler` routing, and drawer scaffold. |

---

### 1.2 Data Layer — Local Persistence & Security (3 Files)

| # | File Path | Architectural Role | Key Classes / Interfaces | Primary Responsibility |
|---|-----------|--------------------|--------------------------|------------------------|
| 3 | `app/src/main/java/com/example/data/local/AppDatabase.kt` | Room Database | `@Database class AppDatabase : RoomDatabase()` | Room database definition (version 2, schemas exported). Declares DAOs: `productDao()`, `cartDao()`, `wishlistDao()`, `orderDao()`, `inquiryDao()`, `categoryDao()`. Provides Thread-safe Singleton builder with `MIGRATION_1_2`. |
| 4 | `app/src/main/java/com/example/data/local/CatalogDaos.kt` | Room DAOs | `ProductDao`, `CartDao`, `WishlistDao`, `OrderDao`, `InquiryDao`, `CategoryDao` | Defines reactive Room queries returning `Flow<List<T>>` for reactive UI binding, conflict replacement strategies (`OnConflictStrategy.REPLACE`), upsert methods, and transactional order placements. |
| 5 | `app/src/main/java/com/example/data/local/UserSessionManager.kt` | Secure Session Store | `class UserSessionManager(context: Context)` | Manages user session state, customer profile, and admin authentication. Uses AndroidX Security `EncryptedSharedPreferences` backed by Android Keystore with fallback to standard `SharedPreferences`. Handles password hashing and session tokens. |

---

### 1.3 Data Layer — Remote Services & Messaging (3 Files)

| # | File Path | Architectural Role | Key Classes / Services | Primary Responsibility |
|---|-----------|--------------------|------------------------|------------------------|
| 6 | `app/src/main/java/com/example/data/remote/FirestoreCatalogService.kt` | Firestore Sync Engine | `class FirestoreCatalogService` | Synchronizes catalog products, inquiries, and orders with Google Cloud Firestore collections: `/products`, `/orders`, `/inquiries`, `/newsletter`. Implements real-time snapshot listeners with coroutine Flow emission. |
| 7 | `app/src/main/java/com/example/data/remote/SanctuaryFirebaseMessagingService.kt` | Push Notification Receiver | `class SanctuaryFirebaseMessagingService : FirebaseMessagingService()` | Handles incoming Firebase Cloud Messaging (FCM) push payloads (`onMessageReceived`), extracts payload type (`order_update`, `promotional`), and triggers localized system heads-up notifications. |
| 8 | `app/src/main/java/com/example/data/remote/EmailDeliveryService.kt` | Direct SMTP Client | `object EmailDeliveryService` | Implements asynchronous direct client-side SMTP dispatch using JavaMail (`smtp.gmail.com:587` with STARTTLS) for order confirmations, inquiries, and OTP delivery. Dispatches on `Dispatchers.IO`. |

---

### 1.4 Data Layer — Configuration & Remote Defaults (2 Files)

| # | File Path | Architectural Role | Key Classes / Objects | Primary Responsibility |
|---|-----------|--------------------|-----------------------|------------------------|
| 9 | `app/src/main/java/com/example/data/config/AppConfig.kt` | Configuration Models | `data class AppConfig`, `data class SmtpConfig`, `data class RazorpayConfig` | Typed data models holding runtime operational parameters (gateway mode, feature flags, API endpoints, payment keys, timeout thresholds). |
| 10 | `app/src/main/java/com/example/data/config/AppConfigProvider.kt` | Configuration Provider | `object AppConfigProvider` | Aggregates runtime configurations from `BuildConfig` fields (populated via `.env` secrets gradle plugin) and remote Firestore config doc (`/config/app_settings`). |

---

### 1.5 Data Layer — Domain & Entity Models (2 Files)

| # | File Path | Architectural Role | Key Classes / Enums | Primary Responsibility |
|---|-----------|--------------------|---------------------|------------------------|
| 11 | `app/src/main/java/com/example/data/model/CatalogEntities.kt` | Core Room Entities | `@Entity ProductEntity`, `@Entity CartItemEntity`, `@Entity WishlistItemEntity`, `@Entity OrderEntity`, `@Entity InquiryEntity`, `@Entity CategoryEntity` | Database table entities modeling the entire e-commerce domain. Contains embedded Room TypeConverters (`Converters.kt`) for JSON string lists and timestamp dates. |
| 12 | `app/src/main/java/com/example/data/model/BespokeStudioModels.kt` | Bespoke Customizer Models | `enum class BespokeCoreType`, `BespokeComfortLayer`, `BespokeQuiltCover`, `BespokeSizeStandard`, `BespokeFirmness`, `data class BespokeMattressConfiguration` | Domain models, material specifications, ergonomic firmness multipliers, dimension matrix, and dynamic price calculation engines for custom luxury mattresses. |

---

### 1.6 Data Layer — Gemini Generative AI Integration (2 Files)

| # | File Path | Architectural Role | Key Classes / Interfaces | Primary Responsibility |
|---|-----------|--------------------|--------------------------|------------------------|
| 13 | `app/src/main/java/com/example/data/gemini/ChatMessage.kt` | AI Chat Domain Models | `enum class MessageRole`, `data class ChatMessage` | Encapsulates conversational history entries between user and AI sleep concierge, including timestamp and message delivery status. |
| 14 | `app/src/main/java/com/example/data/gemini/GeminiChatService.kt` | Gemini API Client | `class GeminiChatService(apiKey: String)` | Integrates Google's `generativeai` SDK (`gemini-1.5-flash`). Enforces system sleep consultant persona prompts, injects catalog product metadata into context, and streams or returns assistant responses. |

---

### 1.7 Data Layer — Repository & Default Catalog (2 Files)

| # | File Path | Architectural Role | Key Classes / Objects | Primary Responsibility |
|---|-----------|--------------------|-----------------------|------------------------|
| 15 | `app/src/main/java/com/example/data/repository/DefaultCatalogData.kt` | Hardcoded Fallback Catalog | `object DefaultCatalogData` | Provides seed catalog data (9 luxury mattresses, bed frames, silk pillows, duvets) used to prepopulate Room on database creation and as offline fallback. |
| 16 | `app/src/main/java/com/example/data/repository/GoodDreamRepository.kt` | Single Source of Truth | `class GoodDreamRepository(...)` | Mediates data access across Room DAOs, Firestore remote sync, Gemini chat, and SMTP mailer. Exposes unified `Flow` streams for catalog products, cart items, wishlist, user orders, and inquiries. |

---

### 1.8 UI Layer — ViewModel & State Management (1 File)

| # | File Path | Architectural Role | Key Classes / Enums | Primary Responsibility |
|---|-----------|--------------------|---------------------|------------------------|
| 17 | `app/src/main/java/com/example/ui/viewmodel/GoodDreamViewModel.kt` | Central UI State Machine | `class GoodDreamViewModel(...) : ViewModel()`, `data class GoodDreamUiState`, `enum class MainTab`, `enum class ActivePage` | Primary ViewModel managing entire application state (~1381 lines). Controls active navigation page/tab, cart mutations, checkout flow, filter/search queries, inquiry submissions, bespoke configuration, admin catalog mutations, and authentication tokens. |

---

### 1.9 UI Layer — Screens (13 Files)

| # | File Path | Architectural Role | Key Composable Functions | Primary Responsibility |
|---|-----------|--------------------|--------------------------|------------------------|
| 18 | `app/src/main/java/com/example/ui/screens/HomeScreen.kt` | Top-Level Tab Screen | `HomeScreen(...)` | Main landing screen. Features hero carousel, brand story banner, curated luxury categories row, featured sleep systems grid, interactive sleep quiz banner, and VIP newsletter signup. |
| 19 | `app/src/main/java/com/example/ui/screens/ProductListingScreen.kt` | Catalog Listing Screen | `ProductListingScreen(...)` | Search and browsing screen with category chip filters, sorting bottom sheet (price, rating, firmness), price range sliders, and animated responsive grid of product cards. |
| 20 | `app/src/main/java/com/example/ui/screens/ProductDetailScreen.kt` | Product Detail (PDP) | `ProductDetailScreen(...)` | Immersive PDP with multi-image gallery pager, dimension selector, firmness gauge visualizer, material breakdown, customer review list, add-to-cart bottom bar, and wishlist toggle. |
| 21 | `app/src/main/java/com/example/ui/screens/CartScreen.kt` | Shopping Cart Screen | `CartScreen(...)` | Displays cart items with quantity steppers, item removal animations, coupon code entry input, price breakdown (subtotal, tax, delivery fee), and checkout CTA. |
| 22 | `app/src/main/java/com/example/ui/screens/CheckoutScreen.kt` | Order Checkout Screen | `CheckoutScreen(...)` | Multi-step checkout screen: delivery address input, contact validation, payment method selection (Razorpay Online vs. Cash on Delivery), and order placement trigger. |
| 23 | `app/src/main/java/com/example/ui/screens/OrderSuccessScreen.kt` | Order Confirmation Screen | `OrderSuccessScreen(...)` | Post-purchase celebration screen with animated checkmark, generated Order ID, estimated delivery timeline, invoice download button, and return-to-home CTA. |
| 24 | `app/src/main/java/com/example/ui/screens/CustomerLoginScreen.kt` | Authentication Screen | `CustomerLoginScreen(...)` | User login and registration screen. Supports password authentication, OTP-based login, biometric authentication prompt, and toggle to password recovery. |
| 25 | `app/src/main/java/com/example/ui/screens/AdminLoginScreen.kt` | Admin Portal Login | `AdminLoginScreen(...)` | Secure portal entry for GoodDream staff and store administrators. Requires master administrative credentials and optional 2FA code. |
| 26 | `app/src/main/java/com/example/ui/screens/AccountScreen.kt` | User Profile Screen | `AccountScreen(...)` | Displays customer profile details, order history overview, saved shipping addresses, concierge contact shortcuts, app settings, and logout action. |
| 27 | `app/src/main/java/com/example/ui/screens/CategoriesHubScreen.kt` | Category Explorer | `CategoriesHubScreen(...)` | Visual categorical taxonomy browser showcasing mattress collections, artisan bedding, luxury toppers, and ergonomic pillows. |
| 28 | `app/src/main/java/com/example/ui/screens/BespokeStudioScreen.kt` | Custom Configurator | `BespokeStudioScreen(...)` | Custom mattress builder with interactive 3D-styled layer stacking visualizer, core spring selection, comfort latex foam options, cover fabrics, monogramming, and live pricing. |
| 29 | `app/src/main/java/com/example/ui/screens/DedicatedFormScreens.kt` | Specialized Support Forms | `CustomInquiryScreen`, `YourNeedsScreen`, `RepairsScreen`, `ComplaintsScreen`, `FeedbackScreen`, `WarrantiesScreen` | Suite of customer service and warranty claim forms with input validation, photo attachment pickers, and direct submission to Firestore and SMTP. |
| 30 | `app/src/main/java/com/example/ui/screens/SecondaryScreens.kt` | Rewards & Inbox Screens | `RewardsScreen`, `NotificationsScreen` | Displays VIP customer loyalty points, referral bonuses, and system announcement notifications. |

---

### 1.10 UI Layer — Reusable Components & Modals (14 Files)

| # | File Path | Architectural Role | Key Composable Components | Primary Responsibility |
|---|-----------|--------------------|---------------------------|------------------------|
| 31 | `app/src/main/java/com/example/ui/components/GoodDreamTopAppBar.kt` | Top App Bar | `GoodDreamTopAppBar(...)` | Universal header bar with brand serif typography, navigation menu drawer toggle, search trigger, cart badge counter, and wishlist counter. |
| 32 | `app/src/main/java/com/example/ui/components/NavigationComponents.kt` | Bottom Navigation | `GoodDreamBottomNavBar(...)`, `GoodDreamNavRail(...)` | Bottom navigation bar for standard phone viewports and navigation rail for large screens/foldables. Renders icons with active state indicators. |
| 33 | `app/src/main/java/com/example/ui/components/AppDrawer.kt` | Navigation Drawer | `AppDrawerContent(...)` | Modal navigation drawer providing shortcuts to all categories, customer support channels, policies, admin login, and user account status. |
| 34 | `app/src/main/java/com/example/ui/components/AiChatBotModal.kt` | AI Concierge Modal | `AiChatBotModal(...)` | Floating conversational bottom sheet integrating the Gemini AI assistant. Renders markdown-styled chat bubbles, quick prompt suggestions, and error states. |
| 35 | `app/src/main/java/com/example/ui/components/ProductComparisonModal.kt` | Spec Comparison Tool | `ProductComparisonModal(...)` | Side-by-side comparison modal allowing users to compare firmness ratings, dimensions, materials, spring counts, and prices across two mattresses. |
| 36 | `app/src/main/java/com/example/ui/components/ProductCrmStudioModal.kt` | Admin CRM & Inventory | `ProductCrmStudioModal(...)` | Comprehensive store manager modal. Allows adding/editing/deleting catalog products, reviewing customer orders, updating tracking numbers, and answering inquiries. |
| 37 | `app/src/main/java/com/example/ui/components/SleepFirmnessQuizModal.kt` | Diagnostic Sleep Quiz | `SleepFirmnessQuizModal(...)` | 4-step interactive quiz evaluating sleeping position, body type, and firmness preference to recommend ideal GoodDream mattress models. |
| 38 | `app/src/main/java/com/example/ui/components/OrderTrackingModal.kt` | Live Order Tracker | `OrderTrackingModal(...)` | Order tracking sheet displaying real-time delivery status, carrier dispatch tracking ID, and interactive step-by-step progress timeline. |
| 39 | `app/src/main/java/com/example/ui/components/CartAndWishlistSheets.kt` | Quick Peek Sheets | `CartQuickSheet(...)`, `WishlistModalSheet(...)` | Slide-up modal sheets providing fast access to cart contents and saved wishlist items without navigating away from the current screen. |
| 40 | `app/src/main/java/com/example/ui/components/AuthRequiredGateModal.kt` | Auth Interceptor Modal | `AuthRequiredGateModal(...)` | Security gate dialog intercepted when unauthenticated users attempt restricted actions (e.g. checkout, bespoke ordering, wishlist saving). |
| 41 | `app/src/main/java/com/example/ui/components/LegalPoliciesModal.kt` | Policy Viewer | `LegalPoliciesModal(...)` | Markdown-rendered modal displaying Terms of Service, Privacy Policy, Refund Policy, and Cookie Policy. |
| 42 | `app/src/main/java/com/example/ui/components/SupportAndInquiryModals.kt` | Quick Contact Dialogs | `QuickInquiryModal(...)`, `ScheduleConsultationModal(...)` | Lightweight dialogs for fast phone consultation scheduling and concierge email callbacks. |
| 43 | `app/src/main/java/com/example/ui/components/ShimmerSkeletons.kt` | Skeleton Loaders | `ProductCardShimmer()`, `HeroBannerShimmer()`, `CartItemShimmer()` | Animated shimmer loading placeholders providing smooth perceived loading performance during data fetches. |
| 44 | `app/src/main/java/com/example/ui/components/ComfortAnimations.kt` | Micro-Interactions | `FloatingHeartIcon()`, `PulsingGlow()`, `SpringRevealAnimation()` | Luxury motion effects, smooth transitions, and tactile feedback animations. |

---

### 1.11 UI Layer — Theme & Styling (4 Files)

| # | File Path | Architectural Role | Key Declarations | Primary Responsibility |
|---|-----------|--------------------|------------------|------------------------|
| 45 | `app/src/main/java/com/example/ui/theme/Color.kt` | Color Palette | `ForestGreenPrimary`, `WarmCreamBackground`, `DeepGoldAccent`, `RichEbonyText`, `SurfaceIvory` | Defines the luxury brand color palette (gold accents, champagne neutrals, deep forest green, dark mode obsidian). |
| 46 | `app/src/main/java/com/example/ui/theme/Type.kt` | Typography Scale | `Typography` | Material 3 typography tokens. Configures serif headlines and sans-serif body styles. |
| 47 | `app/src/main/java/com/example/ui/theme/Theme.kt` | Theme Provider | `GoodDreamTheme(darkTheme: Boolean, ...)` | Compose theme wrapper applying dynamic color schemes, typography, and system bar luminance matching. |
| 48 | `app/src/main/java/com/example/ui/theme/LuxuryHaptics.kt` | Tactile Feedback | `object LuxuryHaptics` | Provides bespoke vibration and tactile feedback patterns (e.g., selection tick, success pulse, error double-buzz) via `Vibrator` / `VibratorManager`. |

---

### 1.12 Utility Layer (4 Files)

| # | File Path | Architectural Role | Key Classes / Objects | Primary Responsibility |
|---|-----------|--------------------|-----------------------|------------------------|
| 49 | `app/src/main/java/com/example/util/RazorpayPaymentHelper.kt` | Payment Orchestrator | `class RazorpayPaymentHelper(activity: Activity)` | Interfaces with `com.razorpay.Checkout`. Prepares JSON payment payload with customer details, sets color theme, and starts Razorpay checkout activity. |
| 50 | `app/src/main/java/com/example/util/NotificationHelper.kt` | System Notifications | `object NotificationHelper` | Builds and posts native Android system notifications (order updates, tracking status, promotional alerts) using `NotificationCompat.Builder`. |
| 51 | `app/src/main/java/com/example/util/InvoicePrinterHelper.kt` | PDF Invoice Generator | `object InvoicePrinterHelper` | Renders a styled luxury HTML invoice with order summary and customer details, then passes it to Android's `PrintManager` for native PDF generation. |
| 52 | `app/src/main/java/com/example/util/NetworkConnectivityObserver.kt` | Network Monitor | `class NetworkConnectivityObserver(context: Context)` | Uses `ConnectivityManager.NetworkCallback` to observe cellular and Wi-Fi reachability, exposing a reactive `Flow<Boolean>` for offline banner rendering. |

---

### 1.13 Test Suites (8 Files)

| # | File Path | Test Type | Target Scope |
|---|-----------|-----------|--------------|
| 1 | `app/src/test/java/com/example/ExampleUnitTest.kt` | Unit Test | Basic mathematical assertions. |
| 2 | `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Robolectric Test | Shadow Android context and basic lifecycle test. |
| 3 | `app/src/test/java/com/example/GreetingScreenshotTest.kt` | Screenshot / UI Test | Compose screenshot test harness. |
| 4 | `app/src/test/java/com/example/BespokeStudioUnitTest.kt` | Domain Unit Test | Verifies `BespokeStudioModels` price calculator, dimension limits, and firmness weighting. |
| 5 | `app/src/test/java/com/example/RazorpayPaymentUnitTest.kt` | Payment Unit Test | Tests payment JSON payload generation and currency formatting. |
| 6 | `app/src/test/java/com/example/NotificationPushUnitTest.kt` | Push Unit Test | Tests push notification payload parsing and intent extra validation. |
| 7 | `app/src/test/java/com/example/EmailDeliveryUnitTest.kt` | Remote Unit Test | Validates SMTP recipient format and MIME message encoding logic. |
| 8 | `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | Instrumented Test | Android instrumentation runner verification (`getInstrumentation().targetContext`). |

---

## 2. Navigation Architecture & Entry Points

### 2.1 Navigation Pattern
The application uses an **in-memory, state-driven navigation architecture** mediated by `GoodDreamViewModel` rather than AndroidX Navigation Compose `NavHost`. 
State is driven by:
1. `uiState.selectedTab: MainTab` (Primary bottom navigation tab)
2. `uiState.activePage: ActivePage` (Full-screen overlay/modal destination)
3. `uiState.selectedProduct: ProductEntity?` (Product Detail Screen overlay)
4. `uiState.isCategoriesHubOpen: Boolean` (Category taxonomy explorer)

### 2.2 Top-Level Tabs (`MainTab`)

| Tab Enum | Label | Backing Screen | Description |
|----------|-------|----------------|-------------|
| `MainTab.HOME` | Home | `HomeScreen.kt` | Main storefront, brand hero, category highlights, sleep quiz banner. |
| `MainTab.PRODUCTS` | Collection | `ProductListingScreen.kt` | Complete mattress and bedding catalog with filters and sorting. |
| `MainTab.NEW_LAUNCHES` | Novelties | `ProductListingScreen(newLaunchesOnly = true)` | Filtered view showcasing newly released luxury sleep systems. |
| `MainTab.ACCOUNT` | Sanctuary | `AccountScreen.kt` | User profile, order history, address book, customer support shortcuts. |

### 2.3 Secondary Destinations (`ActivePage`)

| ActivePage Enum | Target Composable | Navigation Trigger | Back / Dismiss Behavior |
|-----------------|-------------------|--------------------|-------------------------|
| `CART` | `CartScreen.kt` | TopAppBar cart icon, quick sheet CTA | Closes active page; returns to prior tab |
| `CHECKOUT` | `CheckoutScreen.kt` | "Proceed to Checkout" button in Cart | Navigates back to `CART` |
| `ORDER_SUCCESS` | `OrderSuccessScreen.kt` | Successful payment or COD submission | Closes active page; resets to `HOME` |
| `CUSTOM_INQUIRY` | `DedicatedFormScreens.CustomInquiryScreen` | Drawer / Account "Custom Inquiry" | Closes active page |
| `YOUR_NEEDS` | `DedicatedFormScreens.YourNeedsScreen` | Drawer / Support menu | Closes active page |
| `REPAIRS` | `DedicatedFormScreens.RepairsScreen` | Drawer / Account "Request Repair" | Closes active page |
| `COMPLAINTS` | `DedicatedFormScreens.ComplaintsScreen` | Drawer / Account "Register Complaint" | Closes active page |
| `FEEDBACKS` | `DedicatedFormScreens.FeedbackScreen` | Drawer / Account "Leave Feedback" | Closes active page |
| `SERVICE_WARRANTIES` | `DedicatedFormScreens.WarrantiesScreen` | Drawer / Account "Warranty Claims" | Closes active page |
| `SPONSOR_REWARDS` | `SecondaryScreens.RewardsScreen` | Drawer "VIP Rewards" | Closes active page |
| `PURCHASE_REWARDS` | `SecondaryScreens.RewardsScreen` | Account "My Rewards" | Closes active page |
| `MESSAGE_FOR_YOU` | `SecondaryScreens.NotificationsScreen` | TopAppBar bell / Drawer "Inbox" | Closes active page |
| `ADMIN_PANEL` | `ProductCrmStudioModal.kt` | Admin login success or drawer shortcut | Closes active page |
| `USER_LOGIN` | `CustomerLoginScreen.kt` | Account tab login button or auth gate | Closes active page |
| `ADMIN_LOGIN` | `AdminLoginScreen.kt` | Drawer "Staff Login" / Secret gesture | Closes active page |
| `AI_CHAT_BOT` | `AiChatBotModal.kt` | Floating action button (FAB) / Home banner | Closes active page |
| `SLEEP_QUIZ` | `SleepFirmnessQuizModal.kt` | Home quiz banner / PDP firmness helper | Closes active page |
| `ORDER_TRACKING` | `OrderTrackingModal.kt` | Account order item / System notification tap | Closes active page |
| `COMPARE_PRODUCTS` | `ProductComparisonModal.kt` | PDP "Compare" button | Closes active page |
| `LEGAL_POLICIES` | `LegalPoliciesModal.kt` | Drawer footer links | Closes active page |
| `PRIVACY_POLICY` | `LegalPoliciesModal(PolicyType.PRIVACY)` | Login screen / Drawer link | Closes active page |
| `TERMS_OF_SERVICE` | `LegalPoliciesModal(PolicyType.TERMS)` | Checkout terms link | Closes active page |
| `REFUND_POLICY` | `LegalPoliciesModal(PolicyType.REFUND)` | Footer / Checkout policy link | Closes active page |
| `COOKIE_POLICY` | `LegalPoliciesModal(PolicyType.COOKIE)` | Legal modal tab | Closes active page |
| `BESPOKE_STUDIO` | `BespokeStudioScreen.kt` | Home "Bespoke Customizer" banner | Closes active page |

---

## 3. External Integrations Matrix

| Integration | Library / SDK | Implementation File(s) | Functionality | Auth / Credentials Source |
|-------------|---------------|------------------------|---------------|---------------------------|
| **Firebase Cloud Firestore** | `com.google.firebase:firebase-firestore-ktx` | `FirestoreCatalogService.kt`, `GoodDreamRepository.kt` | Remote product catalog sync, real-time order submission, customer inquiries, newsletter signups. | `google-services.json` |
| **Firebase Cloud Messaging** | `com.google.firebase:firebase-messaging-ktx` | `SanctuaryFirebaseMessagingService.kt`, `NotificationHelper.kt` | Push notification delivery for order status changes, delivery dispatch, and VIP offers. | `google-services.json` |
| **Google Gemini AI** | `com.google.ai.client.generativeai:generativeai:0.9.0` | `GeminiChatService.kt`, `ChatMessage.kt`, `AiChatBotModal.kt` | On-device luxury sleep advisor; answers questions about materials, firmness, and catalog matching. | `GEMINI_API_KEY` via `BuildConfig` |
| **Razorpay Payment Gateway** | `com.razorpay:checkout:1.6.40` | `RazorpayPaymentHelper.kt`, `MainActivity.kt`, `CheckoutScreen.kt` | Credit/Debit Card, UPI, NetBanking checkout modal; handles INR currency transactions. | `RAZORPAY_KEY_ID` via `BuildConfig` |
| **Room Database** | `androidx.room:room-runtime:2.6.1` | `AppDatabase.kt`, `CatalogDaos.kt`, `CatalogEntities.kt` | Local offline SQLite cache for products, categories, cart, wishlist, inquiries, and orders. | Local SQLite file `gooddream_sanctuary.db` |
| **EncryptedSharedPreferences** | `androidx.security:security-crypto:1.1.0-alpha06` | `UserSessionManager.kt` | Stores customer authentication token, email, profile details, and biometric preferences. | Android Keystore master key `_luxury_sanctuary_key_` |
| **Jakarta / JavaMail SMTP** | `com.sun.mail:android-mail:1.6.7`, `android-activation:1.6.7` | `EmailDeliveryService.kt` | Direct SMTP dispatch over TLS (port 587) for transactional emails, OTP verification codes, and invoice delivery. | `SMTP_PASSWORD` & `SMTP_USER` via `BuildConfig` |
| **Coil Image Loading** | `io.coil-kt.coil3:coil-compose:3.0.4` | Used across `HomeScreen.kt`, `ProductDetailScreen.kt`, `ProductListingScreen.kt`, `BespokeStudioScreen.kt` | Asynchronous image loading, disk caching, and crossfade animation for high-res product photography. | Network URLs / Android drawables |
| **Android Print Framework** | Android Framework `android.print.PrintManager` | `InvoicePrinterHelper.kt`, `OrderSuccessScreen.kt` | Generates formatted PDF purchase receipts and sends to Android native print/save spooler. | System Service |

---

## 4. Key Configuration Files & Settings Audit

### 4.1 `app/build.gradle.kts` & Dependencies
- **Application ID**: `com.example.gooddream`
- **SDK Versions**: `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`, `versionCode = 1`, `versionName = "1.0.0"`
- **Compose Compiler**: Kotlin 2.0.21 Compose Compiler plugin enabled.
- **R8 / ProGuard**:
  - `isMinifyEnabled = true` and `isShrinkResources = true` in release build type.
  - ProGuard rules referenced: `proguard-rules.pro`.
- **Signing Configs**:
  - Release signing configured with environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
  - **Fallback**: If environment variables are missing, release signing automatically falls back to root `debug.keystore`.
- **Secrets Gradle Plugin**:
  - `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` enabled.
  - Reads `.env` and exports `GEMINI_API_KEY`, `RAZORPAY_KEY_ID`, `SMTP_USER`, `SMTP_PASSWORD`, `DEFAULT_RECAPTCHA_SITE_KEY` into `BuildConfig`.

### 4.2 `AndroidManifest.xml`
- **Permissions Declared**:
  - `android.permission.INTERNET` (Network requests to Firestore, Razorpay, SMTP, Gemini)
  - `android.permission.ACCESS_NETWORK_STATE` (Connectivity monitoring)
  - `android.permission.POST_NOTIFICATIONS` (Android 13+ push notifications)
  - `android.permission.VIBRATE` (Haptic feedback)
- **Application Configuration**:
  - `android:name=".GoodDreamApplication"`
  - `android:allowBackup="true"` with `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"`.
  - `android:networkSecurityConfig="@xml/network_security_config"`
  - `android:supportsRtl="true"`
- **Activities & Services**:
  - `MainActivity`: `exported="true"`, `launchMode="singleTop"`, with `MAIN` / `LAUNCHER` intent filter.
  - `SanctuaryFirebaseMessagingService`: `exported="false"`, with `com.google.firebase.MESSAGING_EVENT` intent filter.

### 4.3 `network_security_config.xml`
- **Cleartext Traffic**: Explicitly disallowed globally (`cleartextTrafficPermitted="false"`).
- **Trust Anchors**: System CAs trusted (`<certificates src="system" />`).
- **Certificate Pinning**: No `<pin-set>` configured. All TLS trust delegates to Android OS default trust store.

### 4.4 `firestore.rules`
- Security rules deployed for Cloud Firestore:
  - `/products/{productId}`: Public read allowed (`allow read: if true;`), write requires admin claim, **delete allows unauthenticated deletion** (`allow delete: if true;`).
  - `/orders/{orderId}`: Public unauthenticated read and update allowed (`allow read, update: if true;`).
  - `/inquiries/{inquiryId}`: Public unauthenticated read allowed (`allow read: if true;`).
  - `/users/{userId}`: Allows read/write only if `request.auth.uid == userId`.

---

## 5. Discrepancies, Vulnerabilities & Auditor Focal Points

### 5.1 Critical Security Vulnerabilities (For Security Auditor)

1. **Unencrypted SharedPreferences Fallback**:
   - **Location**: `UserSessionManager.kt`, lines 28–30.
   - **Observation**: If Android Keystore throws any exception during `EncryptedSharedPreferences` initialization, it catches the error and silently initializes an unencrypted, plaintext `SharedPreferences` file named `gooddream_user_session`.
   - **Risk**: Auth tokens, email, phone numbers, and profile details can be written unencrypted to disk.

2. **Weak Password Hashing with Static Hardcoded Salt**:
   - **Location**: `UserSessionManager.kt`, lines 207–212.
   - **Observation**: Custom registration hashes passwords using standard single-round `MessageDigest.getInstance("SHA-256")` with a static string salt `"GoodDreamSecuritySalt2026"` concatenated with user email.
   - **Risk**: Vulnerable to fast GPU/rainbow table brute force attacks. Should be upgraded to PBKDF2, bcrypt, or Argon2.

3. **Hardcoded Master Admin Credentials**:
   - **Location**: `GoodDreamViewModel.kt`, companion object lines 1377–1378.
   - **Observation**: 
     ```kotlin
     const val OFFICIAL_ADMIN_EMAIL = "Lakshya190207@gmail.com"
     const val OFFICIAL_ADMIN_PASSWORD = "GoodDream@2026"
     ```
   - **Risk**: Any user entering these hardcoded credentials receives administrative privileges. These credentials can be easily decompiled from the APK.

4. **Live Credentials Committed in `.env`**:
   - **Location**: Root `.env`.
   - **Observation**: Contains active credentials:
     - `SMTP_PASSWORD=lrxhdgzwqrodwmzg` (Gmail App Password for personal account `Lakshya190207@gmail.com`)
     - `GEMINI_API_KEY=AQ.Ab8RN6...` (Live Google AI API key)
     - `RAZORPAY_KEY_ID=rzp_test_51gX7Y8Z9abcde` (Razorpay API Key)
   - **Risk**: Secret leakage in version control.

5. **Direct Client-Side SMTP Socket Connection**:
   - **Location**: `EmailDeliveryService.kt`.
   - **Observation**: Uses JavaMail to connect directly from mobile clients to `smtp.gmail.com:587` using personal Gmail App Password credentials.
   - **Risk**: Direct credential exposure; client devices on cellular or corporate networks frequently block port 587/25, causing silent email dispatch failure; Gmail daily sending quota limits will quickly exhaust.

6. **Permissive Firestore Security Rules**:
   - **Location**: `firestore.rules`.
   - **Observation**:
     - `match /products/{productId} { allow delete: if true; }` — Any client can wipe out the entire product catalog without authentication.
     - `match /inquiries/{inquiryId} { allow read: if true; }` — Any client can read customer contact inquiries, exposing names, emails, and phone numbers.
     - `match /orders/{orderId} { allow read, update: if true; }` — Any client can read and modify other customers' order statuses.

7. **Client-Side Payment Verification (No Server-Side HMAC)**:
   - **Location**: `RazorpayPaymentHelper.kt` & `MainActivity.kt`.
   - **Observation**: Client creates amount directly in JSON and marks order as paid immediately upon receiving `onPaymentSuccess(razorpayPaymentId)`. There is no backend order creation API (`/orders`) or server-side webhook validating the `razorpay_signature` HMAC SHA-256.
   - **Risk**: Client-side payment tampering; orders can be marked paid with arbitrary or spoofed payment IDs.

8. **Debug Keystore Bundled in Repository**:
   - **Location**: Root `debug.keystore` and `app/build.gradle.kts`.
   - **Observation**: `app/build.gradle.kts` release signing config explicitly falls back to signing release bundles with `debug.keystore` if environment variables are unset.

---

### 5.2 UI/UX, Compose & Design System Discrepancies (For UI/UX Auditor)

1. **Incomplete Material 3 Typography Scale**:
   - **Location**: `ui/theme/Type.kt`.
   - **Observation**: Only `bodyLarge` is configured in the `Typography` declaration. Headings (`headlineLarge`, `headlineMedium`, `titleLarge`, `titleMedium`, `labelLarge`) are left unconfigured, resulting in unstyled system fallbacks that deviate from the intended luxury serif editorial typography.

2. **Hardcoded Light Colors in Navigation Components**:
   - **Location**: `ui/components/NavigationComponents.kt`.
   - **Observation**: `GoodDreamBottomNavBar` explicitly sets `containerColor = CardSurfaceWhite` and hardcoded `ForestGreenPrimary` tint for active icons and `TextSecondaryMuted` for inactive icons rather than pulling dynamically from `MaterialTheme.colorScheme`.
   - **Risk**: Breaks dark theme contrast and night mode legibility.

3. **Lack of Jetpack Navigation Component Route Graph**:
   - **Location**: `GoodDreamViewModel.kt` & `MainActivity.kt`.
   - **Observation**: Entire app routing is handled via ViewModel state (`ActivePage` and `MainTab` enums) inside a single `Box` layout.
   - **Risk**: Android OS process death restoration (`SavedStateHandle`) will not automatically reconstruct deep navigation stacks; standard deep-link URI intents (`https://gooddream.com/product/123`) cannot be mapped via manifest intent filters.

4. **Missing URL Deep Links in `AndroidManifest.xml`**:
   - **Location**: `AndroidManifest.xml`.
   - **Observation**: `MainActivity` only handles custom notification extras (`NotificationHelper.EXTRA_ORDER_ID`). There are no `<data android:scheme="gooddream" ...>` or App Links configured for web-to-app routing.

5. **Edge-to-Edge System Bar Inset Handling**:
   - **Location**: `MainActivity.kt` lines 301–303.
   - **Observation**: Overlays with `activePage != ActivePage.NONE` or `selectedProduct != null` zero out `innerPadding` (`PaddingValues(0.dp)`), leaving top app bars and bottom actions in certain modal screens vulnerable to overlapping camera cutouts and navigation pills unless explicitly handled inside every child composable.

---

## 6. Verification & Compilation Method

To verify the files, types, and build status described in this inventory:
- **Gradle Compilation**: Run `./gradlew assembleDebug` or `./gradlew test` to execute the existing 7 unit test suites.
- **Inspect Kotlin Sources**: Verify all 52 source files in `app/src/main/java/com/example/`.
- **Inspect Security Settings**: Review `firestore.rules`, `.env`, and `network_security_config.xml`.
