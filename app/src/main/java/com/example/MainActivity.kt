package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActivePage
import com.example.ui.viewmodel.GoodDreamViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.NotificationHelper
import com.example.util.RazorpayPaymentHelper
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
  private lateinit var viewModel: GoodDreamViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[GoodDreamViewModel::class.java]

    RazorpayPaymentHelper.preload(this)
    handleIntent(intent)

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      MyApplicationTheme(darkTheme = uiState.isDarkMode) {
        GoodDreamApp(viewModel = viewModel)
      }
    }
  }

  override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
    Timber.i("Razorpay onPaymentSuccess: id=$razorpayPaymentId")
    viewModel.onPaymentSuccess(
      paymentId = razorpayPaymentId ?: "RZP-${System.currentTimeMillis()}",
      paymentDataJson = paymentData?.data?.toString()
    )
  }

  override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
    Timber.w("Razorpay onPaymentError: code=$code, response=$response")
    viewModel.onPaymentError(code, response)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    val rawOrderId = intent?.getStringExtra(NotificationHelper.EXTRA_ORDER_ID)?.trim()
    if (!rawOrderId.isNullOrBlank() && rawOrderId.length <= 64 && rawOrderId.matches(Regex("^[A-Za-z0-9\\-_]+$"))) {
      Timber.i("MainActivity opened via deep link for order: %s", rawOrderId)
      viewModel.trackOrder(rawOrderId)
    } else if (!rawOrderId.isNullOrBlank()) {
      Timber.w("Rejected invalid or malformed order ID from deep link intent: %s", rawOrderId)
    }

    val rawChatId = intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)?.trim()
    if (!rawChatId.isNullOrBlank() && rawChatId.length <= 64 && rawChatId.matches(Regex("^[A-Za-z0-9\\-_]+$"))) {
      Timber.i("MainActivity opened via direct message notification for chat: %s", rawChatId)
      viewModel.openCustomerSupportChatWithSessionId(rawChatId)
    } else if (!rawChatId.isNullOrBlank()) {
      Timber.w("Rejected invalid or malformed chat ID from intent: %s", rawChatId)
    }
  }
}

sealed interface AppDestination {
    data class Page(val page: ActivePage) : AppDestination
    data class Pdp(val product: ProductEntity) : AppDestination
    data class TabView(val tab: MainTab, val selectedCategory: CategoryEntity?) : AppDestination
}

