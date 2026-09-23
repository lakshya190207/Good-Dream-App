package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getAllActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isNewLaunch = 1 ORDER BY createdAt DESC")
    fun getNewLaunches(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isSpringhavenSeries = 1 ORDER BY createdAt DESC")
    fun getSpringhavenSeries(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE productId = :productId")
    suspend fun updateQuantity(productId: String, quantity: Int)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun removeFromCart(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items ORDER BY addedAt DESC")
    fun getAllWishlistItems(): Flow<List<WishlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun removeFromWishlist(productId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    fun isInWishlist(productId: String): Flow<Boolean>

    @Query("DELETE FROM wishlist_items")
    suspend fun clearAllWishlist()
}

@Dao
interface InquiryDao {
    @Query("SELECT * FROM inquiries ORDER BY createdAt DESC")
    fun getAllInquiries(): Flow<List<InquiryEntity>>

    @Query("SELECT * FROM inquiries WHERE LOWER(customerEmail) = LOWER(:email) ORDER BY createdAt DESC")
    fun getInquiriesByEmail(email: String): Flow<List<InquiryEntity>>

    @Query("SELECT * FROM inquiries WHERE (referenceNumber = :ref OR id = :ref) AND LOWER(customerEmail) = LOWER(:email) LIMIT 1")
    suspend fun getInquiryByRefAndEmail(ref: String, email: String): InquiryEntity?

    @Query("SELECT * FROM inquiries WHERE customerPhone = :phone OR referenceNumber = :ref")
    suspend fun lookupInquiries(phone: String, ref: String): List<InquiryEntity>

    @Query("SELECT * FROM inquiries WHERE id = :id OR referenceNumber = :id LIMIT 1")
    suspend fun getInquiryById(id: String): InquiryEntity?

    @Query("UPDATE inquiries SET status = :status WHERE id = :id OR referenceNumber = :id")
    suspend fun updateInquiryStatus(id: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInquiry(inquiry: InquiryEntity): Long
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config")
    fun getAllConfigs(): Flow<List<AppConfigEntity>>

    @Query("SELECT configValue FROM app_config WHERE configKey = :key LIMIT 1")
    suspend fun getConfigValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<AppConfigEntity>)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE LOWER(customerEmail) = LOWER(:email) ORDER BY createdAt DESC")
    fun getOrdersByEmail(email: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id AND LOWER(customerEmail) = LOWER(:email) LIMIT 1")
    suspend fun getOrderByIdAndEmail(id: String, email: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAtEpochMs DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAtEpochMs DESC")
    suspend fun getAllUsersSync(): List<UserEntity>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentSession = 1 LIMIT 1")
    suspend fun getCurrentSessionUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isCurrentSession = 0")
    suspend fun clearCurrentSessionFlag()

    @Query("UPDATE users SET isCurrentSession = 1 WHERE LOWER(email) = LOWER(:email)")
    suspend fun setCurrentSessionFlag(email: String)

    @Query("DELETE FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun deleteUserByEmail(email: String)
}
