package com.example.data.remote

import com.example.data.model.CategoryEntity
import com.example.data.model.InquiryEntity
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration

/**
 * Service for centralizing real-time catalog, CRM, inquiry, and order data in Firebase Firestore.
 * Provides resilient offline fallback so the application works seamlessly even if Firebase credentials
 * or network connectivity are deferred.
 */
class FirestoreCatalogService {

    companion object {
        private const val TAG = "FirestoreCatalog"
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_PRODUCTS = "products"
        const val COLLECTION_ORDERS = "orders"
        const val COLLECTION_INQUIRIES = "inquiries"
        @Volatile private var isSettingsConfigured = false
    }

    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            val apps = FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext)
            if (apps.isEmpty()) {
                Timber.tag(TAG).w("No active FirebaseApp instance found; Firestore operations deferred.")
                null
            } else {
                val db = FirebaseFirestore.getInstance()
                if (!isSettingsConfigured) {
                    try {
                        @Suppress("DEPRECATION")
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                        db.firestoreSettings = settings
                        isSettingsConfigured = true
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("Notice: Firestore settings already initialized: ${e.message}")
                    }
                }
                db
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Firebase not yet initialized: ${e.message}")
            null
        }
    }

    /**
     * Fetches all active categories from Firestore.
     */
    suspend fun fetchRemoteCategories(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection(COLLECTION_CATEGORIES)
                .get()
                .awaitTask()

            val categories = snapshot.documents.mapNotNull { doc -> documentToCategory(doc) }
            Timber.tag(TAG).i("Fetched ${categories.size} categories from Firestore")
            categories
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to fetch remote categories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches all products from Firestore.
     */
    suspend fun fetchRemoteProducts(): List<ProductEntity> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection(COLLECTION_PRODUCTS)
                .get()
                .awaitTask()

            val products = snapshot.documents.mapNotNull { doc -> documentToProduct(doc) }
            Timber.tag(TAG).i("Fetched ${products.size} products from Firestore")
            products
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to fetch remote products: ${e.message}")
            emptyList()
        }
    }

    /**
     * Seeds initial default categories and products to Firestore if the cloud collections are empty.
     */
    suspend fun seedInitialCatalogIfEmpty(
        defaultCategories: List<CategoryEntity>,
        defaultProducts: List<ProductEntity>
    ) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext
        try {
            val catSnapshot = firestore.collection(COLLECTION_CATEGORIES).limit(1).get().awaitTask()
            if (catSnapshot.isEmpty) {
                Timber.tag(TAG).i("Firestore categories collection empty. Seeding ${defaultCategories.size} categories...")
                for (cat in defaultCategories) {
                    firestore.collection(COLLECTION_CATEGORIES).document(cat.id).set(cat.toFirestoreMap()).awaitTask()
                }
            }

            val prodSnapshot = firestore.collection(COLLECTION_PRODUCTS).limit(1).get().awaitTask()
            if (prodSnapshot.isEmpty) {
                Timber.tag(TAG).i("Firestore products collection empty. Seeding ${defaultProducts.size} products...")
                for (prod in defaultProducts) {
                    firestore.collection(COLLECTION_PRODUCTS).document(prod.id).set(prod.toFirestoreMap()).awaitTask()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Error seeding initial catalog to Firestore: ${e.message}")
        }
    }

    /**
     * Upserts a product in Firestore (called when an admin saves or edits a product in CRM Studio).
     */
    suspend fun saveProductToCloud(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(product.id)
                .set(product.toFirestoreMap())
                .awaitTask()
            Timber.tag(TAG).i("Product ${product.id} successfully synced to Firestore")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to sync product ${product.id} to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Deletes a product from Firestore (called when an admin removes a product in CRM Studio).
     */
    suspend fun deleteProductFromCloud(productId: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_PRODUCTS)
                .document(productId)
                .delete()
                .awaitTask()
            Timber.tag(TAG).i("Product $productId successfully deleted from Firestore")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to delete product $productId from Firestore: ${e.message}")
            false
        }
    }

    /**
     * Saves a placed order to Firestore so the warehouse and dispatch team receives it in real-time.
     */
    suspend fun saveOrderToCloud(order: OrderEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_ORDERS)
                .document(order.id)
                .set(order.toFirestoreMap())
                .awaitTask()
            Timber.tag(TAG).i("Order ${order.id} successfully published to Firestore orders")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to publish order ${order.id} to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Saves an inquiry, complaint, repair, or warranty claim to Firestore.
     */
    suspend fun saveInquiryToCloud(inquiry: InquiryEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            val docId = inquiry.referenceNumber.ifBlank { "INQ-${System.currentTimeMillis()}" }
            firestore.collection(COLLECTION_INQUIRIES)
                .document(docId)
                .set(inquiry.toFirestoreMap())
                .awaitTask()
            Timber.tag(TAG).i("Inquiry $docId successfully published to Firestore inquiries")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to publish inquiry to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Updates an inquiry or claim status in Firestore (e.g. 'In Review', 'Contacted', 'Resolved').
     */
    suspend fun updateInquiryStatusInCloud(docId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_INQUIRIES)
                .document(docId)
                .update("status", newStatus)
                .awaitTask()
            Timber.tag(TAG).i("Inquiry $docId status updated to $newStatus in Firestore")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to update inquiry $docId status in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Listens in real-time to changes in Firestore products collection and emits updates.
     */
    fun startRealtimeProductSync(onProductsUpdated: (List<ProductEntity>) -> Unit): ListenerRegistration? {
        val firestore = getFirestoreInstance() ?: return null
        return try {
            firestore.collection(COLLECTION_PRODUCTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).w("Realtime product sync error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val products = snapshot.documents.mapNotNull { doc -> documentToProduct(doc) }
                        if (products.isNotEmpty()) {
                            onProductsUpdated(products)
                            Timber.tag(TAG).i("Realtime product sync updated ${products.size} products")
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to attach realtime product listener: ${e.message}")
            null
        }
    }

    /**
     * Updates an order status (e.g. "Processing", "Dispatched", "Delivered") in Firestore.
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_ORDERS)
                .document(orderId)
                .update("status", newStatus)
                .awaitTask()
            Timber.tag(TAG).i("Order $orderId status updated to $newStatus in Firestore")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to update order status in Firestore: ${e.message}")
            false
        }
    }

    // Mapping Helpers
    private fun ProductEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "categoryId" to categoryId,
        "title" to title,
        "subtitle" to subtitle,
        "sku" to sku,
        "isSpringhavenSeries" to isSpringhavenSeries,
        "description" to description,
        "specificationsJson" to specificationsJson,
        "price" to price,
        "originalPrice" to originalPrice,
        "imagesJson" to imagesJson,
        "isNewLaunch" to isNewLaunch,
        "warrantyYears" to warrantyYears,
        "thicknessInches" to thicknessInches,
        "material" to material,
        "dimensions" to dimensions,
        "firmness" to firmness,
        "videoUrl" to videoUrl,
        "createdAt" to createdAt
    )

    private fun documentToProduct(doc: DocumentSnapshot): ProductEntity? {
        val data = doc.data ?: return null
        val id = doc.id.ifBlank { data["id"] as? String ?: return null }
        return ProductEntity(
            id = id,
            categoryId = data["categoryId"] as? String ?: "cat_springhaven",
            title = data["title"] as? String ?: "",
            subtitle = data["subtitle"] as? String ?: "",
            sku = data["sku"] as? String ?: "GD-PRD",
            isSpringhavenSeries = data["isSpringhavenSeries"] as? Boolean ?: false,
            description = data["description"] as? String ?: "",
            specificationsJson = data["specificationsJson"] as? String ?: "",
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            originalPrice = (data["originalPrice"] as? Number)?.toDouble() ?: 0.0,
            imagesJson = data["imagesJson"] as? String ?: "",
            isNewLaunch = data["isNewLaunch"] as? Boolean ?: false,
            warrantyYears = (data["warrantyYears"] as? Number)?.toInt() ?: 5,
            thicknessInches = (data["thicknessInches"] as? Number)?.toInt() ?: 8,
            material = data["material"] as? String ?: "Swedish Coils & Natural Latex",
            dimensions = data["dimensions"] as? String ?: "78\" x 72\"",
            firmness = data["firmness"] as? String ?: "Medium Firm",
            videoUrl = data["videoUrl"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun CategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "slug" to slug,
        "displayOrder" to displayOrder,
        "thumbnailUrl" to thumbnailUrl,
        "subtitle" to subtitle,
        "isActive" to isActive
    )

    private fun documentToCategory(doc: DocumentSnapshot): CategoryEntity? {
        val data = doc.data ?: return null
        val id = doc.id.ifBlank { data["id"] as? String ?: return null }
        return CategoryEntity(
            id = id,
            name = data["name"] as? String ?: "",
            slug = data["slug"] as? String ?: "",
            displayOrder = (data["displayOrder"] as? Number)?.toInt() ?: 0,
            thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
            subtitle = data["subtitle"] as? String ?: "",
            isActive = data["isActive"] as? Boolean ?: true
        )
    }

    private fun OrderEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerEmail" to customerEmail,
        "deliveryAddress" to deliveryAddress,
        "city" to city,
        "state" to state,
        "pincode" to pincode,
        "deliverySlot" to deliverySlot,
        "floorElevator" to floorElevator,
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "totalAmount" to totalAmount,
        "itemsSummary" to itemsSummary,
        "status" to status,
        "createdAt" to createdAt
    )

    private fun InquiryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "referenceNumber" to referenceNumber,
        "type" to type,
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerEmail" to customerEmail,
        "categoryOrProduct" to categoryOrProduct,
        "details" to details,
        "status" to status,
        "createdAt" to createdAt
    )

    /**
     * Records the generated OTP in Firestore under 'otps' collection for auditing and cloud dispatch.
     */
    suspend fun syncOtpRecord(email: String, otp: String, expiresAt: Long): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            val data = mapOf(
                "email" to email,
                "otp" to otp,
                "createdAt" to System.currentTimeMillis(),
                "expiresAt" to expiresAt
            )
            firestore.collection("otps").document(email.replace(".", "_")).set(data).awaitTask()
            Timber.tag(TAG).i("Successfully synchronized OTP to Firestore for $email")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Notice: Could not sync OTP to Firestore: ${e.message}")
            false
        }
    }
}

/**
 * Suspend extension to await a Play Services Task safely.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
