package com.example.data.config

import androidx.annotation.Keep

/**
 * Data model for Contact Info stored in Firestore.
 */
@Keep
data class ContactInfo(
    val phone: String = "+91 7014983696",
    val whatsapp: String = "+91 7014983696",
    val email: String = "gooddreamshomedecor@gmail.com",
    val address: String = "D-4, VIJAY VIHAR COLONY, NAYA KHEDA, Amba Bari, Jaipur, Jaipur- 302039, Rajasthan\nMarketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013",
    val workingHours: String = "Mon - Sun: 9:30 AM - 8:30 PM",
    val website: String = "https://gooddreamhomedecor.com"
) {
    // Required no-arg constructor for Firestore deserialization
    constructor() : this(
        phone = "+91 7014983696",
        whatsapp = "+91 7014983696",
        email = "gooddreamshomedecor@gmail.com",
        address = "D-4, VIJAY VIHAR COLONY, NAYA KHEDA, Amba Bari, Jaipur, Jaipur- 302039, Rajasthan\nMarketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013",
        workingHours = "Mon - Sun: 9:30 AM - 8:30 PM",
        website = "https://gooddreamhomedecor.com"
    )

    fun toMap(): Map<String, Any> = mapOf(
        "phone" to phone,
        "whatsapp" to whatsapp,
        "email" to email,
        "address" to address,
        "workingHours" to workingHours,
        "website" to website
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): ContactInfo {
            if (map == null) return ContactInfo()
            return ContactInfo(
                phone = map["phone"] as? String ?: "+91 7014983696",
                whatsapp = map["whatsapp"] as? String ?: "+91 7014983696",
                email = map["email"] as? String ?: "gooddreamshomedecor@gmail.com",
                address = map["address"] as? String ?: "D-4, VIJAY VIHAR COLONY, NAYA KHEDA, Amba Bari, Jaipur, Jaipur- 302039, Rajasthan\nMarketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013",
                workingHours = map["workingHours"] as? String ?: "Mon - Sun: 9:30 AM - 8:30 PM",
                website = map["website"] as? String ?: "https://gooddreamhomedecor.com"
            )
        }
    }
}

/**
 * Data model for dynamic Offer Banners stored in Firestore.
 */
@Keep
data class OfferBanner(
    val id: String = "banner_default",
    val title: String = "Festive Grand Launch",
    val subtitle: String = "Complimentary Silk Pillows with SpringHaven Series",
    val discountTag: String = "25% OFF",
    val promoCode: String = "GOODDREAM25",
    val imageUrl: String = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
    val actionRoute: String = "cat_springhaven",
    val buttonText: String = "Claim Offer",
    val isActive: Boolean = true
) {
    // Required no-arg constructor for Firestore deserialization
    constructor() : this(
        id = "banner_default",
        title = "Festive Grand Launch",
        subtitle = "Complimentary Silk Pillows with SpringHaven Series",
        discountTag = "25% OFF",
        promoCode = "GOODDREAM25",
        imageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
        actionRoute = "cat_springhaven",
        buttonText = "Claim Offer",
        isActive = true
    )

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "subtitle" to subtitle,
        "discountTag" to discountTag,
        "promoCode" to promoCode,
        "imageUrl" to imageUrl,
        "actionRoute" to actionRoute,
        "buttonText" to buttonText,
        "isActive" to isActive
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): OfferBanner {
            if (map == null) return OfferBanner()
            return OfferBanner(
                id = map["id"] as? String ?: "banner_default",
                title = map["title"] as? String ?: "Festive Grand Launch",
                subtitle = map["subtitle"] as? String ?: "Complimentary Silk Pillows with SpringHaven Series",
                discountTag = map["discountTag"] as? String ?: "25% OFF",
                promoCode = map["promoCode"] as? String ?: "GOODDREAM25",
                imageUrl = map["imageUrl"] as? String ?: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
                actionRoute = map["actionRoute"] as? String ?: "cat_springhaven",
                buttonText = map["buttonText"] as? String ?: "Claim Offer",
                isActive = (map["isActive"] as? Boolean) ?: true
            )
        }
    }
}

/**
 * Top-level Firestore data model for AppConfig storing dynamic content.
 */
@Keep
data class AppConfig(
    val companyName: String = "GOOD DREAMS HOME DECOR PRIVATE LIMITED",
    val tagline: String = "Comfort for a Better Tomorrow",
    val contactInfo: ContactInfo = ContactInfo(),
    val offerBanners: List<OfferBanner> = emptyList(),
    val announcement: String = "Doorstep Delivery & Assembly Complimentary Across Jaipur & All India",
    val freeDeliveryThreshold: Double = 999.0,
    val warrantyPolicyUrl: String = "https://gooddreamhomedecor.com/warranty-policy",
    val supportHours: String = "Mon - Sun: 9:30 AM - 8:30 PM",
    val razorpayKeyId: String = "",
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    // Required no-arg constructor for Firestore deserialization
    constructor() : this(
        companyName = "GOOD DREAMS HOME DECOR PRIVATE LIMITED",
        tagline = "Comfort for a Better Tomorrow"
    )

    fun toMap(): Map<String, Any> = mapOf(
        "companyName" to companyName,
        "tagline" to tagline,
        "contactInfo" to contactInfo.toMap(),
        "offerBanners" to offerBanners.map { it.toMap() },
        "announcement" to announcement,
        "freeDeliveryThreshold" to freeDeliveryThreshold,
        "warrantyPolicyUrl" to warrantyPolicyUrl,
        "supportHours" to supportHours,
        "razorpayKeyId" to razorpayKeyId,
        "lastFetchedAt" to lastFetchedAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): AppConfig {
            if (map == null) return AppConfig()
            val contactMap = map["contactInfo"] as? Map<String, Any?>
            val bannersRaw = map["offerBanners"] as? List<Map<String, Any?>>
            val bannersList = bannersRaw?.map { OfferBanner.fromMap(it) } ?: AppConfig().offerBanners

            return AppConfig(
                companyName = map["companyName"] as? String ?: "GOOD DREAMS HOME DECOR PRIVATE LIMITED",
                tagline = map["tagline"] as? String ?: "Comfort for a Better Tomorrow",
                contactInfo = ContactInfo.fromMap(contactMap),
                offerBanners = bannersList,
                announcement = map["announcement"] as? String ?: "Doorstep Delivery & Assembly Complimentary Across Jaipur & All India",
                freeDeliveryThreshold = (map["freeDeliveryThreshold"] as? Number)?.toDouble() ?: 999.0,
                warrantyPolicyUrl = map["warrantyPolicyUrl"] as? String ?: "https://gooddreamhomedecor.com/warranty-policy",
                supportHours = map["supportHours"] as? String ?: "Mon - Sun: 9:30 AM - 8:30 PM",
                razorpayKeyId = map["razorpayKeyId"] as? String ?: "",
                lastFetchedAt = (map["lastFetchedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
