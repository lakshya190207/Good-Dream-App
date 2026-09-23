package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import timber.log.Timber

/**
 * Production-grade notification manager configuring Android 8.0+ notification channels,
 * rich order stage notifications, and deep link intents.
 */
object NotificationHelper {

    const val CHANNEL_ID_ORDERS = "sanctuary_orders_channel"
    const val CHANNEL_ID_PRIVILEGES = "sanctuary_privileges_channel"
    const val CHANNEL_ID_SUPPORT_CHAT = "sanctuary_support_chat_channel"

    const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
    const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
    const val ACTION_OPEN_ORDER = "com.example.ACTION_OPEN_ORDER"
    const val ACTION_OPEN_CHAT = "com.example.ACTION_OPEN_CHAT"

    /**
     * Idempotently creates system notification channels.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Order Tracking (High Importance)
            val ordersChannel = NotificationChannel(
                CHANNEL_ID_ORDERS,
                "Sanctuary Orders & Delivery",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live updates on artisan crafting, climate-controlled fleet transit, and in-room setup appointments"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: Privileges & Collection Announcements (Default Importance)
            val privilegesChannel = NotificationChannel(
                CHANNEL_ID_PRIVILEGES,
                "Sanctuary Privileges & Collections",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Member-exclusive invitations, anniversary privileges, and new launch previews"
                setShowBadge(true)
            }

            // Channel 3: Live Concierge Direct Messages & Customer Support (High Importance)
            val supportChatChannel = NotificationChannel(
                CHANNEL_ID_SUPPORT_CHAT,
                "Sanctuary Concierge Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct replies and personal messages from Good Dream executive concierge specialists"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(ordersChannel, privilegesChannel, supportChatChannel))
            Timber.d("Sanctuary notification channels registered")
        }
    }

    /**
     * Dispatches an interactive delivery stage notification.
     */
    fun showOrderStageNotification(
        context: Context,
        orderId: String,
        stageTitle: String,
        stageDescription: String,
        notificationId: Int = (orderId.hashCode() and 0x7FFFFFFF)
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ORDER
            putExtra(EXTRA_ORDER_ID, orderId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forestGreenColor = ContextCompat.getColor(context, R.color.forest_green_luxury)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ORDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(forestGreenColor)
            .setContentTitle("Order Delivery • $stageTitle")
            .setContentText(stageDescription)
            .setSubText("Good Dream Sanctuary")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Good Dream Sanctuary • $stageTitle")
                    .bigText("$stageDescription\n\nOrder ID: $orderId")
                    .setSummaryText("Order Milestone")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        safeNotify(context, notificationId, notification)
    }

    /**
     * Dispatches an incoming remote FCM notification.
     */
    fun showRemoteNotification(
        context: Context,
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        createNotificationChannels(context)

        val orderId = data["order_id"]
        val targetChannel = if (orderId != null) CHANNEL_ID_ORDERS else CHANNEL_ID_PRIVILEGES

        val intent = Intent(context, MainActivity::class.java).apply {
            if (orderId != null) {
                action = ACTION_OPEN_ORDER
                putExtra(EXTRA_ORDER_ID, orderId)
            }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (title?.hashCode() ?: 0) + (body?.hashCode() ?: 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forestGreenColor = ContextCompat.getColor(context, R.color.forest_green_luxury)

        val notification = NotificationCompat.Builder(context, targetChannel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(forestGreenColor)
            .setContentTitle(title ?: "Good Dream Sanctuary")
            .setContentText(body ?: "You have a new update from Good Dream.")
            .setSubText("Good Dream Sanctuary")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(if (orderId != null) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        safeNotify(context, (2000..9999).random(), notification)
    }

    /**
     * Dispatches an interactive direct message notification from the administrator / concierge to the user.
     * Tapping the notification opens MainActivity with EXTRA_CHAT_ID, immediately launching
     * into the real-time Live Support Chat modal so the customer can reply.
     */
    fun showAdminDirectMessageNotification(
        context: Context,
        chatId: String,
        customerName: String,
        adminMessage: String,
        orderReference: String? = null
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CHAT
            putExtra(EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forestGreenColor = ContextCompat.getColor(context, R.color.forest_green_luxury)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SUPPORT_CHAT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(forestGreenColor)
            .setContentTitle("Good Dream Concierge • Direct Message")
            .setContentText(adminMessage)
            .setSubText("Good Dream Concierge")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Good Dream Concierge • $customerName")
                    .bigText(adminMessage + if (!orderReference.isNullOrBlank()) "\n\nRegarding Order: $orderReference" else "")
                    .setSummaryText("Live Concierge Desk")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = (chatId.hashCode() and 0x7FFFFFFF)
        safeNotify(context, notifId, notification)
        Timber.i("Direct message notification posted for chat %s (notifId: %d)", chatId, notifId)
    }

    private fun safeNotify(context: Context, notificationId: Int, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Throwable) {
            Timber.w(e, "Safe notification dispatch failed for id %d", notificationId)
        }
    }
}