@Composable
fun GoodDreamApp(
    viewModel: GoodDreamViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Hardcore Security: Screen Capture & Recent Tasks Switcher Thumbnail Shielding
    // Automatically applies FLAG_SECURE whenever the administrative CRM Studio is active
    // to block screenshot capturing and prevent the Android OS from caching task switcher
    // thumbnails containing customer PII (phone numbers, delivery addresses, order spend).
    DisposableEffect(uiState.activePage) {
        val window = activity?.window
        val isSensitiveAdminScreen = uiState.activePage == ActivePage.ADMIN_PANEL
        if (isSensitiveAdminScreen) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.i("POST_NOTIFICATIONS runtime permission granted")
        } else {
            Timber.w("POST_NOTIFICATIONS runtime permission denied by user")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Intercept back navigation button presses:
    // Hierarchically navigates back through open drawers, modals, sheets, product details,
    // and categories; if at root Home, prompts user "Press back again to close the app"
    // and only exits when pressed again within 2 seconds.
    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> {
                coroutineScope.launch { drawerState.close() }
            }
            uiState.activePage == ActivePage.CHECKOUT -> {
                viewModel.openPage(ActivePage.CART)
            }
            uiState.activePage == ActivePage.ORDER_SUCCESS -> {
                viewModel.closeActivePage()
                viewModel.selectTab(MainTab.HOME)
            }
            uiState.activePage == ActivePage.CUSTOMER_SUPPORT_CHAT -> {
                viewModel.closeCustomerSupportChat()
            }
            uiState.activePage != ActivePage.NONE -> {
                viewModel.closeActivePage()
            }
            uiState.isCartSheetVisible -> {
                viewModel.toggleCartSheet(false)
            }
            uiState.isWishlistSheetVisible -> {
                viewModel.toggleWishlistSheet(false)
            }
            uiState.selectedProduct != null -> {
                viewModel.closeProductDetail()
            }
            uiState.currentTab == MainTab.PRODUCTS && uiState.selectedCategory != null -> {
                viewModel.clearSelectedCategory()
            }
            uiState.currentTab != MainTab.HOME -> {
                viewModel.selectTab(MainTab.HOME)
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000L) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Press back again to close the app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Display transient notification messages
    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            if (!msg.startsWith("Added") || uiState.lastAddedCartProduct == null) {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearNotificationMessage()
        }
    }

    val supportPhone = uiState.appConfig.contactInfo.phone.ifBlank {
        uiState.appConfigs["support_phone"] ?: "+91 7014983696"
    }
    val supportEmail = uiState.appConfig.contactInfo.email.ifBlank {
        uiState.appConfigs["support_email"] ?: "gooddreamshomedecor@gmail.com"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GoodDreamDrawerContent(
                currentTab = uiState.currentTab,
                onSelectTab = { tab ->
                    viewModel.selectTab(tab)
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenModal = { modal ->
                    viewModel.openModal(modal)
                    coroutineScope.launch { drawerState.close() }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                supportPhone = supportPhone,
                supportEmail = supportEmail,
                isAdminAuthenticated = uiState.isAdminAuthenticated
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("app_scaffold"),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    // Top App Bar is visible on all primary tabs (PDP and dedicated pages have their own dedicated top bars)
                    if (uiState.selectedProduct == null && uiState.activePage == ActivePage.NONE) {
                        GoodDreamTopAppBar(
                            onMenuClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onWishlistClick = { viewModel.toggleWishlistSheet(true) },
                            onCartClick = { viewModel.openCartPage() },
                            wishlistCount = uiState.wishlistIds.size,
                            cartCount = uiState.totalCartItemsCount,
                            isDarkMode = uiState.isDarkMode
                        )
                    }

                    // Flagship Luxury Offline Mode Status Banner
                    AnimatedVisibility(
                        visible = !uiState.isOnline,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = ForestGreenDark,
                            contentColor = SatinGoldAccent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = SatinGoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Sanctuary Mode • Local Catalog Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = SatinGoldAccent
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Navigation Bar is visible on main screens, hidden on PDP and dedicated pages
                if (uiState.selectedProduct == null && uiState.activePage == ActivePage.NONE) {
                    GoodDreamBottomNavBar(
                        currentTab = uiState.currentTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) }
                    )
                }
            },
            floatingActionButton = {
                if (uiState.selectedProduct == null && uiState.activePage == ActivePage.NONE) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openPage(ActivePage.AI_CHAT_BOT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Concierge",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "Ask AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                        },
                        containerColor = SatinGoldAccent,
                        contentColor = TextPrimaryDark,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.testTag("fab_ai_concierge")
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { snackbarData ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ForestGreenDark,
                        border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.8f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = snackbarData.visuals.message,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (uiState.selectedProduct != null || uiState.activePage != ActivePage.NONE) PaddingValues(0.dp) else innerPadding)
            ) {
                val currentDestination: AppDestination = when {
                    uiState.activePage != ActivePage.NONE -> AppDestination.Page(uiState.activePage)
                    uiState.selectedProduct != null -> AppDestination.Pdp(uiState.selectedProduct!!)
                    else -> AppDestination.TabView(uiState.currentTab, uiState.selectedCategory)
                }

                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        val isTargetPageOrPdp = targetState is AppDestination.Page || targetState is AppDestination.Pdp
                        val isInitialPageOrPdp = initialState is AppDestination.Page || initialState is AppDestination.Pdp

                        if (isTargetPageOrPdp && !isInitialPageOrPdp) {
                            (slideInVertically(
                                initialOffsetY = { fullHeight -> (fullHeight * 0.08f).toInt() },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                                )
                        } else if (!isTargetPageOrPdp && isInitialPageOrPdp) {
                            fadeIn(animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing))
                                .togetherWith(
                                    slideOutVertically(
                                        targetOffsetY = { fullHeight -> (fullHeight * 0.08f).toInt() },
                                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                                )
                        } else if (isTargetPageOrPdp && isInitialPageOrPdp) {
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(250)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> (-fullWidth * 0.18f).toInt() },
                                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(200))
                                )
                        } else {
                            fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing))
                                )
                        }
                    },
                    label = "screen_routing_transition",
                    modifier = Modifier.fillMaxSize()
                ) { destination ->
                    // Main Routing: Dedicated Full-Screen Pages, PDP, or Tab Screens
                    when (destination) {
                        is AppDestination.Pdp -> {
                            ProductDetailScreen(
                                product = destination.product,
                                isWishlisted = uiState.wishlistIds.contains(destination.product.id),
                                onToggleWishlist = { viewModel.toggleWishlist(destination.product.id) },
                                onAddToCart = { quantity ->
                                    viewModel.addToCart(destination.product.id, quantity)
                                },
                                onInquireProduct = { product ->
                                    viewModel.sendChatMessage("Could you give me details, firmness rating, and recommended uses for ${product.title}?")
                                    viewModel.openPage(ActivePage.AI_CHAT_BOT)
                                },
                                onBack = { viewModel.closeProductDetail() },
                                onOpenCart = { viewModel.openCartPage() },
                                onOpenBespokeStudio = {
                                    viewModel.closeProductDetail()
                                    viewModel.openBespokeStudio()
                                }
                            )
                        }

                        is AppDestination.TabView -> {
                            when (destination.tab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        onNavigateToTab = { tab -> viewModel.selectTab(tab) },
                                        onOpenModal = { modal -> viewModel.openPage(modal) },
                                        appConfig = uiState.appConfig,
                                        isLoading = uiState.isCatalogLoading
                                    )
                                }

                                MainTab.PRODUCTS -> {
                                    if (destination.selectedCategory != null) {
                                        ProductListingScreen(
                                            products = uiState.products,
                                            categories = uiState.categories,
                                            selectedCategory = destination.selectedCategory,
                                            searchQuery = uiState.searchQuery,
                                            onSearchChange = { viewModel.setSearchQuery(it) },
                                            wishlistIds = uiState.wishlistIds,
                                            onToggleWishlist = { viewModel.toggleWishlist(it) },
                                            onSelectProduct = { viewModel.openProductDetail(it) },
                                            onSelectCategory = { cat ->
                                                if (cat == null) viewModel.clearSelectedCategory()
                                                else viewModel.openCategory(cat)
                                            },
                                            onBackToCategories = { viewModel.clearSelectedCategory() },
                                            onNavigateHome = { viewModel.selectTab(MainTab.HOME) },
                                            isLoading = uiState.isCatalogLoading,
                                            onRefresh = { viewModel.refreshCatalog() },
                                            userSleepProfile = uiState.userSleepProfile
                                        )
                                    } else {
                                        CategoriesHubScreen(
                                            categories = uiState.categories,
                                            onSelectCategory = { cat -> viewModel.openCategory(cat) },
                                            isLoading = uiState.isCatalogLoading,
                                            onRefresh = { viewModel.refreshCatalog() }
                                        )
                                    }
                                }

                                MainTab.NEW_LAUNCHES -> {
                                    NewLaunchesScreen(
                                        products = uiState.products,
                                        categories = uiState.categories,
                                        wishlistIds = uiState.wishlistIds,
                                        onToggleWishlist = { viewModel.toggleWishlist(it) },
                                        onSelectProduct = { viewModel.openProductDetail(it) },
                                        onNavigateHome = { viewModel.selectTab(MainTab.HOME) },
                                        onNavigateToCategories = {
                                            viewModel.selectTab(MainTab.PRODUCTS)
                                            viewModel.clearSelectedCategory()
                                        }
                                    )
                                }

                                MainTab.ACCOUNT -> {
                                    AccountScreen(
                                        wishlistCount = uiState.wishlistIds.size,
                                        cartCount = uiState.totalCartItemsCount,
                                        onOpenModal = { modal -> viewModel.openPage(modal) },
                                        onOpenCart = { viewModel.openCartPage() },
                                        onOpenWishlist = { viewModel.toggleWishlistSheet(true) },
                                        supportPhone = supportPhone,
                                        supportEmail = supportEmail,
                                        privacyPolicyUrl = "https://gooddreamhomedecor.com/privacy-policy",
                                        isDarkMode = uiState.isDarkMode,
                                        onToggleTheme = { viewModel.toggleDarkMode() },
                                        onClearAllData = {
                                            viewModel.deleteAccountAndPurgeData()
                                        },
                                        isUserLoggedIn = uiState.isUserLoggedIn,
                                        loggedInUserEmail = uiState.loggedInUserEmail,
                                        loggedInUserName = uiState.loggedInUserName,
                                        isAdminAuthenticated = uiState.isAdminAuthenticated,
                                        onLoginUser = { viewModel.openPage(ActivePage.USER_LOGIN) },
                                        onLogoutUser = { viewModel.logoutUser() }
                                    )
                                }
                            }
                        }

                        is AppDestination.Page -> {
                            when (destination.page) {
                                ActivePage.NONE -> {}
                                ActivePage.CART -> {
                                    CartScreen(
                                        cartItems = uiState.cartItems,
                                        totalPrice = uiState.totalCartPrice,
                                        onUpdateQuantity = { prodId, qty -> viewModel.updateCartQuantity(prodId, qty) },
                                        onRemoveItem = { prodId -> viewModel.removeFromCart(prodId) },
                                        onClearCart = { viewModel.clearCart() },
                                        onProceedToCheckout = { viewModel.openCheckoutPage() },
                                        onExploreCatalog = {
                                            viewModel.closeActivePage()
                                            viewModel.selectTab(MainTab.PRODUCTS)
                                        },
                                        onBack = { viewModel.closeActivePage() },
                                        isUserLoggedIn = uiState.isUserLoggedIn,
                                        loggedInUserName = uiState.loggedInUserName,
                                        offerBanners = uiState.appConfig.offerBanners,
                                        appliedCouponCode = uiState.appliedCouponCode,
                                        appliedDiscountPercent = uiState.appliedDiscountPercent,
                                        onApplyCoupon = { code, pct -> viewModel.applyCoupon(code, pct) },
                                        onRemoveCoupon = { viewModel.removeCoupon() },
                                        onNavigateToLogin = { viewModel.openPage(ActivePage.USER_LOGIN) }
                                    )
                                }

                                ActivePage.CHECKOUT -> {
                                    CheckoutScreen(
                                        cartItems = uiState.cartItems,
                                        totalPrice = uiState.totalCartPrice,
                                        appliedCouponCode = uiState.appliedCouponCode,
                                        discountPercent = uiState.appliedDiscountPercent,
                                        discountAmount = uiState.discountAmount,
                                        finalPayablePrice = uiState.finalPayablePrice,
                                        initialCustomerEmail = uiState.loggedInUserEmail ?: "",
                                        initialCustomerName = uiState.loggedInUserName ?: "",
                                        savedAddresses = uiState.savedAddresses,
                                        onSaveAddress = { viewModel.saveDeliveryAddress(it) },
                                        onDeleteAddress = { viewModel.deleteDeliveryAddress(it) },
                                        isPaymentProcessing = uiState.isPaymentProcessing,
                                        paymentErrorMessage = uiState.paymentErrorMessage,
                                        onClearPaymentError = { viewModel.clearPaymentError() },
                                        onInitiateOnlinePayment = { draft ->
                                            viewModel.setPendingPaymentDraft(draft)
                                            val summary = uiState.cartItems.joinToString(", ") { "${it.product.title} (${it.quantity}x)" }
                                            val currentActivity = context as? Activity
                                            if (currentActivity != null) {
                                                try {
                                                    RazorpayPaymentHelper.startPayment(
                                                        activity = currentActivity,
                                                        orderDraft = draft,
                                                        cartSummary = summary
                                                    )
                                                } catch (e: Exception) {
                                                    viewModel.onPaymentError(-1, e.message ?: "Failed to launch Razorpay.")
                                                }
                                            } else {
                                                viewModel.onPaymentError(-1, "Unable to find host activity for checkout.")
                                            }
                                        },
                                        onPlaceOrder = { name, phone, email, address, city, state, pincode, slot, floor, payment ->
                                            viewModel.placeOrder(
                                                customerName = name,
                                                customerPhone = phone,
                                                customerEmail = email,
                                                deliveryAddress = address,
                                                city = city,
                                                state = state,
                                                pincode = pincode,
                                                deliverySlot = slot,
                                                floorElevator = floor,
                                                paymentMethod = payment
                                            )
                                        },
                                        onOpenTerms = { viewModel.openPage(ActivePage.TERMS_OF_SERVICE) },
                                        onOpenPrivacy = { viewModel.openPage(ActivePage.PRIVACY_POLICY) },
                                        onBack = { viewModel.openCartPage() }
                                    )
                                }

                                ActivePage.ORDER_SUCCESS -> {
                                    OrderSuccessScreen(
                                        order = uiState.latestPlacedOrder,
                                        onTrackOrder = { orderId ->
                                            viewModel.trackOrder(orderId)
                                        },
                                        onContinueShopping = {
                                            viewModel.closeActivePage()
                                            viewModel.selectTab(MainTab.HOME)
                                        }
                                    )
                                }

                                ActivePage.CUSTOM_INQUIRY, ActivePage.YOUR_NEEDS -> {
                                    if (!uiState.isUserLoggedIn) {
                                        AuthRequiredGateModal(
                                            targetPage = destination.page,
                                            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
                                            onDismiss = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        CustomInquiryScreen(
                                            categories = uiState.categories,
                                            prefilledName = uiState.loggedInUserName,
                                            prefilledEmail = uiState.loggedInUserEmail,
                                            onSubmit = { name, phone, email, cat, details ->
                                                viewModel.submitInquiry("NEEDS", name, phone, email, cat, details) {}
                                            },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.REPAIRS -> {
                                    if (!uiState.isUserLoggedIn) {
                                        AuthRequiredGateModal(
                                            targetPage = destination.page,
                                            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
                                            onDismiss = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        RepairRequestScreen(
                                            prefilledName = uiState.loggedInUserName,
                                            prefilledEmail = uiState.loggedInUserEmail,
                                            onSubmit = { name, phone, email, model, details ->
                                                viewModel.submitInquiry("REPAIR", name, phone, email, model, details) {}
                                            },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.COMPLAINTS -> {
                                    if (!uiState.isUserLoggedIn) {
                                        AuthRequiredGateModal(
                                            targetPage = destination.page,
                                            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
                                            onDismiss = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        ComplaintScreen(
                                            prefilledName = uiState.loggedInUserName,
                                            prefilledEmail = uiState.loggedInUserEmail,
                                            onSubmit = { name, phone, email, orderRef, details ->
                                                viewModel.submitInquiry("COMPLAINT", name, phone, email, orderRef, details) {}
                                            },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.FEEDBACKS -> {
                                    if (!uiState.isUserLoggedIn) {
                                        AuthRequiredGateModal(
                                            targetPage = destination.page,
                                            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
                                            onDismiss = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        FeedbackScreen(
                                            prefilledName = uiState.loggedInUserName,
                                            onSubmit = { name, rating, comment ->
                                                viewModel.submitInquiry("FEEDBACK", name, "$rating Stars", uiState.loggedInUserEmail ?: "", "", comment) {}
                                            },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.SERVICE_WARRANTIES -> {
                                    if (!uiState.isUserLoggedIn) {
                                        AuthRequiredGateModal(
                                            targetPage = destination.page,
                                            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
                                            onDismiss = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        WarrantyServiceScreen(
                                            prefilledName = uiState.loggedInUserName,
                                            prefilledEmail = uiState.loggedInUserEmail,
                                            onSubmit = { name, phone, email, invoice, serial, date ->
                                                val details = "Invoice: $invoice | Serial: $serial | PurchaseDate: $date"
                                                viewModel.submitInquiry("WARRANTY", name, phone, email, serial, details) {}
                                            },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.SPONSOR_REWARDS, ActivePage.PURCHASE_REWARDS, ActivePage.MESSAGE_FOR_YOU -> {
                                    RewardsAndOffersScreen(
                                        onBack = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.ADMIN_PANEL -> {
                                    if (!uiState.isAdminAuthenticated) {
                                        AdminLoginScreen(
                                            adminLockoutUntilEpochMs = uiState.adminLockoutUntilEpochMs,
                                            onLoginAdmin = { id, pass -> viewModel.loginAdminDetailed(id, pass) },
                                            onLoginSuccess = { /* viewModel.loginAdmin automatically opens ADMIN_PANEL */ },
                                            onBack = { viewModel.closeActivePage() }
                                        )
                                    } else {
                                        ProductCrmStudioModal(
                                            categories = uiState.categories,
                                            products = uiState.products,
                                            orders = uiState.orders,
                                            inquiries = uiState.inquiries,
                                            crmUsers = uiState.crmUsers,
                                            supportSessions = uiState.allSupportSessions,
                                            selectedChatSession = uiState.selectedAdminChatSession,
                                            adminChatMessages = uiState.adminChatMessages,
                                            onSaveProduct = { viewModel.addOrUpdateProduct(it) },
                                            onDeleteProduct = { viewModel.deleteProduct(it) },
                                            onResetCatalog = { viewModel.resetCatalog() },
                                            onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
                                            onUpdateInquiryStatus = { id, status -> viewModel.updateInquiryStatus(id, status) },
                                            onSaveUser = { viewModel.saveCrmUser(it.email, it.name, it.phone, it.notes) },
                                            onDeleteUser = { viewModel.deleteCrmUser(it) },
                                            onSelectChatSession = { viewModel.selectAdminChatThread(it) },
                                            onSendAdminReply = { chatId, text -> viewModel.sendAdminSupportReply(chatId, text) },
                                            onSendDirectMessage = { email, name, text, orderRef ->
                                                viewModel.sendAdminDirectMessageToUser(email, name, text, orderRef)
                                            },
                                            onResolveChat = { viewModel.resolveSupportChat(it) },
                                            onLoadSupportDesk = { viewModel.loadAdminSupportDesk() },
                                            onLogoutAdmin = { viewModel.logoutAdmin() },
                                            onClose = { viewModel.closeActivePage() }
                                        )
                                    }
                                }

                                ActivePage.USER_LOGIN -> {
                                    CustomerLoginScreen(
                                        pendingOtp = uiState.pendingGeneratedOtp,
                                        otpExpiresEpochMs = uiState.otpExpiresEpochMs,
                                        customerOtpLockoutUntilEpochMs = uiState.customerOtpLockoutUntilEpochMs,
                                        onRequestOtp = { email -> viewModel.requestUserEmailOtp(email) },
                                        onVerifyOtpDetailed = { email, otp, name, passcode ->
                                            viewModel.verifyUserEmailOtpDetailed(email, otp, name, passcode)
                                        },
                                        onLoginWithPasscode = { email, passcode ->
                                            viewModel.loginWithPasscode(email, passcode)
                                        },
                                        onRegisterDirect = { name, email, passcode ->
                                            viewModel.registerUserDirectly(name, email, passcode)
                                        },
                                        onLoginSuccess = { viewModel.onLoginCompleted() },
                                        onBack = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.ADMIN_LOGIN -> {
                                    AdminLoginScreen(
                                        adminLockoutUntilEpochMs = uiState.adminLockoutUntilEpochMs,
                                        onLoginAdmin = { id, pass -> viewModel.loginAdminDetailed(id, pass) },
                                        onLoginSuccess = { /* Automatically transitions to ADMIN_PANEL */ },
                                        onBack = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.SLEEP_QUIZ -> {
                                    SleepFirmnessQuizModal(
                                        products = uiState.products,
                                        onSelectProduct = { prod ->
                                            viewModel.closeActivePage()
                                            viewModel.openProductDetail(prod)
                                        },
                                        onConsultAi = { prompt ->
                                            viewModel.openPage(ActivePage.AI_CHAT_BOT)
                                            viewModel.sendChatMessage(prompt)
                                        },
                                        onQuizProfileSaved = { profile ->
                                            viewModel.saveUserSleepProfile(profile)
                                        },
                                        onClose = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.ORDER_TRACKING -> {
                                    OrderTrackingModal(
                                        initialOrderRef = uiState.trackingTargetOrderId ?: uiState.latestPlacedOrder?.id ?: uiState.recentInquiryRef,
                                        orders = uiState.orders,
                                        inquiries = uiState.inquiries,
                                        isUserLoggedIn = uiState.isUserLoggedIn,
                                        loggedInUserEmail = uiState.loggedInUserEmail,
                                        isAdmin = uiState.isAdminAuthenticated,
                                        supportPhone = supportPhone,
                                        onOpenLiveChat = { orderRef ->
                                            viewModel.closeActivePage()
                                            viewModel.openCustomerSupportChat(
                                                orderRef = orderRef,
                                                initialMessage = "Hello, I need assistance with my Order #$orderRef."
                                            )
                                        },
                                        onOpenLogin = { viewModel.openPage(ActivePage.USER_LOGIN) },
                                        onClose = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.COMPARE_PRODUCTS -> {
                                    ProductComparisonModal(
                                        mattressProducts = uiState.products,
                                        onSelectProduct = { prod ->
                                            viewModel.closeActivePage()
                                            viewModel.openProductDetail(prod)
                                        },
                                        onClose = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.LEGAL_POLICIES,
                                ActivePage.PRIVACY_POLICY,
                                ActivePage.TERMS_OF_SERVICE,
                                ActivePage.COOKIE_POLICY,
                                ActivePage.REFUND_POLICY,
                                ActivePage.SHIPPING_POLICY -> {
                                    val initialTab = when (destination.page) {
                                        ActivePage.PRIVACY_POLICY -> 1
                                        ActivePage.COOKIE_POLICY -> 2
                                        ActivePage.REFUND_POLICY -> 3
                                        ActivePage.SHIPPING_POLICY -> 4
                                        else -> 0
                                    }
                                    LegalPoliciesModal(
                                        initialTab = initialTab,
                                        onClose = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.AI_CHAT_BOT -> {
                                    AiChatBotModal(
                                        messages = uiState.chatMessages,
                                        isLoading = uiState.isChatLoading,
                                        onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
                                        onClearChat = { viewModel.clearChat() },
                                        onOpenLiveSupport = {
                                            viewModel.closeActivePage()
                                            viewModel.openCustomerSupportChat()
                                        },
                                        onClose = { viewModel.closeActivePage() }
                                    )
                                }

                                ActivePage.CUSTOMER_SUPPORT_CHAT -> {
                                    CustomerSupportChatModal(
                                        session = uiState.activeSupportChatSession,
                                        messages = uiState.supportChatMessages,
                                        isSending = uiState.isSupportChatSending,
                                        onSendMessage = { text -> viewModel.sendCustomerSupportMessage(text) },
                                        onClose = { viewModel.closeCustomerSupportChat() },
                                        supportPhone = supportPhone
                                    )
                                }

                                ActivePage.BESPOKE_STUDIO -> {
                                    BespokeStudioScreen(
                                        onBack = { viewModel.closeActivePage() },
                                        onCommissionSuccess = {
                                            viewModel.closeActivePage()
                                            viewModel.openCartPage()
                                        },
                                        onCommissionMattress = { config, onComplete ->
                                            viewModel.commissionBespokeMattress(config) {
                                                onComplete()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Luxury Dynamic Island Add-to-Cart Notification
                AnimatedVisibility(
                    visible = uiState.lastAddedCartProduct != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.94f),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(240)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    uiState.lastAddedCartProduct?.let { product ->
                        LuxuryAddToCartNotification(
                            product = product,
                            quantity = uiState.lastAddedQuantity,
                            totalCartCount = uiState.totalCartItemsCount,
                            onViewCart = {
                                viewModel.clearLastAddedCartProduct()
                                viewModel.openCartPage()
                            },
                            onDismiss = {
                                viewModel.clearLastAddedCartProduct()
                            }
                        )
                    }
                }

                // Offline Sanctuary Mode Resilience Indicator
                AnimatedVisibility(
                    visible = !uiState.isOnline,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ForestGreenPrimary,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Sanctuary Mode • Browsing Cached Catalog & Wishlist",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Wishlist Bottom Sheet (Quick Peek)
    if (uiState.isWishlistSheetVisible) {
        val wishlistedProducts = remember(uiState.products, uiState.wishlistIds) {
            uiState.products.filter { uiState.wishlistIds.contains(it.id) }
        }
        WishlistBottomSheet(
            wishlistProducts = wishlistedProducts,
            onAddToCart = { prodId -> viewModel.addToCart(prodId) },
            onRemoveFromWishlist = { prodId -> viewModel.toggleWishlist(prodId) },
            onSelectProduct = { prod ->
                viewModel.openProductDetail(prod)
            },
            onDismiss = { viewModel.toggleWishlistSheet(false) }
        )
    }

    // Authentication Gate Modal for Service Forms
    if (uiState.isAuthGateVisible) {
        AuthRequiredGateModal(
            targetPage = uiState.authGateTargetPage,
            onProceedToLogin = { viewModel.proceedToLoginFromGate() },
            onDismiss = { viewModel.dismissAuthGate() }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

