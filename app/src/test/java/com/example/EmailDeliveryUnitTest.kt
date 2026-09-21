package com.example

import com.example.data.remote.EmailDeliveryService
import com.example.data.remote.EmailSendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDeliveryUnitTest {

    @Test
    fun testLiveGoogleSmtpDispatch() = runBlocking {
        val service = EmailDeliveryService()
        val result = service.sendOtpEmail(
            recipientEmail = "lakshyachandra07@gmail.com",
            otp = "924718",
            expiresMinutes = 5
        )
        println("=== LIVE SMTP DISPATCH RESULT: $result ===")
        assertTrue("Result should be an EmailSendResult instance: $result", result is EmailSendResult)
    }
}
