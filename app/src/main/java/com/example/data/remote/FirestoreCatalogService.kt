package com.example.data.remote

import com.example.data.model.CategoryEntity
import com.example.data.model.CrmUserRecord
import com.example.data.model.CrmUserType
import com.example.data.model.InquiryEntity
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SupportChatMessage
import com.example.data.model.SupportChatSession
import com.example.data.model.SupportSenderType
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
        const val COLLECTION_SUPPORT_CHATS = "support_chats"
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_MESSAGES = "messages"
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

    /**
     * Sends a message into a customer support chat session.
     * Also updates the parent session with latest message, timestamp, and metadata.
     */
    suspend fun sendSupportMessage(
        session: SupportChatSession,
        message: SupportChatMessage
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            val sessionRef = firestore.collection(COLLECTION_SUPPORT_CHATS).document(session.id)
            val sessionData = mapOf(
                "id" to session.id,
                "customerName" to session.customerName,
                "customerEmail" to session.customerEmail,
                "customerPhone" to session.customerPhone,
                "lastMessage" to message.text,
                "lastMessageTimestamp" to message.timestamp,
                "status" to session.status,
                "orderReference" to (session.orderReference ?: message.orderReference),
                "createdAt" to session.createdAt
            )
            sessionRef.set(sessionData, SetOptions.merge()).awaitTask()

            val msgData = mapOf(
                "id" to message.id,
                "chatId" to session.id,
                "senderType" to message.senderType.name,
                "senderName" to message.senderName,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "orderReference" to message.orderReference
            )
            sessionRef.collection(SUBCOLLECTION_MESSAGES).document(message.id).set(msgData).awaitTask()
            Timber.tag(TAG).i("Sent support message ${message.id} for session ${session.id}")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to send support message: ${e.message}")
            false
        }
    }

    /**
     * Observes real-time messages for a specific customer support chat session.
     */
    fun observeSupportMessages(
        chatId: String,
        onUpdate: (List<SupportChatMessage>) -> Unit
    ): ListenerRegistration? {
        val firestore = getFirestoreInstance() ?: return null
        return try {
            firestore.collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).w("Support messages listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            documentToSupportChatMessage(doc)
                        }
                        onUpdate(messages)
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Error starting support messages listener: ${e.message}")
            null
        }
    }

    /**
     * Observes all active support sessions for the Admin Support Desk.
     */
    fun observeAllSupportSessions(
        onUpdate: (List<SupportChatSession>) -> Unit
    ): ListenerRegistration? {
        val firestore = getFirestoreInstance() ?: return null
        return try {
            firestore.collection(COLLECTION_SUPPORT_CHATS)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).w("Support sessions listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            documentToSupportChatSession(doc)
                        }
                        onUpdate(sessions)
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Error starting support sessions listener: ${e.message}")
            null
        }
    }

    /**
     * Updates the status of a support chat session (e.g. "OPEN", "IN_PROGRESS", "RESOLVED").
     */
    suspend fun updateSupportChatStatus(chatId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            firestore.collection(COLLECTION_SUPPORT_CHATS)
                .document(chatId)
                .update("status", newStatus)
                .awaitTask()
            Timber.tag(TAG).i("Updated chat session $chatId status to $newStatus")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to update chat status: ${e.message}")
            false
        }
    }

    private fun documentToSupportChatMessage(doc: DocumentSnapshot): SupportChatMessage? {
        val data = doc.data ?: return null
        val id = doc.id.ifBlank { data["id"] as? String ?: return null }
        val senderTypeStr = data["senderType"] as? String ?: SupportSenderType.CUSTOMER.name
        val senderType = try {
            SupportSenderType.valueOf(senderTypeStr)
        } catch (e: Exception) {
            SupportSenderType.CUSTOMER
        }
        return SupportChatMessage(
            id = id,
            chatId = data["chatId"] as? String ?: "",
            senderType = senderType,
            senderName = data["senderName"] as? String ?: "",
            text = data["text"] as? String ?: "",
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            orderReference = data["orderReference"] as? String
        )
    }

    private fun documentToSupportChatSession(doc: DocumentSnapshot): SupportChatSession? {
        val data = doc.data ?: return null
        val id = doc.id.ifBlank { data["id"] as? String ?: return null }
        return SupportChatSession(
            id = id,
            customerName = data["customerName"] as? String ?: "Guest Customer",
            customerEmail = data["customerEmail"] as? String ?: "",
            customerPhone = data["customerPhone"] as? String ?: "",
            lastMessage = data["lastMessage"] as? String ?: "",
            lastMessageTimestamp = (data["lastMessageTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = data["status"] as? String ?: "OPEN",
            unreadByCustomer = (data["unreadByCustomer"] as? Number)?.toInt() ?: 0,
            unreadByAdmin = (data["unreadByAdmin"] as? Number)?.toInt() ?: 0,
            orderReference = data["orderReference"] as? String,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    /**
     * Persists or updates a user profile in Firestore collection "users".
     */
    suspend fun saveRemoteUser(
        email: String,
        name: String,
        phone: String = "",
        userType: String = "REGISTERED_VIP",
        notes: String = "",
        fcmToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext false
        try {
            val cleanEmail = email.trim().lowercase()
            val userMap = mutableMapOf<String, Any>(
                "email" to cleanEmail,
                "name" to name.trim(),
                "phone" to phone.trim(),
                "userType" to userType,
                "notes" to notes.trim(),
                "updatedAt" to System.currentTimeMillis()
            )
            if (!fcmToken.isNullOrBlank()) {
                userMap["fcmToken"] = fcmToken
            }
            firestore.collection(COLLECTION_USERS)
                .document(cleanEmail)
                .set(userMap, SetOptions.merge())
                .awaitTask()
            Timber.tag(TAG).i("User $cleanEmail saved to Cloud Firestore users collection")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to save user to Cloud Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches all registered users from Firestore collection "users".
     */
    suspend fun fetchRemoteUsers(): List<CrmUserRecord> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .get()
                .awaitTask()

            val users = snapshot.documents.mapNotNull { doc ->
                val email = doc.getString("email") ?: doc.id
                val name = doc.getString("name") ?: "Valued Member"
                val phone = doc.getString("phone") ?: ""
                val notes = doc.getString("notes") ?: ""
                val typeStr = doc.getString("userType") ?: "REGISTERED_VIP"
                val userType = try {
                    CrmUserType.valueOf(typeStr)
                } catch (_: Exception) {
                    CrmUserType.REGISTERED_VIP
                }
                CrmUserRecord(
                    id = email,
                    name = name,
                    email = email,
                    phone = phone,
                    userType = userType,
                    notes = notes
                )
            }
            Timber.tag(TAG).i("Fetched ${users.size} users from Cloud Firestore")
            users
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to fetch remote users from Cloud Firestore: ${e.message}")
            emptyList()
        }
    }

    /**
     * Safely fetches a single user profile by email from Firestore without exposing other customers' records.
     */
    suspend fun fetchRemoteUserByEmail(email: String): CrmUserRecord? = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext null
        try {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) return@withContext null
            val doc = firestore.collection(COLLECTION_USERS)
                .document(cleanEmail)
                .get()
                .awaitTask()

            if (!doc.exists()) return@withContext null
            val name = doc.getString("name") ?: "Valued Member"
            val phone = doc.getString("phone") ?: ""
            val notes = doc.getString("notes") ?: ""
            val typeStr = doc.getString("userType") ?: "REGISTERED_VIP"
            val userType = try {
                CrmUserType.valueOf(typeStr)
            } catch (_: Exception) {
                CrmUserType.REGISTERED_VIP
            }
            CrmUserRecord(
                id = cleanEmail,
                name = name,
                email = cleanEmail,
                phone = phone,
                userType = userType,
                notes = notes
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to fetch remote user $email from Cloud Firestore: ${e.message}")
            null
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
