package com.example

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserSessionManager
import com.example.util.NotificationHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationPushUnitTest {

    private lateinit var context: Context
    private lateinit var sessionManager: UserSessionManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = UserSessionManager(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testNotificationChannelsCreation() {
        NotificationHelper.createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ordersChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_ORDERS)
            assertNotNull("Orders notification channel should be registered", ordersChannel)
            assertEquals("sanctuary_orders_channel", ordersChannel?.id)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, ordersChannel?.importance)

            val privilegesChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_PRIVILEGES)
            assertNotNull("Privileges notification channel should be registered", privilegesChannel)
            assertEquals("sanctuary_privileges_channel", privilegesChannel?.id)
        }
    }

    @Test
    fun testUserSessionManagerFcmTokenPersistence() {
        val testToken = "fcm_test_device_token_xyz_12345"
        sessionManager.saveFcmToken(testToken)

        val retrievedToken = sessionManager.getFcmToken()
        assertEquals(testToken, retrievedToken)
    }

    @Test
    fun testShowOrderStageNotification() {
        NotificationHelper.createNotificationChannels(context)

        val testNotificationId = 1234
        // Stage 0: Placed
        NotificationHelper.showOrderStageNotification(
            context = context,
            orderId = "ORD-TEST-9988",
            stageTitle = "Order Reserved & Confirmed",
            stageDescription = "Your bespoke Grand Sovereign Luxury Mattress is confirmed.",
            notificationId = testNotificationId
        )

        val activeNotifications = notificationManager.activeNotifications
        assertTrue("Notification should be posted to NotificationManager", activeNotifications.isNotEmpty())
        val matchedNotification = activeNotifications.find { it.id == testNotificationId }
        assertNotNull("Posted notification should have matching notification ID", matchedNotification)
        assertEquals(NotificationHelper.CHANNEL_ID_ORDERS, matchedNotification?.notification?.channelId)
    }
}
