package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.EmailDeliveryService
import com.example.data.remote.EmailSendResult
import com.example.data.remote.FirestoreCatalogService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GoodDreamRepository(
    private val database: AppDatabase,
    private val firestoreCatalogService: FirestoreCatalogService = FirestoreCatalogService(),
    private val emailDeliveryService: EmailDeliveryService = EmailDeliveryService(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val cartDao = database.cartDao()
    private val wishlistDao = database.wishlistDao()
    private val inquiryDao = database.inquiryDao()
    private val appConfigDao = database.appConfigDao()
    private val orderDao = database.orderDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllActiveCategories()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val newLaunches: Flow<List<ProductEntity>> = productDao.getNewLaunches()
    val springhavenSeries: Flow<List<ProductEntity>> = productDao.getSpringhavenSeries()
    val cartItems: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()
    val wishlistItems: Flow<List<WishlistItemEntity>> = wishlistDao.getAllWishlistItems()
    val allInquiries: Flow<List<InquiryEntity>> = inquiryDao.getAllInquiries()
    val appConfigs: Flow<List<AppConfigEntity>> = appConfigDao.getAllConfigs()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    fun getUserInquiries(email: String): Flow<List<InquiryEntity>> = inquiryDao.getInquiriesByEmail(email)
    fun getUserOrders(email: String): Flow<List<OrderEntity>> = orderDao.getOrdersByEmail(email)

    suspend fun initializeCatalogIfEmpty() = withContext(ioDispatcher) {
        val existingCategories = categoryDao.getAllActiveCategories().first()
        if (existingCategories.isEmpty()) {
            categoryDao.insertCategories(DefaultCatalogData.categories)
            productDao.insertProducts(DefaultCatalogData.products)
            appConfigDao.insertConfigs(DefaultCatalogData.appConfigs)
        }
    }

    /**
     * Synchronizes categories and products with Firestore:
     * 1. If remote Firestore has categories/products, upsert them into local Room database.
     * 2. If remote Firestore is empty, seed it with DefaultCatalogData.
     */
    suspend fun syncCatalogWithCloud() = withContext(ioDispatcher) {
        try {
            // First ensure local DB is initialized if empty
            initializeCatalogIfEmpty()

            // Fetch from Firestore
            val remoteCats = firestoreCatalogService.fetchRemoteCategories()
            val remoteProds = firestoreCatalogService.fetchRemoteProducts()

            if (remoteCats.isNotEmpty()) {
                categoryDao.insertCategories(remoteCats)
                Timber.tag("GoodDreamRepository").i("Upserted ${remoteCats.size} remote categories to Room DB")
            }
            if (remoteProds.isNotEmpty()) {
                productDao.insertProducts(remoteProds)
                Timber.tag("GoodDreamRepository").i("Upserted ${remoteProds.size} remote products to Room DB")
            }

            // If remote is empty, seed it with default catalog data
            if (remoteCats.isEmpty() || remoteProds.isEmpty()) {
                firestoreCatalogService.seedInitialCatalogIfEmpty(
                    DefaultCatalogData.categories,
                    DefaultCatalogData.products
                )
            }
        } catch (e: Exception) {
            Timber.tag("GoodDreamRepository").w("Cloud catalog sync deferred: ${e.message}")
        }
    }

    /**
     * Starts listening in real-time to product updates from Firestore and persists to Room.
     */
    fun startRealtimeProductSync(scope: CoroutineScope) {
        firestoreCatalogService.startRealtimeProductSync { remoteProducts ->
            scope.launch(ioDispatcher) {
                productDao.insertProducts(remoteProducts)
            }
        }
    }

    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>> {
        return productDao.getProductsByCategory(categoryId)
    }

    suspend fun getProductById(id: String): ProductEntity? = withContext(ioDispatcher) {
        productDao.getProductById(id)
    }

    suspend fun insertProduct(product: ProductEntity) = withContext(ioDispatcher) {
        productDao.insertProduct(product)
    }

    suspend fun getCategoryById(id: String): CategoryEntity? = withContext(ioDispatcher) {
        categoryDao.getCategoryById(id)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    // Cart operations
    suspend fun addToCart(productId: String, quantity: Int = 1) = withContext(ioDispatcher) {
        cartDao.addToCart(CartItemEntity(productId = productId, quantity = quantity))
    }

    suspend fun updateCartQuantity(productId: String, quantity: Int) = withContext(ioDispatcher) {
        if (quantity <= 0) {
            cartDao.removeFromCart(productId)
        } else {
            cartDao.updateQuantity(productId, quantity)
        }
    }

    suspend fun removeFromCart(productId: String) = withContext(ioDispatcher) {
        cartDao.removeFromCart(productId)
    }

    suspend fun clearCart() = withContext(ioDispatcher) {
        cartDao.clearCart()
    }

    // Wishlist operations
    suspend fun toggleWishlist(productId: String, isInWishlist: Boolean) = withContext(ioDispatcher) {
        if (isInWishlist) {
            wishlistDao.removeFromWishlist(productId)
        } else {
            wishlistDao.addToWishlist(WishlistItemEntity(productId = productId))
        }
    }

    fun isItemInWishlist(productId: String): Flow<Boolean> {
        return wishlistDao.isInWishlist(productId)
    }

    suspend fun clearWishlist() = withContext(ioDispatcher) {
        wishlistDao.clearAllWishlist()
    }

    // Inquiry & Support operations
    suspend fun submitInquiry(
        type: String,
        name: String,
        phone: String,
        email: String = "",
        categoryOrProduct: String = "",
        details: String
    ): String = withContext(ioDispatcher) {
        val cleanType = type.trim().uppercase()
        val prefix = when (cleanType) {
            "REPAIR" -> "REP"
            "COMPLAINT" -> "CMP"
            "NEEDS" -> "REQ"
            "WARRANTY" -> "WARR"
            "FEEDBACK" -> "REV"
            else -> "INQ"
        }
        val refNumber = "GD-$prefix-${System.currentTimeMillis() % 1000000}"
        val inquiry = InquiryEntity(
            type = cleanType,
            referenceNumber = refNumber,
            customerName = name.trim(),
            customerPhone = phone.trim(),
            customerEmail = email.trim(),
            categoryOrProduct = categoryOrProduct.trim(),
            details = details.trim(),
            status = "Received"
        )
        inquiryDao.insertInquiry(inquiry)
        firestoreCatalogService.saveInquiryToCloud(inquiry)

        // Asynchronously dispatch luxury email notifications without blocking caller
        CoroutineScope(ioDispatcher).launch {
            try {
                // 1. Send customer confirmation email if customer provided email
                val cleanEmail = email.trim()
                if (cleanEmail.contains("@") && cleanEmail.contains(".")) {
                    if (cleanType == "WARRANTY") {
                        val serial = if (categoryOrProduct.isNotBlank()) categoryOrProduct else "GD-SN-ARTISAN"
                        val purchaseDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        emailDeliveryService.sendWarrantyCertificateEmail(
                            recipientEmail = cleanEmail,
                            customerName = name.trim(),
                            certificateNumber = refNumber,
                            mattressSerial = serial,
                            purchaseDate = purchaseDate
                        )
                    } else {
                        emailDeliveryService.sendInquiryConfirmationEmail(
                            recipientEmail = cleanEmail,
                            customerName = name.trim(),
                            referenceNumber = refNumber,
                            inquiryType = cleanType,
                            details = details.trim()
                        )
                    }
                }

                // 2. Send instant priority lead alert to store owner (Lakshya190207@gmail.com)
                emailDeliveryService.sendAdminLeadAlert(
                    customerName = name.trim(),
                    customerPhone = phone.trim(),
                    customerEmail = cleanEmail.ifBlank { "Not provided" },
                    inquiryType = cleanType,
                    details = "Category/Ref: $categoryOrProduct | Details: $details",
                    referenceNumber = refNumber
                )
            } catch (e: Exception) {
                Timber.w(e, "Non-fatal: Failed to send lead notification emails: ${e.message}")
            }
        }

        refNumber
    }

    suspend fun updateInquiryStatus(inquiryIdOrRef: String, newStatus: String) = withContext(ioDispatcher) {
        inquiryDao.updateInquiryStatus(inquiryIdOrRef, newStatus)
        firestoreCatalogService.updateInquiryStatusInCloud(inquiryIdOrRef, newStatus)
    }

    suspend fun lookupInquiries(phoneOrRef: String): List<InquiryEntity> = withContext(ioDispatcher) {
        inquiryDao.lookupInquiries(phoneOrRef, phoneOrRef)
    }

    // Admin CRUD operations (Task 4 in PRD)
    suspend fun insertOrUpdateProduct(product: ProductEntity) = withContext(ioDispatcher) {
        productDao.insertProduct(product)
        firestoreCatalogService.saveProductToCloud(product)
    }

    suspend fun deleteProduct(productId: String) = withContext(ioDispatcher) {
        productDao.deleteProductById(productId)
        firestoreCatalogService.deleteProductFromCloud(productId)
    }

    suspend fun updateAppConfig(key: String, value: String) = withContext(ioDispatcher) {
        appConfigDao.insertConfig(AppConfigEntity(configKey = key, configValue = value))
    }

    suspend fun resetToDefaultCatalog() = withContext(ioDispatcher) {
        categoryDao.insertCategories(DefaultCatalogData.categories)
        productDao.insertProducts(DefaultCatalogData.products)
        appConfigDao.insertConfigs(DefaultCatalogData.appConfigs)
        firestoreCatalogService.seedInitialCatalogIfEmpty(
            DefaultCatalogData.categories,
            DefaultCatalogData.products
        )
    }

    // Real Order & Checkout operations
    suspend fun insertOrder(order: OrderEntity) = withContext(ioDispatcher) {
        orderDao.insertOrder(order)
        firestoreCatalogService.saveOrderToCloud(order)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) = withContext(ioDispatcher) {
        orderDao.updateOrderStatus(orderId, newStatus)
    }

    suspend fun getOrderById(id: String): OrderEntity? = withContext(ioDispatcher) {
        orderDao.getOrderById(id)
    }

    suspend fun lookupUserInquiry(ref: String, email: String): InquiryEntity? = withContext(ioDispatcher) {
        inquiryDao.getInquiryByRefAndEmail(ref, email)
    }

    suspend fun lookupUserOrder(orderId: String, email: String): OrderEntity? = withContext(ioDispatcher) {
        orderDao.getOrderByIdAndEmail(orderId, email)
    }

    suspend fun syncOtpRecord(email: String, otp: String, expiresAt: Long): Boolean = withContext(ioDispatcher) {
        firestoreCatalogService.syncOtpRecord(email, otp, expiresAt)
    }

    suspend fun dispatchOtpEmail(recipientEmail: String, otp: String, expiresAt: Long): EmailSendResult = withContext(ioDispatcher) {
        val result = emailDeliveryService.sendOtpEmail(recipientEmail, otp, 5)
        // Non-blocking fire-and-forget sync to Firestore so DB operations never delay or block email
        CoroutineScope(ioDispatcher).launch {
            try {
                firestoreCatalogService.syncOtpRecord(recipientEmail, otp, expiresAt)
            } catch (e: Throwable) {
                Timber.tag("GoodDreamRepository").w("Notice: Firestore OTP sync deferred: ${e.message}")
            }
        }
        result
    }
}
