package com.example.data.config

import androidx.annotation.Keep

/**
 * Data model for Contact Info stored in Firestore.
 */
@Keep
data class ContactInfo(
    val phone: String = "+91 80 4123 9999",
    val whatsapp: String = "+91 98765 43210",
    val email: String = "care@gooddreamhomedecor.com",
    val address: String = "No. 44, Good Dream Pavilion, Interior Boulevard, Indiranagar, Bengaluru, Karnataka 560038",
    val workingHours: String = "Mon - Sun: 9:30 AM - 8:30 PM",
    val website: String = "https://gooddreamhomedecor.com"
) {
    // Required no-arg constructor for Firestore deserialization
    constructor() : this(
        phone = "+91 80 4123 9999",
        whatsapp = "+91 98765 43210",
        email = "care@gooddreamhomedecor.com",
        address = "No. 44, Good Dream Pavilion, Interior Boulevard, Indiranagar, Bengaluru, Karnataka 560038",
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
                phone = map["phone"] as? String ?: "+91 80 4123 9999",
                whatsapp = map["whatsapp"] as? String ?: "+91 98765 43210",
                email = map["email"] as? String ?: "care@gooddreamhomedecor.com",
                address = map["address"] as? String ?: "No. 44, Good Dream Pavilion, Interior Boulevard, Indiranagar, Bengaluru, Karnataka 560038",
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
    val companyName: String = "Good Dream Home Decor Private Limited",
    val tagline: String = "Comfort for a Better Tomorrow",
    val contactInfo: ContactInfo = ContactInfo(),
    val offerBanners: List<OfferBanner> = listOf(
        OfferBanner(
            id = "banner_springhaven",
            title = "SpringHaven Master Launch Privilege",
            subtitle = "Complimentary Silk Pillows & Cashmere Topper with Sovereign Series",
            discountTag = "25% OFF",
            promoCode = "SPRINGHAVEN25",
            imageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
            actionRoute = "cat_springhaven",
            buttonText = "Explore Sovereign",
            isActive = true
        ),
        OfferBanner(
            id = "banner_sleep_trial",
            title = "100-Night Risk-Free Sleep Trial",
            subtitle = "Zero-Risk Comfort Guarantee on All Orthopedic Mattresses",
            discountTag = "RISK-FREE",
            promoCode = "DREAM100",
            imageUrl = "https://images.unsplash.com/photo-1540518614846-7ede433c4ef0?q=80&w=1000&auto=format&fit=crop",
            actionRoute = "cat_mattress",
            buttonText = "Shop Mattresses",
            isActive = true
        ),
        OfferBanner(
            id = "banner_referral",
            title = "Privilege Referral Rewards",
            subtitle = "Earn ₹2,500 Store Credit on Every Friend & Family Referral",
            discountTag = "₹2,500 BONUS",
            promoCode = "PRIVILEGE2500",
            imageUrl = "https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=1000&auto=format&fit=crop",
            actionRoute = "PURCHASE_REWARDS",
            buttonText = "Refer & Earn",
            isActive = true
        )
    ),
    val announcement: String = "White-Glove Doorstep Delivery & Assembly Complimentary Across Bengaluru",
    val freeDeliveryThreshold: Double = 999.0,
    val warrantyPolicyUrl: String = "https://gooddreamhomedecor.com/warranty-policy",
    val supportHours: String = "Mon - Sun: 9:30 AM - 8:30 PM",
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    // Required no-arg constructor for Firestore deserialization
    constructor() : this(
        companyName = "Good Dream Home Decor Private Limited",
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
                companyName = map["companyName"] as? String ?: "Good Dream Home Decor Private Limited",
                tagline = map["tagline"] as? String ?: "Comfort for a Better Tomorrow",
                contactInfo = ContactInfo.fromMap(contactMap),
                offerBanners = bannersList,
                announcement = map["announcement"] as? String ?: "White-Glove Doorstep Delivery & Assembly Complimentary Across Bengaluru",
                freeDeliveryThreshold = (map["freeDeliveryThreshold"] as? Number)?.toDouble() ?: 999.0,
                warrantyPolicyUrl = map["warrantyPolicyUrl"] as? String ?: "https://gooddreamhomedecor.com/warranty-policy",
                supportHours = map["supportHours"] as? String ?: "Mon - Sun: 9:30 AM - 8:30 PM",
                lastFetchedAt = (map["lastFetchedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
