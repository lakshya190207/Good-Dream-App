package com.example.data.remote

import com.example.data.local.UserSessionManager
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Service handling Firebase Cloud Messaging lifecycle, device registration tokens,
 * and incoming downstream push notifications for Good Dream Sanctuary.
 */
class SanctuaryFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Sanctuary FCM Token Refreshed successfully (token length: %d)", token.length)

        try {
            val sessionManager = UserSessionManager(applicationContext)
            sessionManager.saveFcmToken(token)
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist refreshed FCM token")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM push received from: %s", remoteMessage.from)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Good Dream Sanctuary"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New order delivery update."

        NotificationHelper.showRemoteNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = remoteMessage.data
        )
    }
}
