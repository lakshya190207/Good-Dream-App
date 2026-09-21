package com.example

import com.example.data.model.PendingPaymentOrderDraft
import com.example.util.RazorpayPaymentHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RazorpayPaymentUnitTest {

    @Test
    fun testFullOnlinePaymentOptionsBuilding() {
        val draft = PendingPaymentOrderDraft(
            customerName = "Lord Sterling",
            customerPhone = "9876543210",
            customerEmail = "sterling@luxury.in",
            deliveryAddress = "Penthouse 4B, Sky Villa",
            city = "Bengaluru",
            state = "Karnataka",
            pincode = "560001",
            deliverySlot = "Evening (4 PM - 8 PM)",
            floorElevator = "Ground Floor",
            paymentCategory = "ONLINE",
            paymentMethodDetail = "UPI (Instant)",
            payableAmount = 45000.0,
            isCod = false
        )

        val options = RazorpayPaymentHelper.buildPaymentOptions(draft, "Bespoke Royal Velvet Bed (1x)")

        assertEquals("Good Dream Home Decor", options.getString("name"))
        assertEquals("INR", options.getString("currency"))
        assertEquals("#102E23", options.getString("theme.color"))
        assertEquals(4500000L, options.getLong("amount"))
        assertTrue(options.getString("description").contains("Sanctuary Sleep Suite"))

        val prefill = options.getJSONObject("prefill")
        assertEquals("sterling@luxury.in", prefill.getString("email"))
        assertEquals("9876543210", prefill.getString("contact"))
        assertEquals("Lord Sterling", prefill.getString("name"))

        val retry = options.getJSONObject("retry")
        assertTrue(retry.getBoolean("enabled"))
        assertEquals(2, retry.getInt("max_count"))

        val modal = options.getJSONObject("modal")
        assertTrue(modal.getBoolean("confirm_close"))
    }

    @Test
    fun testCodAdvancePaymentOptionsBuilding() {
        val total = 100000.0
        val advance = total * 0.20
        val remaining = total - advance

        val draft = PendingPaymentOrderDraft(
            customerName = "Lady Genevieve",
            customerPhone = "9811122334",
            customerEmail = "genevieve@luxury.in",
            deliveryAddress = "Estate No. 7, Heritage Hills",
            city = "New Delhi",
            state = "Delhi",
            pincode = "110001",
            deliverySlot = "Morning (9 AM - 1 PM)",
            floorElevator = "Service Elevator Available",
            paymentCategory = "COD",
            paymentMethodDetail = "Cash On Delivery (20% Advance via Razorpay)",
            payableAmount = advance,
            isCod = true,
            codAdvanceAmount = advance,
            codBalanceAmount = remaining
        )

        val options = RazorpayPaymentHelper.buildPaymentOptions(draft, "Grand Sovereign Mattress (1x)")

        assertEquals(2000000L, options.getLong("amount"))
        assertEquals("20% White-Glove Booking Advance", options.getString("description"))
    }

    @Test
    fun testMinimumPaiseClamp() {
        val draft = PendingPaymentOrderDraft(
            customerName = "",
            customerPhone = "",
            customerEmail = "",
            deliveryAddress = "Test Address",
            city = "Mumbai",
            state = "Maharashtra",
            pincode = "400001",
            deliverySlot = "Afternoon (1 PM - 4 PM)",
            floorElevator = "Ground Floor",
            paymentCategory = "ONLINE",
            paymentMethodDetail = "UPI",
            payableAmount = 0.5,
            isCod = false
        )

        val options = RazorpayPaymentHelper.buildPaymentOptions(draft, "Swatch Sample")
        assertEquals(100L, options.getLong("amount"))

        val prefill = options.getJSONObject("prefill")
        assertEquals("concierge@gooddream.in", prefill.getString("email"))
        assertEquals("9876543210", prefill.getString("contact"))
        assertEquals("Sanctuary Member", prefill.getString("name"))
    }
}
