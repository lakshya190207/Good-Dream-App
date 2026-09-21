package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val displayOrder: Int,
    val thumbnailUrl: String,
    val subtitle: String,
    val isActive: Boolean = true
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val title: String,
    val subtitle: String,
    val sku: String,
    val isSpringhavenSeries: Boolean = false,
    val description: String,
    val specificationsJson: String = "", // Stored as "Key: Value|Key: Value"
    val price: Double,
    val originalPrice: Double,
    val imagesJson: String, // Stored as pipe-separated URLs
    val isNewLaunch: Boolean = false,
    val warrantyYears: Int = 5,
    val thicknessInches: Int = 8,
    val material: String = "Premium Materials",
    val dimensions: String = "78\" x 72\"",
    val firmness: String = "Medium Firm",
    val videoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getImagesList(): List<String> = imagesJson.split("|").filter { it.isNotBlank() }
    
    fun hasVideo(): Boolean = videoUrl.isNotBlank()
    
    fun getSpecificationsMap(): Map<String, String> {
        return specificationsJson.split("|")
            .filter { it.contains(":") }
            .associate {
                val parts = it.split(":", limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey val productId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inquiries")
data class InquiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "NEEDS", "REPAIR", "COMPLAINT", "ORDER_INQUIRY"
    val referenceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String = "",
    val categoryOrProduct: String = "",
    val details: String,
    val status: String = "Registered",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val deliveryAddress: String,
    val city: String,
    val state: String,
    val pincode: String,
    val deliverySlot: String,
    val floorElevator: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val totalAmount: Double,
    val itemsSummary: String,
    val status: String = "Confirmed",
    val createdAt: Long = System.currentTimeMillis()
)

data class PendingPaymentOrderDraft(
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val deliveryAddress: String,
    val city: String,
    val state: String,
    val pincode: String,
    val deliverySlot: String,
    val floorElevator: String,
    val paymentCategory: String, // "ONLINE" or "COD"
    val paymentMethodDetail: String, // "UPI (Google Pay)", "Card Ending in 8842", etc.
    val payableAmount: Double,
    val isCod: Boolean = false,
    val codAdvanceAmount: Double = 0.0,
    val codBalanceAmount: Double = 0.0
)
