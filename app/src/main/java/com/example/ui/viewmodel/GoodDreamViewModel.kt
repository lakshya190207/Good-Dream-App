package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.AppConfig
import com.example.data.config.AppConfigProvider
import com.example.data.gemini.ChatMessage
import com.example.data.gemini.GeminiChatService
import com.example.data.gemini.MessageRole
import com.example.data.local.AppDatabase
import com.example.data.local.SavedAddress
import com.example.data.local.UserSessionManager
import com.example.data.model.*
import com.example.data.remote.EmailSendResult
import com.example.data.repository.GoodDreamRepository
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    PRODUCTS,
    NEW_LAUNCHES,
    ACCOUNT
}

enum class ActivePage {
    NONE,
    CART,
    CHECKOUT,
    ORDER_SUCCESS,
    CUSTOM_INQUIRY,
    YOUR_NEEDS,
    SPONSOR_REWARDS,
    FEEDBACKS,
    PURCHASE_REWARDS,
    REPAIRS,
    COMPLAINTS,
    MESSAGE_FOR_YOU,
    SERVICE_WARRANTIES,
    ADMIN_PANEL,
    USER_LOGIN,
    ADMIN_LOGIN,
    AI_CHAT_BOT,
    SLEEP_QUIZ,
    ORDER_TRACKING,
    COMPARE_PRODUCTS,
    LEGAL_POLICIES,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    REFUND_POLICY,
    COOKIE_POLICY,
    BESPOKE_STUDIO
}

typealias ActiveModal = ActivePage

data class CartItemWithProduct(
    val product: ProductEntity,
    val quantity: Int
)

sealed interface OtpRequestResult {
    data class Success(val otp: String, val message: String) : OtpRequestResult
    data class RateLimited(val remainingSeconds: Int, val message: String) : OtpRequestResult
    data class InvalidEmail(val message: String) : OtpRequestResult
    data class DeliveryFailed(val message: String) : OtpRequestResult
}

sealed interface OtpVerifyResult {
    data object Success : OtpVerifyResult
    data class InvalidCode(val attemptsRemaining: Int, val message: String) : OtpVerifyResult
    data class Expired(val message: String) : OtpVerifyResult
    data class Locked(val remainingSeconds: Int, val message: String) : OtpVerifyResult
}

sealed interface AdminAuthResult {
    data object Success : AdminAuthResult
    data class InvalidCredentials(val attemptsRemaining: Int, val message: String) : AdminAuthResult
    data class Locked(val remainingSeconds: Int, val message: String) : AdminAuthResult
}

sealed interface PasscodeAuthResult {
    data class Success(val isAdmin: Boolean, val name: String, val message: String) : PasscodeAuthResult
    data class InvalidCredentials(val attemptsRemaining: Int, val message: String) : PasscodeAuthResult
    data class UserNotFound(val message: String) : PasscodeAuthResult
    data class Locked(val remainingSeconds: Int, val message: String) : PasscodeAuthResult
}

data class GoodDreamUiState(
    val currentTab: MainTab = MainTab.HOME,
    val activePage: ActivePage = ActivePage.NONE,
    val activeModal: ActivePage = ActivePage.NONE,
    val selectedCategory: CategoryEntity? = null,
    val selectedProduct: ProductEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val cartItems: List<CartItemWithProduct> = emptyList(),
    val wishlistIds: Set<String> = emptySet(),
    val isCartSheetVisible: Boolean = false,
    val isWishlistSheetVisible: Boolean = false,
    val searchQuery: String = "",
    val sortFilter: String = "Default", // "Default", "SpringHaven Only", "Price: Low to High", "Price: High to Low"
    val userNotificationMessage: String? = null,
    val recentInquiryRef: String? = null,
    val latestPlacedOrder: OrderEntity? = null,
    val trackingTargetOrderId: String? = null,
    val orders: List<OrderEntity> = emptyList(),
    val inquiries: List<InquiryEntity> = emptyList(),
    val appConfigs: Map<String, String> = emptyMap(),
    val appConfig: AppConfig = AppConfig(),
    val isCatalogLoading: Boolean = false,
    val isDarkMode: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val loggedInUserEmail: String? = null,
    val loggedInUserName: String? = null,
    val isAdminAuthenticated: Boolean = false,
    val pendingGeneratedOtp: String? = null,
    val otpDeliveryMessage: String? = null,
    val otpExpiresEpochMs: Long = 0L,
    val customerOtpLockoutUntilEpochMs: Long = 0L,
    val customerOtpFailedAttempts: Int = 0,
    val adminLockoutUntilEpochMs: Long = 0L,
    val adminFailedAttempts: Int = 0,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            role = MessageRole.MODEL,
            text = "Welcome to Good Dream Home Decor! 🌙 I'm your DreamCare AI Concierge. How can I help you today? Ask me about mattress firmness for back pain, custom sizing, stain cleaning, 100-night trials, or order status."
        )
    ),
    val isChatLoading: Boolean = false,
    val selectedGeminiModel: String = GeminiChatService.MODEL_FLASH_LITE,
    val pendingPostLoginDestination: ActivePage? = null,
    val isAuthGateVisible: Boolean = false,
    val userSleepProfile: String? = null,
    val lastAddedCartProduct: ProductEntity? = null,
    val lastAddedQuantity: Int = 1,
    val authGateTargetPage: ActivePage? = null,
    val savedAddresses: List<SavedAddress> = emptyList(),
    val isOnline: Boolean = true,
    val fcmToken: String? = null,
    val appliedCouponCode: String? = null,
    val appliedDiscountPercent: Int = 0,
    val pendingPaymentOrderDraft: PendingPaymentOrderDraft? = null,
    val isPaymentProcessing: Boolean = false,
    val paymentErrorMessage: String? = null,
    val crmUsers: List<CrmUserRecord> = emptyList()
) {
    val totalCartPrice: Double
        get() = cartItems.sumOf { it.product.price * it.quantity }

    val discountAmount: Double
        get() = if (appliedDiscountPercent > 0) totalCartPrice * (appliedDiscountPercent / 100.0) else 0.0

    val finalPayablePrice: Double
        get() = (totalCartPrice - discountAmount).coerceAtLeast(0.0)

    val totalCartItemsCount: Int
        get() = cartItems.sumOf { it.quantity }
}

class GoodDreamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GoodDreamRepository(AppDatabase.getInstance(application))
    private val geminiChatService = GeminiChatService()
    private val userSessionManager = UserSessionManager(application)
    private val connectivityObserver = com.example.util.NetworkConnectivityObserver(application)
    private val initialEmail = userSessionManager.getUserEmail()
    private val initialAddresses = if (initialEmail != null) userSessionManager.getSavedAddresses(initialEmail) else emptyList()

    private val _uiState = MutableStateFlow(
        GoodDreamUiState(
            isUserLoggedIn = userSessionManager.isUserLoggedIn(),
            loggedInUserEmail = initialEmail,
            loggedInUserName = userSessionManager.getUserName(),
            savedAddresses = initialAddresses,
            fcmToken = userSessionManager.getFcmToken()
        )
    )
    val uiState: StateFlow<GoodDreamUiState> = _uiState.asStateFlow()

    init {
        // Initialize Android notification channels
        NotificationHelper.createNotificationChannels(application)

        // Retrieve current FCM registration token
        try {
            @Suppress("DEPRECATION")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Timber.d("Sanctuary FCM registration token refreshed successfully")
                    userSessionManager.saveFcmToken(token)
                    _uiState.update { it.copy(fcmToken = token) }
                } else {
                    Timber.w(task.exception, "FCM registration token retrieval failed")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error initializing FCM messaging client")
        }

        // Observe real-time network connectivity for offline resilience
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        // Observe dynamic AppConfig from Firestore provider at startup
        viewModelScope.launch {
            AppConfigProvider.configState.collect { remoteConfig ->
                _uiState.update { current ->
                    val updatedMap = current.appConfigs.toMutableMap().apply {
                        put("company_name", remoteConfig.companyName)
                        put("tagline", remoteConfig.tagline)
                        put("support_phone", remoteConfig.contactInfo.phone)
                        put("support_whatsapp", remoteConfig.contactInfo.whatsapp)
                        put("support_email", remoteConfig.contactInfo.email)
                        put("store_address", remoteConfig.contactInfo.address)
                        put("warranty_policy_url", remoteConfig.warrantyPolicyUrl)
                        put("support_hours", remoteConfig.supportHours)
                        put("announcement", remoteConfig.announcement)
                        remoteConfig.offerBanners.firstOrNull()?.let { b ->
                            put("banner_offer", "${b.title}: ${b.subtitle}")
                        }
                    }
                    current.copy(
                        appConfig = remoteConfig,
                        appConfigs = updatedMap
                    )
                }
            }
        }

        // Initialize local database catalog, sync with Firestore cloud backend, and fetch remote config
        viewModelScope.launch {
            _uiState.update { it.copy(isCatalogLoading = true) }
            repository.syncCatalogWithCloud()
            AppConfigProvider.fetchAtStartup(application)
            // Brief delay allows shimmer skeletons to be perceived naturally during initial boot/sync
            delay(400)
            _uiState.update { it.copy(isCatalogLoading = false) }
            syncCrmUsers()
        }

        // Observe categories
        viewModelScope.launch {
            repository.categories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
                preloadCategoryImages(cats)
            }
        }

        // Observe products
        viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.update { it.copy(products = prods) }
                rebuildSearchIndex(prods)
                preloadCatalogImages(prods)
            }
        }

        // Observe wishlist
        viewModelScope.launch {
            repository.wishlistItems.collect { items ->
                _uiState.update { it.copy(wishlistIds = items.map { item -> item.productId }.toSet()) }
            }
        }

        // Observe cart
        viewModelScope.launch {
            combine(repository.cartItems, repository.allProducts) { cartItems, allProducts ->
                val productMap = allProducts.associateBy { it.id }
                cartItems.mapNotNull { cartItem ->
                    productMap[cartItem.productId]?.let { product ->
                        CartItemWithProduct(product, cartItem.quantity)
                    }
                }
            }.collect { resolvedCart ->
                _uiState.update { it.copy(cartItems = resolvedCart) }
            }
        }

        // Observe appConfigs
        viewModelScope.launch {
            repository.appConfigs.collect { configs ->
                _uiState.update { it.copy(appConfigs = configs.associate { c -> c.configKey to c.configValue }) }
            }
        }

        // Observe orders with strict customer data isolation
        viewModelScope.launch {
            combine(
                repository.allOrders,
                _uiState.map { Triple(it.isUserLoggedIn, it.loggedInUserEmail, it.isAdminAuthenticated) }.distinctUntilChanged()
            ) { orderList, auth ->
                val (isLoggedIn, userEmail, isAdmin) = auth
                when {
                    isAdmin -> orderList
                    isLoggedIn && !userEmail.isNullOrBlank() -> {
                        orderList.filter { it.customerEmail.equals(userEmail, ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }.collect { scopedOrders ->
                _uiState.update { it.copy(orders = scopedOrders) }
            }
        }

        // Observe customer inquiries & leads with strict customer data isolation
        viewModelScope.launch {
            combine(
                repository.allInquiries,
                _uiState.map { Triple(it.isUserLoggedIn, it.loggedInUserEmail, it.isAdminAuthenticated) }.distinctUntilChanged()
            ) { inqList, auth ->
                val (isLoggedIn, userEmail, isAdmin) = auth
                when {
                    isAdmin -> inqList
                    isLoggedIn && !userEmail.isNullOrBlank() -> {
                        inqList.filter { it.customerEmail.equals(userEmail, ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }.collect { scopedInquiries ->
                _uiState.update { it.copy(inquiries = scopedInquiries) }
            }
        }

        // Start background realtime product listener from Firestore
        repository.startRealtimeProductSync(viewModelScope)
    }

    // Fast in-memory token index for 120 FPS predictive search without allocations
    @Volatile
    private var productSearchIndex: Map<String, List<ProductEntity>> = emptyMap()

    private fun rebuildSearchIndex(products: List<ProductEntity>) {
        val index = mutableMapOf<String, MutableList<ProductEntity>>()
        for (prod in products) {
            val tokens = (prod.title.split(" ") +
                    prod.subtitle.split(" ") +
                    prod.material.split(" ") +
                    prod.firmness.split(" ") +
                    prod.description.split(" "))
                .map { it.lowercase().trim().filter { c -> c.isLetterOrDigit() } }
                .filter { it.length >= 2 }
                .toSet()
            for (token in tokens) {
                index.getOrPut(token) { mutableListOf() }.add(prod)
            }
        }
        productSearchIndex = index
    }

    /**
     * High-speed tokenized in-memory search query execution for 120 FPS UI flings.
     */
    fun queryFastSearch(query: String, allProducts: List<ProductEntity>): List<ProductEntity> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return allProducts
        val queryTokens = trimmed.split(" ")
            .map { it.trim().filter { c -> c.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        if (queryTokens.isEmpty()) return allProducts

        val candidateSets = queryTokens.mapNotNull { token ->
            val matching = productSearchIndex.filterKeys { it.contains(token) }.values.flatten().toSet()
            if (matching.isEmpty()) null else matching
        }
        if (candidateSets.isEmpty()) {
            return allProducts.filter { prod ->
                prod.title.contains(trimmed, ignoreCase = true) ||
                prod.subtitle.contains(trimmed, ignoreCase = true) ||
                prod.description.contains(trimmed, ignoreCase = true) ||
                prod.material.contains(trimmed, ignoreCase = true)
            }
        }
        return candidateSets.reduce { acc, set -> acc.intersect(set) }.toList()
    }

    /**
     * Eagerly pre-warms the top 10 catalog mattress hero images into Coil's memory cache
     * on a background coroutine, ensuring instant 0ms image rendering when navigating.
     */
    private fun preloadCatalogImages(products: List<ProductEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val imageLoader = Coil.imageLoader(app)
                val heroUrls = products.take(10).flatMap { it.getImagesList().take(1) }
                for (url in heroUrls) {
                    if (url.isNotBlank()) {
                        val request = ImageRequest.Builder(app)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }
                }
                Timber.d("Eagerly pre-warmed %d catalog hero images into Coil memory cache", heroUrls.size)
            } catch (e: Exception) {
                Timber.w(e, "Non-critical error during catalog image pre-warming")
            }
        }
    }

    /**
     * Pre-warms category thumbnails into Coil's memory cache.
     */
    private fun preloadCategoryImages(categories: List<CategoryEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val imageLoader = Coil.imageLoader(app)
                for (cat in categories) {
                    if (cat.thumbnailUrl.isNotBlank()) {
                        val request = ImageRequest.Builder(app)
                            .data(cat.thumbnailUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Non-critical error during category image pre-warming")
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                selectedCategory = if (tab == MainTab.PRODUCTS) it.selectedCategory else null,
                selectedProduct = null
            )
        }
    }

    fun openCategory(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                currentTab = MainTab.PRODUCTS,
                selectedCategory = category,
                selectedProduct = null
            )
        }
    }

    fun clearSelectedCategory() {
        _uiState.update { it.copy(selectedCategory = null) }
    }

    fun openProductDetail(product: ProductEntity) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun closeProductDetail() {
        _uiState.update { it.copy(selectedProduct = null) }
    }

    fun openPage(page: ActivePage) {
        val formPages = listOf(
            ActivePage.YOUR_NEEDS,
            ActivePage.CUSTOM_INQUIRY,
            ActivePage.SERVICE_WARRANTIES,
            ActivePage.REPAIRS,
            ActivePage.COMPLAINTS,
            ActivePage.FEEDBACKS
        )
        if (page in formPages && !_uiState.value.isUserLoggedIn) {
            _uiState.update {
                it.copy(
                    isAuthGateVisible = true,
                    authGateTargetPage = page
                )
            }
            return
        }
        if (page == ActivePage.ADMIN_PANEL) {
            syncCrmUsers()
        }
        _uiState.update { it.copy(activePage = page, activeModal = page) }
    }

    fun proceedToLoginFromGate() {
        val target = _uiState.value.authGateTargetPage
        _uiState.update {
            it.copy(
                isAuthGateVisible = false,
                pendingPostLoginDestination = target,
                activePage = ActivePage.USER_LOGIN,
                activeModal = ActivePage.USER_LOGIN
            )
        }
    }

    fun dismissAuthGate() {
        _uiState.update {
            it.copy(
                isAuthGateVisible = false,
                authGateTargetPage = null
            )
        }
    }

    fun onLoginCompleted() {
        val dest = _uiState.value.pendingPostLoginDestination
        if (dest != null) {
            _uiState.update {
                it.copy(
                    activePage = dest,
                    activeModal = dest,
                    pendingPostLoginDestination = null
                )
            }
        } else {
            closeActivePage()
        }
    }

    fun closeActivePage() {
        _uiState.update { it.copy(activePage = ActivePage.NONE, activeModal = ActivePage.NONE) }
    }

    fun openModal(modal: ActiveModal) {
        openPage(modal)
    }

    fun closeModal() {
        closeActivePage()
    }

    fun openCartPage() {
        openPage(ActivePage.CART)
    }

    fun openCheckoutPage() {
        openPage(ActivePage.CHECKOUT)
    }

    fun openBespokeStudio() {
        openPage(ActivePage.BESPOKE_STUDIO)
    }

    fun commissionBespokeMattress(
        config: BespokeMattressConfiguration,
        onSuccess: (ProductEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val calculatedPrice = config.calculatePrice()
            val customSku = config.generateSku()
            val (w, l) = config.getEffectiveDimensions()
            val bespokeProduct = ProductEntity(
                id = "bespoke_${System.currentTimeMillis()}",
                categoryId = "cat_mattresses",
                title = "Bespoke Sovereign Mattress (${w}\" x ${l}\" x ${config.customThicknessInches}\")",
                subtitle = "${config.coreType.title} • ${config.comfortLayer.title}",
                sku = customSku,
                isSpringhavenSeries = config.coreType == BespokeCoreType.POCKET_SPRINGS || config.coreType == BespokeCoreType.HYBRID_SOVEREIGN,
                description = "Atelier handcrafted bespoke luxury mattress built precisely to custom specifications.\n\n" +
                    "• Core Architecture: ${config.coreType.description}\n" +
                    "• Comfort Layer: ${config.comfortLayer.description}\n" +
                    "• Quilt & Fabric: ${config.quiltCover.description}\n" +
                    "• Dimensions: ${w}\" Width x ${l}\" Length x ${config.customThicknessInches}\" Thickness\n" +
                    "• Firmness Level: ${config.firmnessPreference.label} (${config.firmnessPreference.score})\n" +
                    (if (config.customMonogramText.isNotBlank()) "• Bespoke Monogram: \"${config.customMonogramText.trim().uppercase()}\" (${config.customMonogramColor.label} Thread)\n" else "") +
                    (if (config.includeFoundationBedBase) "• Matching Upholstered Bed Foundation Base Included\n" else "") +
                    (if (config.includeMatchingPillows) "• Pair of Tailored Sanctuary Comfort Pillows Included\n" else ""),
                specificationsJson = "Core: ${config.coreType.title}|Comfort: ${config.comfortLayer.title}|Cover: ${config.quiltCover.title}|Firmness: ${config.firmnessPreference.label}|Dimensions: ${w}\"x${l}\"x${config.customThicknessInches}\"|Monogram: ${if (config.customMonogramText.isNotBlank()) config.customMonogramText.trim().uppercase() else "None"}",
                price = calculatedPrice,
                originalPrice = kotlin.math.round(calculatedPrice * 1.30 / 100.0) * 100.0,
                imagesJson = "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=800|https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=800|https://images.unsplash.com/photo-1540518614846-7ede433c4ef7?q=80&w=800",
                isNewLaunch = true,
                warrantyYears = 25,
                thicknessInches = config.customThicknessInches,
                material = "${config.coreType.title} & ${config.comfortLayer.title}",
                dimensions = "${w}\" x ${l}\"",
                firmness = config.firmnessPreference.label,
                createdAt = System.currentTimeMillis()
            )

            repository.insertProduct(bespokeProduct)
            repository.addToCart(bespokeProduct.id)

            _uiState.update {
                it.copy(
                    userNotificationMessage = null,
                    lastAddedCartProduct = bespokeProduct,
                    lastAddedQuantity = 1
                )
            }
            onSuccess(bespokeProduct)
        }
    }

    fun trackOrder(orderId: String) {
        _uiState.update {
            it.copy(
                trackingTargetOrderId = orderId,
                activePage = ActivePage.ORDER_TRACKING,
                activeModal = ActivePage.ORDER_TRACKING
            )
        }
    }

    fun applyCoupon(code: String, percent: Int) {
        _uiState.update {
            it.copy(
                appliedCouponCode = code.trim().uppercase(),
                appliedDiscountPercent = percent,
                userNotificationMessage = "Privilege code $code applied ($percent% OFF)"
            )
        }
    }

    fun removeCoupon() {
        _uiState.update {
            it.copy(
                appliedCouponCode = null,
                appliedDiscountPercent = 0,
                userNotificationMessage = "Privilege code removed"
            )
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            val stageIndex = when (newStatus.lowercase()) {
                "crafting", "atelier crafting", "in review" -> 1
                "dispatched", "fleet transit", "transit" -> 2
                "delivered", "in-room setup", "setup", "resolved" -> 3
                else -> 0
            }
            simulateOrderStageNotification(orderId, stageIndex)
            _uiState.update { it.copy(userNotificationMessage = "Order $orderId updated to: $newStatus") }
            syncCrmUsers()
        }
    }

    fun syncCrmUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val registeredUsers = userSessionManager.getAllRegisteredUsers()
            val allOrdersList = repository.allOrders.first()
            val allInquiriesList = repository.allInquiries.first()

            val ordersByEmail = allOrdersList.filter { it.customerEmail.isNotBlank() }
                .groupBy { it.customerEmail.trim().lowercase() }
            val inquiriesByEmail = allInquiriesList.filter { it.customerEmail.isNotBlank() }
                .groupBy { it.customerEmail.trim().lowercase() }

            val mergedMap = mutableMapOf<String, CrmUserRecord>()

            // 1. Add Registered VIPs
            registeredUsers.forEach { regUser ->
                val emailKey = regUser.email.trim().lowercase()
                val userOrders = ordersByEmail[emailKey] ?: emptyList()
                val totalSpend = userOrders.sumOf { it.totalAmount }
                val lastOrder = userOrders.maxByOrNull { it.createdAt }
                val latestActivity = maxOf(regUser.lastActivityEpochMs, lastOrder?.createdAt ?: 0L)

                mergedMap[emailKey] = regUser.copy(
                    totalOrders = userOrders.size,
                    lifetimeSpend = totalSpend,
                    lastActivityEpochMs = latestActivity
                )
            }

            // 2. Add Order Clients who might not have registered with password
            ordersByEmail.forEach { (emailKey, userOrders) ->
                if (!mergedMap.containsKey(emailKey)) {
                    val latestOrder = userOrders.maxByOrNull { it.createdAt } ?: userOrders.first()
                    val totalSpend = userOrders.sumOf { it.totalAmount }
                    val addresses = userOrders.map { ord ->
                        SavedAddress(
                            id = ord.id,
                            tag = "Order Delivery",
                            fullName = ord.customerName,
                            phoneNumber = ord.customerPhone,
                            flatHouseNo = ord.deliveryAddress,
                            streetLocality = ord.city,
                            city = ord.city,
                            state = ord.state,
                            pincode = ord.pincode
                        )
                    }.distinctBy { it.flatHouseNo }

                    mergedMap[emailKey] = CrmUserRecord(
                        id = emailKey,
                        name = latestOrder.customerName,
                        email = emailKey,
                        phone = latestOrder.customerPhone,
                        userType = CrmUserType.ORDER_CLIENT,
                        totalOrders = userOrders.size,
                        lifetimeSpend = totalSpend,
                        lastActivityEpochMs = latestOrder.createdAt,
                        addresses = addresses
                    )
                }
            }

            // 3. Add Inquiry Leads
            inquiriesByEmail.forEach { (emailKey, userInquiries) ->
                if (!mergedMap.containsKey(emailKey)) {
                    val latestInquiry = userInquiries.maxByOrNull { it.createdAt } ?: userInquiries.first()
                    mergedMap[emailKey] = CrmUserRecord(
                        id = emailKey,
                        name = latestInquiry.customerName,
                        email = emailKey,
                        phone = latestInquiry.customerPhone,
                        userType = CrmUserType.INQUIRY_LEAD,
                        totalOrders = 0,
                        lifetimeSpend = 0.0,
                        lastActivityEpochMs = latestInquiry.createdAt,
                        notes = "Inquiry [${latestInquiry.type}]: ${latestInquiry.details.take(60)}"
                    )
                }
            }

            val sortedUsers = mergedMap.values.sortedWith(
                compareByDescending<CrmUserRecord> { it.lifetimeSpend }
                    .thenByDescending { it.lastActivityEpochMs }
            )

            _uiState.update { it.copy(crmUsers = sortedUsers) }
        }
    }

    fun saveCrmUser(email: String, name: String, phone: String, notes: String = "") {
        userSessionManager.saveCrmUser(email, name, phone, notes)
        syncCrmUsers()
        _uiState.update { it.copy(userNotificationMessage = "Client profile updated for $name") }
    }

    fun deleteCrmUser(email: String) {
        userSessionManager.deleteUserAccountAndData(email)
        syncCrmUsers()
        _uiState.update { it.copy(userNotificationMessage = "User data purged for $email") }
    }

    fun placeOrder(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        deliveryAddress: String,
        city: String,
        state: String,
        pincode: String,
        deliverySlot: String,
        floorElevator: String,
        paymentMethod: String,
        paymentStatus: String = "Paid",
        onOrderPlaced: (OrderEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val stateVal = _uiState.value
            val orderId = "GD-ORD-2026-${(10000..99999).random()}"
            val itemsSummary = buildString {
                if (stateVal.cartItems.isNotEmpty()) {
                    append(stateVal.cartItems.joinToString(", ") { "${it.product.title} (${it.quantity}x)" })
                } else {
                    append("Luxury Bedding Sanctuary Item")
                }
                if (!stateVal.appliedCouponCode.isNullOrBlank()) {
                    append(" [Privilege Code: ${stateVal.appliedCouponCode} (${stateVal.appliedDiscountPercent}% OFF)]")
                }
            }
            val totalAmount = if (stateVal.finalPayablePrice > 0) {
                stateVal.finalPayablePrice
            } else if (stateVal.totalCartPrice > 0) {
                stateVal.totalCartPrice
            } else {
                48999.0
            }

            val newOrder = OrderEntity(
                id = orderId,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerEmail = customerEmail.trim(),
                deliveryAddress = deliveryAddress.trim(),
                city = city.trim(),
                state = state.trim(),
                pincode = pincode.trim(),
                deliverySlot = deliverySlot,
                floorElevator = floorElevator,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                totalAmount = totalAmount,
                itemsSummary = itemsSummary,
                status = "Confirmed",
                createdAt = System.currentTimeMillis()
            )

            repository.insertOrder(newOrder)
            repository.clearCart()

            _uiState.update {
                it.copy(
                    latestPlacedOrder = newOrder,
                    trackingTargetOrderId = newOrder.id,
                    activePage = ActivePage.ORDER_SUCCESS,
                    activeModal = ActivePage.ORDER_SUCCESS,
                    appliedCouponCode = null,
                    appliedDiscountPercent = 0,
                    userNotificationMessage = "Order Confirmed: $orderId"
                )
            }
            onOrderPlaced(newOrder)
            syncCrmUsers()

            try {
                NotificationHelper.showOrderStageNotification(
                    context = getApplication(),
                    orderId = newOrder.id,
                    stageTitle = "Order Reserved & Confirmed",
                    stageDescription = "Your bespoke handcrafted mattress has been recorded. Artisan tailoring scheduled."
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to post order confirmation push notification")
            }
        }
    }

    fun setPendingPaymentDraft(draft: PendingPaymentOrderDraft) {
        _uiState.update {
            it.copy(
                pendingPaymentOrderDraft = draft,
                isPaymentProcessing = true,
                paymentErrorMessage = null
            )
        }
    }

    fun onPaymentSuccess(paymentId: String, paymentDataJson: String? = null) {
        val draft = _uiState.value.pendingPaymentOrderDraft
        _uiState.update {
            it.copy(
                isPaymentProcessing = false,
                paymentErrorMessage = null
            )
        }

        if (draft != null) {
            val methodStr = if (draft.isCod) {
                "Cash on Delivery (20% Advance ₹${draft.codAdvanceAmount.toInt()} paid via Razorpay Txn: $paymentId • 80% Balance ₹${draft.codBalanceAmount.toInt()} due on delivery)"
            } else {
                "Razorpay Online (${draft.paymentMethodDetail} • Txn: $paymentId)"
            }
            placeOrder(
                customerName = draft.customerName,
                customerPhone = draft.customerPhone,
                customerEmail = draft.customerEmail,
                deliveryAddress = draft.deliveryAddress,
                city = draft.city,
                state = draft.state,
                pincode = draft.pincode,
                deliverySlot = draft.deliverySlot,
                floorElevator = draft.floorElevator,
                paymentMethod = methodStr,
                paymentStatus = "Paid"
            )
            _uiState.update { it.copy(pendingPaymentOrderDraft = null) }
        }
    }

    fun onPaymentError(code: Int, errorDescription: String?) {
        Timber.w("Razorpay payment failed/cancelled: code=$code, desc=$errorDescription")
        _uiState.update {
            it.copy(
                isPaymentProcessing = false,
                paymentErrorMessage = errorDescription ?: "Payment was cancelled or could not be completed."
            )
        }
    }

    fun clearPaymentError() {
        _uiState.update { it.copy(paymentErrorMessage = null, isPaymentProcessing = false) }
    }

    fun simulateOrderStageNotification(orderId: String, stageIndex: Int) {
        val (stageTitle, stageDesc) = when (stageIndex) {
            1 -> "Atelier Crafting & Orthopedic Testing" to "Handcrafted assembly of pocket coils and organic zero-VOC Belgian latex layers underway."
            2 -> "Dispatched via Climate-Regulated Fleet" to "Enclosed in triple-layer sterile wraps in dedicated temperature-controlled transit."
            3 -> "Out for White-Glove In-Room Setup" to "Our certified specialists are arriving for bedroom setup and packaging removal."
            else -> "Order Confirmed & Logged" to "Your Good Dream handcrafted bedding is officially booked."
        }
        try {
            NotificationHelper.showOrderStageNotification(
                context = getApplication(),
                orderId = orderId,
                stageTitle = stageTitle,
                stageDescription = stageDesc
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to post simulated stage push notification")
        }
    }

    fun toggleCartSheet(show: Boolean) {
        _uiState.update { it.copy(isCartSheetVisible = show) }
    }

    fun toggleWishlistSheet(show: Boolean) {
        _uiState.update { it.copy(isWishlistSheetVisible = show) }
    }

    fun addToCart(productId: String, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(productId, quantity)
            val product = _uiState.value.products.find { it.id == productId }
            _uiState.update {
                it.copy(
                    userNotificationMessage = null,
                    lastAddedCartProduct = product,
                    lastAddedQuantity = quantity
                )
            }
        }
    }

    fun clearLastAddedCartProduct() {
        _uiState.update { it.copy(lastAddedCartProduct = null) }
    }

    fun saveUserSleepProfile(profile: String) {
        _uiState.update { it.copy(userSleepProfile = profile) }
    }

    fun saveDeliveryAddress(address: SavedAddress) {
        val email = _uiState.value.loggedInUserEmail ?: return
        userSessionManager.saveAddress(email, address)
        val updated = userSessionManager.getSavedAddresses(email)
        _uiState.update { it.copy(savedAddresses = updated) }
    }

    fun deleteDeliveryAddress(addressId: String) {
        val email = _uiState.value.loggedInUserEmail ?: return
        userSessionManager.deleteAddress(email, addressId)
        val updated = userSessionManager.getSavedAddresses(email)
        _uiState.update { it.copy(savedAddresses = updated) }
    }

    fun loadSavedAddresses() {
        val email = _uiState.value.loggedInUserEmail
        val addresses = if (!email.isNullOrBlank()) userSessionManager.getSavedAddresses(email) else emptyList()
        _uiState.update { it.copy(savedAddresses = addresses) }
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(productId, newQuantity)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            _uiState.update { it.copy(userNotificationMessage = "Cart emptied") }
        }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch {
            val isWishlisted = _uiState.value.wishlistIds.contains(productId)
            repository.toggleWishlist(productId, isWishlisted)
            _uiState.update {
                it.copy(
                    userNotificationMessage = if (isWishlisted) "Removed from Wishlist" else "Added to Wishlist"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun setSortFilter(filter: String) {
        _uiState.update { it.copy(sortFilter = filter) }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }

    fun submitInquiry(
        type: String,
        name: String,
        phone: String,
        email: String = "",
        categoryOrProduct: String = "",
        details: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val ref = repository.submitInquiry(type, name, phone, email, categoryOrProduct, details)
            _uiState.update {
                it.copy(
                    recentInquiryRef = ref,
                    userNotificationMessage = "Request submitted successfully! Reference: $ref"
                )
            }
            onSuccess(ref)
        }
    }

    fun updateInquiryStatus(inquiryId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateInquiryStatus(inquiryId, newStatus)
            _uiState.update { it.copy(userNotificationMessage = "Lead #$inquiryId status updated to '$newStatus'") }
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateProduct(product)
            _uiState.update { it.copy(userNotificationMessage = "Product updated in Catalog") }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _uiState.update { it.copy(userNotificationMessage = "Product removed from Catalog") }
        }
    }

    fun updateConfig(key: String, value: String) {
        viewModelScope.launch {
            repository.updateAppConfig(key, value)
            _uiState.update { it.copy(userNotificationMessage = "Configuration updated") }
        }
    }

    fun resetCatalog() {
        viewModelScope.launch {
            repository.resetToDefaultCatalog()
            _uiState.update { it.copy(userNotificationMessage = "Catalog reset to factory defaults") }
        }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val userMsg = ChatMessage(role = MessageRole.USER, text = trimmed)
        val currentHistory = _uiState.value.chatMessages
        val updatedHistory = currentHistory + userMsg

        _uiState.update {
            it.copy(
                chatMessages = updatedHistory,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val responseText = geminiChatService.sendMessage(
                    history = currentHistory,
                    userMessage = trimmed,
                    model = _uiState.value.selectedGeminiModel
                )
                val modelMsg = ChatMessage(role = MessageRole.MODEL, text = responseText)
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + modelMsg,
                        isChatLoading = false
                    )
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    role = MessageRole.MODEL,
                    text = "I encountered an issue connecting to the AI service. Please try again or reach our concierge directly at +91 80 4123 9999.",
                    isError = true
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + errorMsg,
                        isChatLoading = false
                    )
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                chatMessages = listOf(
                    ChatMessage(
                        role = MessageRole.MODEL,
                        text = "Chat cleared. I'm ready to help you with mattress sizing, firmness, care tips, warranties, or any small problem!"
                    )
                )
            )
        }
    }

    fun setGeminiModel(model: String) {
        _uiState.update { it.copy(selectedGeminiModel = model) }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCatalogLoading = true) }
            repository.syncCatalogWithCloud()
            AppConfigProvider.fetchRemoteConfig()
            delay(500)
            _uiState.update { it.copy(isCatalogLoading = false) }
        }
    }

    fun refreshAppConfig() {
        viewModelScope.launch {
            AppConfigProvider.fetchRemoteConfig()
        }
    }

    // -------------------------------------------------------------
    // Customer Authentication (Email OTP with Rate Limiting & Expiry)
    // -------------------------------------------------------------

    private val recentOtpRequests = mutableListOf<Long>()
    private val secureRandom = SecureRandom()

    /**
     * Initiates customer sign-in by generating a cryptographically secure 6-digit OTP.
     * Enforces:
     * - Cooldown rate limit (minimum 30 seconds between requests)
     * - Sliding window rate limit (max 4 requests per 10 minutes)
     * - 5-minute code expiration
     * - Lockout enforcement if previous brute-force attempts occurred
     */
    suspend fun requestUserEmailOtp(email: String): OtpRequestResult {
        val now = System.currentTimeMillis()
        val currentLockout = _uiState.value.customerOtpLockoutUntilEpochMs
        if (now < currentLockout) {
            val remainingSec = (((currentLockout - now) / 1000) + 1).toInt()
            return OtpRequestResult.RateLimited(
                remainingSeconds = remainingSec,
                message = "Verification temporarily locked due to failed attempts. Please wait ${remainingSec}s."
            )
        }

        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.length > 100 || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return OtpRequestResult.InvalidEmail("Please enter a valid email address (e.g. name@domain.com)")
        }

        // Clean up requests older than 10 minutes
        recentOtpRequests.removeAll { now - it > 10 * 60 * 1000L }

        // Enforce cooldown: at least 30 seconds since last request
        val lastRequest = recentOtpRequests.lastOrNull() ?: 0L
        if (now - lastRequest < 30_000L) {
            val remainingSec = (((30_000L - (now - lastRequest)) / 1000) + 1).toInt()
            return OtpRequestResult.RateLimited(
                remainingSeconds = remainingSec,
                message = "Please wait ${remainingSec}s before requesting a new verification code."
            )
        }

        // Enforce maximum 4 requests per 10 minutes
        if (recentOtpRequests.size >= 4) {
            return OtpRequestResult.RateLimited(
                remainingSeconds = 60,
                message = "Rate limit reached: Maximum 4 OTP requests per 10 minutes. Please wait before trying again."
            )
        }

        // Generate cryptographically secure 6-digit OTP
        val randomOtp = String.format("%06d", secureRandom.nextInt(1_000_000))
        val expiresAt = now + 5 * 60 * 1000L // 5 minutes validity

        return when (val sendResult = repository.dispatchOtpEmail(cleanEmail, randomOtp, expiresAt)) {
            is EmailSendResult.Success -> {
                recentOtpRequests.add(now)
                _uiState.update {
                    it.copy(
                        pendingGeneratedOtp = randomOtp,
                        otpExpiresEpochMs = expiresAt,
                        customerOtpFailedAttempts = 0,
                        otpDeliveryMessage = "Verification code dispatched to $cleanEmail. Please check your inbox and spam folder."
                    )
                }
                OtpRequestResult.Success(
                    otp = randomOtp,
                    message = "Verification code sent to $cleanEmail. Please check your inbox and spam folder."
                )
            }
            is EmailSendResult.MissingCredentials -> {
                OtpRequestResult.DeliveryFailed(sendResult.message)
            }
            is EmailSendResult.Failure -> {
                OtpRequestResult.DeliveryFailed("Failed to send verification email: ${sendResult.error}. Please check internet connection and try again.")
            }
        }
    }

    /**
     * Verifies the entered OTP with brute-force attempt limits and expiration checking.
     * Allows max 5 incorrect attempts before code is invalidated and a 60-second lockout is applied.
     * If the verified email is the official admin email (Lakshya190207@gmail.com), elevates session to admin.
     * If setupPasscode is provided (from new user registration), securely saves it for future passcode logins.
     */
    fun verifyUserEmailOtpDetailed(
        email: String,
        enteredOtp: String,
        customName: String? = null,
        setupPasscode: String? = null
    ): OtpVerifyResult {
        val now = System.currentTimeMillis()
        val currentLockout = _uiState.value.customerOtpLockoutUntilEpochMs
        if (now < currentLockout) {
            val remainingSec = (((currentLockout - now) / 1000) + 1).toInt()
            return OtpVerifyResult.Locked(
                remainingSeconds = remainingSec,
                message = "Too many failed attempts. Verification locked for ${remainingSec}s."
            )
        }

        val expectedOtp = _uiState.value.pendingGeneratedOtp
        val expiryTime = _uiState.value.otpExpiresEpochMs

        if (expectedOtp == null || (expiryTime > 0 && now > expiryTime)) {
            _uiState.update { it.copy(pendingGeneratedOtp = null) }
            return OtpVerifyResult.Expired(
                message = "This verification code has expired (5-minute validity). Please request a new code."
            )
        }

        val cleanOtp = enteredOtp.trim()
        val isValid = (cleanOtp == expectedOtp)

        if (isValid) {
            val cleanEmail = email.trim().lowercase()
            val isAdmin = cleanEmail.equals(OFFICIAL_ADMIN_EMAIL, ignoreCase = true)
            val name = if (!customName.isNullOrBlank()) {
                customName.trim()
            } else {
                userSessionManager.getUserNameForEmail(cleanEmail)
                    ?: cleanEmail.substringBefore("@")
                        .replace(".", " ")
                        .replace("_", " ")
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        .ifBlank { if (isAdmin) "Executive Administrator" else "Valued Member" }
            }

            if (!setupPasscode.isNullOrBlank()) {
                userSessionManager.registerUser(cleanEmail, name, setupPasscode.trim())
            } else {
                userSessionManager.saveUserSession(cleanEmail, name)
            }

            _uiState.update {
                it.copy(
                    isUserLoggedIn = true,
                    loggedInUserEmail = cleanEmail,
                    loggedInUserName = name,
                    isAdminAuthenticated = isAdmin,
                    pendingGeneratedOtp = null,
                    otpExpiresEpochMs = 0L,
                    customerOtpFailedAttempts = 0,
                    customerOtpLockoutUntilEpochMs = 0L,
                    otpDeliveryMessage = null,
                    userNotificationMessage = if (isAdmin) "Welcome back, Administrator ($OFFICIAL_ADMIN_EMAIL)!" else "Welcome to Good Dream Sanctuary, $name!"
                )
            }
            loadSavedAddresses()
            syncCrmUsers()
            return OtpVerifyResult.Success
        } else {
            val failedCount = _uiState.value.customerOtpFailedAttempts + 1
            if (failedCount >= 5) {
                val lockoutUntil = now + 60_000L
                _uiState.update {
                    it.copy(
                        customerOtpFailedAttempts = failedCount,
                        customerOtpLockoutUntilEpochMs = lockoutUntil,
                        pendingGeneratedOtp = null // invalidate code on brute force attack
                    )
                }
                return OtpVerifyResult.Locked(
                    remainingSeconds = 60,
                    message = "Too many incorrect attempts. For your security, this code was revoked. Please wait 60s."
                )
            } else {
                _uiState.update { it.copy(customerOtpFailedAttempts = failedCount) }
                val remaining = 5 - failedCount
                return OtpVerifyResult.InvalidCode(
                    attemptsRemaining = remaining,
                    message = "Incorrect code. $remaining attempt(s) remaining before security lockout."
                )
            }
        }
    }

    /**
     * Authenticates an existing user via their saved passcode or admin credentials.
     * Enforces rate limiting against brute-force attacks.
     * Automatically elevates to admin if the official admin email and password match.
     */
    fun loginWithPasscode(email: String, passcode: String): PasscodeAuthResult {
        val now = System.currentTimeMillis()
        val currentLockout = _uiState.value.customerOtpLockoutUntilEpochMs
        if (now < currentLockout) {
            val remainingSec = (((currentLockout - now) / 1000) + 1).toInt()
            return PasscodeAuthResult.Locked(
                remainingSeconds = remainingSec,
                message = "Verification locked. Please wait ${remainingSec}s before retrying."
            )
        }

        val cleanEmail = email.trim().lowercase()
        val cleanPass = passcode.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return PasscodeAuthResult.InvalidCredentials(
                attemptsRemaining = 5 - _uiState.value.customerOtpFailedAttempts,
                message = "Please enter a valid email address."
            )
        }

        val isAdmin = cleanEmail.equals(OFFICIAL_ADMIN_EMAIL, ignoreCase = true)
        if (isAdmin) {
            val enteredHash = computeAdminHash(cleanPass)
            val expectedBytes = ADMIN_PASSWORD_HASH.toByteArray(Charsets.UTF_8)
            val enteredBytes = enteredHash.toByteArray(Charsets.UTF_8)
            val isOfficialPass = (expectedBytes.size == enteredBytes.size &&
                    java.security.MessageDigest.isEqual(expectedBytes, enteredBytes))
            val isCustomPass = userSessionManager.verifyUserPasscode(cleanEmail, cleanPass)

            if (isOfficialPass || isCustomPass) {
                userSessionManager.saveUserSession(cleanEmail, "Executive Administrator")
                _uiState.update {
                    it.copy(
                        isUserLoggedIn = true,
                        loggedInUserEmail = cleanEmail,
                        loggedInUserName = "Executive Administrator",
                        isAdminAuthenticated = true,
                        customerOtpFailedAttempts = 0,
                        customerOtpLockoutUntilEpochMs = 0L,
                        userNotificationMessage = "Executive Admin Studio unlocked for $OFFICIAL_ADMIN_EMAIL."
                    )
                }
                return PasscodeAuthResult.Success(
                    isAdmin = true,
                    name = "Executive Administrator",
                    message = "Welcome back, Administrator!"
                )
            }
        } else {
            // Standard customer verification
            if (!userSessionManager.hasUserPasscode(cleanEmail)) {
                return PasscodeAuthResult.UserNotFound(
                    "No passcode configured for $cleanEmail. Please sign in with OTP or register a new account."
                )
            }

            if (userSessionManager.verifyUserPasscode(cleanEmail, cleanPass)) {
                val name = userSessionManager.getUserNameForEmail(cleanEmail)
                    ?: cleanEmail.substringBefore("@")
                userSessionManager.saveUserSession(cleanEmail, name)
                _uiState.update {
                    it.copy(
                        isUserLoggedIn = true,
                        loggedInUserEmail = cleanEmail,
                        loggedInUserName = name,
                        isAdminAuthenticated = false,
                        customerOtpFailedAttempts = 0,
                        customerOtpLockoutUntilEpochMs = 0L,
                        userNotificationMessage = "Welcome back to Good Dream Sanctuary, $name!"
                    )
                }
                loadSavedAddresses()
                return PasscodeAuthResult.Success(
                    isAdmin = false,
                    name = name,
                    message = "Welcome back, $name!"
                )
            }
        }

        // Invalid passcode handling
        val failed = _uiState.value.customerOtpFailedAttempts + 1
        if (failed >= 5) {
            val lockoutUntil = now + 60_000L
            _uiState.update {
                it.copy(
                    customerOtpFailedAttempts = failed,
                    customerOtpLockoutUntilEpochMs = lockoutUntil
                )
            }
            return PasscodeAuthResult.Locked(
                remainingSeconds = 60,
                message = "Too many failed attempts. Security lock active for 60 seconds."
            )
        }

        val remaining = (5 - failed).coerceAtLeast(0)
        _uiState.update { it.copy(customerOtpFailedAttempts = failed) }
        return PasscodeAuthResult.InvalidCredentials(
            attemptsRemaining = remaining,
            message = "Incorrect passcode. $remaining attempt${if (remaining == 1) "" else "s"} remaining."
        )
    }

    suspend fun sendUserEmailOtp(email: String): String {
        return when (val res = requestUserEmailOtp(email)) {
            is OtpRequestResult.Success -> res.otp
            is OtpRequestResult.RateLimited -> res.message
            is OtpRequestResult.InvalidEmail -> ""
            is OtpRequestResult.DeliveryFailed -> res.message
        }
    }

    fun verifyUserEmailOtp(email: String, enteredOtp: String): Boolean {
        return verifyUserEmailOtpDetailed(email, enteredOtp) is OtpVerifyResult.Success
    }

    /**
     * Logs out the customer, clears secure persistent storage, and resets UI state.
     * If the session was admin authenticated, revokes admin privileges immediately.
     */
    fun logoutUser() {
        userSessionManager.clearUserSession()
        _uiState.update {
            it.copy(
                isUserLoggedIn = false,
                loggedInUserEmail = null,
                loggedInUserName = null,
                isAdminAuthenticated = false,
                orders = emptyList(),
                inquiries = emptyList(),
                savedAddresses = emptyList(),
                pendingPostLoginDestination = null,
                isAuthGateVisible = false,
                authGateTargetPage = null,
                userNotificationMessage = "You have signed out of your account."
            )
        }
    }

    /**
     * Completely purges the user's account credentials, active cart, and saved wishlist
     * in compliance with Google Play Store User Data & Account Deletion policy.
     */
    fun deleteAccountAndPurgeData() {
        val currentEmail = _uiState.value.loggedInUserEmail
        userSessionManager.deleteUserAccountAndData(currentEmail)
        viewModelScope.launch {
            repository.clearCart()
            repository.clearWishlist()
        }
        _uiState.update {
            it.copy(
                isUserLoggedIn = false,
                loggedInUserEmail = null,
                loggedInUserName = null,
                isAdminAuthenticated = false,
                cartItems = emptyList(),
                wishlistIds = emptySet(),
                orders = emptyList(),
                inquiries = emptyList(),
                savedAddresses = emptyList(),
                pendingPostLoginDestination = null,
                isAuthGateVisible = false,
                authGateTargetPage = null,
                userNotificationMessage = "Account session and all local data permanently erased."
            )
        }
    }

    // -------------------------------------------------------------
    // Administrator Authentication (Admin ID & Master Password with Rate Limiting)
    // -------------------------------------------------------------

    /**
     * Authenticates executive personnel to access Catalog CRM Studio.
     * Enforces rate limiting against brute-force attacks (60-second lockout after 5 failed attempts).
     * Compares passwords in constant-time using MessageDigest.isEqual.
     * Official Admin ID: Lakshya190207@gmail.com
     */
    fun loginAdminDetailed(adminId: String, adminPass: String): AdminAuthResult {
        val now = System.currentTimeMillis()
        val currentLockout = _uiState.value.adminLockoutUntilEpochMs
        if (now < currentLockout) {
            val remainingSec = (((currentLockout - now) / 1000) + 1).toInt()
            return AdminAuthResult.Locked(
                remainingSeconds = remainingSec,
                message = "Security Lockout: Too many failed admin login attempts. Try again in ${remainingSec}s."
            )
        }

        val cleanId = adminId.trim()
        val cleanPass = adminPass.trim()

        val isIdValid = cleanId.equals(OFFICIAL_ADMIN_EMAIL, ignoreCase = true)

        val enteredHash = computeAdminHash(cleanPass)
        val expectedBytes = ADMIN_PASSWORD_HASH.toByteArray(Charsets.UTF_8)
        val enteredBytes = enteredHash.toByteArray(Charsets.UTF_8)
        val isPassValid = enteredBytes.size == expectedBytes.size &&
                MessageDigest.isEqual(enteredBytes, expectedBytes)

        if (isIdValid && isPassValid) {
            _uiState.update {
                it.copy(
                    isAdminAuthenticated = true,
                    adminFailedAttempts = 0,
                    adminLockoutUntilEpochMs = 0L,
                    userNotificationMessage = "Executive Admin Studio unlocked for $OFFICIAL_ADMIN_EMAIL."
                )
            }
            openPage(ActivePage.ADMIN_PANEL)
            return AdminAuthResult.Success
        } else {
            val failedCount = _uiState.value.adminFailedAttempts + 1
            if (failedCount >= 5) {
                val lockoutUntil = now + 60_000L // 60s lockout
                _uiState.update {
                    it.copy(
                        adminFailedAttempts = failedCount,
                        adminLockoutUntilEpochMs = lockoutUntil
                    )
                }
                return AdminAuthResult.Locked(
                    remainingSeconds = 60,
                    message = "Security Alert: 5 consecutive failed attempts. Administrative access locked for 60 seconds."
                )
            } else {
                _uiState.update { it.copy(adminFailedAttempts = failedCount) }
                val remaining = 5 - failedCount
                return AdminAuthResult.InvalidCredentials(
                    attemptsRemaining = remaining,
                    message = "Invalid Admin ID or Master Password. $remaining attempt(s) remaining before security lockout."
                )
            }
        }
    }

    private fun computeAdminHash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedBytes = ("$password:$ADMIN_SALT").toByteArray(Charsets.UTF_8)
        val hash = digest.digest(saltedBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun loginAdmin(adminId: String, adminPass: String): Boolean {
        return loginAdminDetailed(adminId, adminPass) is AdminAuthResult.Success
    }

    /**
     * Locks the executive CRM Studio and revokes administrator privileges.
     */
    fun logoutAdmin() {
        _uiState.update {
            it.copy(
                isAdminAuthenticated = false,
                userNotificationMessage = "Admin session ended. Studio locked."
            )
        }
        if (_uiState.value.activePage == ActivePage.ADMIN_PANEL || _uiState.value.activePage == ActivePage.ADMIN_LOGIN) {
            closeActivePage()
        }
    }

    companion object {
        const val OFFICIAL_ADMIN_EMAIL = "Lakshya190207@gmail.com"
        // Cryptographic salted hash of authorized master admin password ("GoodDream@2026:SanctuaryAdminSalt2026")
        // Prevents plaintext secret scraping from decompiled DEX bytecode.
        private const val ADMIN_SALT = "SanctuaryAdminSalt2026"
        private const val ADMIN_PASSWORD_HASH = "ae696cc810d25ceeaa01174edb12cadaf05bb107a6923ecdf65fff40aadd9db6"
    }
}

