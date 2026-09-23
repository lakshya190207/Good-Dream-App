package com.example.util

import android.app.Activity
import com.example.BuildConfig
import com.example.data.config.AppConfigProvider
import com.example.data.config.FirebaseRemoteConfigHelper
import com.example.data.model.PendingPaymentOrderDraft
import com.razorpay.Checkout
import org.json.JSONObject
import timber.log.Timber

/**
 * Production-ready Razorpay Checkout Bridge for Good Dream Luxury Home Decor.
 * Supports Turbo Native UPI Intent (GPay, PhonePe, Paytm), Credit/Debit Cards,
 * Netbanking, and No-Cost EMI with bank-grade 256-bit encryption.
 */
object RazorpayPaymentHelper {

    fun preload(activity: Activity) {
        try {
            Checkout.preload(activity.applicationContext)
        } catch (e: Exception) {
            Timber.e(e, "Failed to preload Razorpay Checkout")
        }
    }

    internal fun buildPaymentOptions(
        orderDraft: PendingPaymentOrderDraft,
        cartSummary: String
    ): JSONObject {
        val amountInPaise = (orderDraft.payableAmount * 100).toLong().coerceAtLeast(100L)

        return JSONObject().apply {
            put("name", "Good Dream Home Decor")
            val desc = if (orderDraft.isCod) {
                "20% Delivery Booking Advance"
            } else {
                "Sanctuary Sleep Suite: " + cartSummary.take(120)
            }
            put("description", desc)
            put("image", "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=300")
            put("theme.color", "#102E23")
            put("currency", "INR")
            put("amount", amountInPaise)

            val prefill = JSONObject().apply {
                put("email", orderDraft.customerEmail.ifBlank { "concierge@gooddream.in" })
                put("contact", orderDraft.customerPhone.ifBlank { "9876543210" })
                put("name", orderDraft.customerName.ifBlank { "Sanctuary Member" })
            }
            put("prefill", prefill)

            val retryObj = JSONObject().apply {
                put("enabled", true)
                put("max_count", 2)
            }
            put("retry", retryObj)

            val modalObj = JSONObject().apply {
                put("confirm_close", true)
            }
            put("modal", modalObj)
        }
    }

    /**
     * Resolves the active Razorpay Key ID based on the following priority hierarchy:
     * 1. Firebase Remote Config (`razorpay_key_id`)
     * 2. Firestore Global Config (`AppConfig.razorpayKeyId`)
     * 3. BuildConfig / .env variable (`BuildConfig.RAZORPAY_KEY_ID`)
     * 4. Offline Sandbox Test Fallback (`"rzp_test_51gX7Y8Z9abcde"`)
     */
    fun getEffectiveKeyId(): String {
        // Priority 1: Firebase Remote Config parameter
        val remoteConfigKey = FirebaseRemoteConfigHelper.getRazorpayKeyId()
        if (remoteConfigKey.isNotBlank() && remoteConfigKey.startsWith("rzp_")) {
            val prefix = if (remoteConfigKey.length > 8) remoteConfigKey.take(8) + "..." else remoteConfigKey
            Timber.d("Active Razorpay Key resolved from Firebase Remote Config: %s", prefix)
            return remoteConfigKey
        }

        // Priority 2: Realtime Firestore AppConfig document
        val firestoreKey = AppConfigProvider.configState.value.razorpayKeyId.trim()
        if (firestoreKey.isNotBlank() && firestoreKey.startsWith("rzp_")) {
            val prefix = if (firestoreKey.length > 8) firestoreKey.take(8) + "..." else firestoreKey
            Timber.d("Active Razorpay Key resolved from Firestore AppConfig: %s", prefix)
            return firestoreKey
        }

        // Priority 3: Local build-time environment variable (.env)
        val buildConfigKey = BuildConfig.RAZORPAY_KEY_ID.trim()
        if (buildConfigKey.isNotBlank() && !buildConfigKey.startsWith("MY_") && !buildConfigKey.contains("placeholder", ignoreCase = true)) {
            val prefix = if (buildConfigKey.length > 8) buildConfigKey.take(8) + "..." else buildConfigKey
            Timber.d("Active Razorpay Key resolved from BuildConfig: %s", prefix)
            return buildConfigKey
        }

        // Priority 4: Offline sandbox test fallback
        Timber.w("No external Razorpay Key configured; falling back to active test key")
        return "rzp_test_TfYEXhy3Yk78zc"
    }

    fun startPayment(
        activity: Activity,
        orderDraft: PendingPaymentOrderDraft,
        cartSummary: String
    ) {
        val checkout = Checkout()
        val effectiveKey = getEffectiveKeyId()
        checkout.setKeyID(effectiveKey)

        try {
            val options = buildPaymentOptions(orderDraft, cartSummary)
            checkout.open(activity, options)
        } catch (e: Exception) {
            Timber.e(e, "Error opening Razorpay checkout: " + e.message)
            throw e
        }
    }
}
