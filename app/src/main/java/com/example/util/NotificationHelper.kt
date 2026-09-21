package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import timber.log.Timber

/**
 * Production-grade notification manager configuring Android 8.0+ notification channels,
 * rich White-Glove order stage notifications, and deep link intents.
 */
object NotificationHelper {

    const val CHANNEL_ID_ORDERS = "sanctuary_orders_channel"
    const val CHANNEL_ID_PRIVILEGES = "sanctuary_privileges_channel"

    const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
    const val ACTION_OPEN_ORDER = "com.example.ACTION_OPEN_ORDER"

    /**
     * Idempotently creates system notification channels.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: White-Glove Order Tracking (High Importance)
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

            notificationManager.createNotificationChannels(listOf(ordersChannel, privilegesChannel))
            Timber.d("Sanctuary notification channels registered")
        }
    }

    /**
     * Dispatches an interactive White-Glove delivery stage notification.
     */
    fun showOrderStageNotification(
        context: Context,
        orderId: String,
        stageTitle: String,
        stageDescription: String,
        notificationId: Int = (1000..9999).random()
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
            .setContentTitle("White-Glove Delivery • $stageTitle")
            .setContentText(stageDescription)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🏛️ Good Dream Sanctuary • $stageTitle")
                    .bigText("$stageDescription\n\nOrder ID: $orderId")
                    .setSummaryText("Order Milestone")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(if (orderId != null) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify((2000..9999).random(), notification)
    }
}
