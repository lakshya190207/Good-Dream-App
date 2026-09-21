package com.example

import com.example.data.local.SavedAddress
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class CrmUserSyncUnitTest {

    @Test
    fun testCrmUserRecordCreation() {
        val user = CrmUserRecord(
            id = "vip@gooddream.in",
            name = "Aarav Sharma",
            email = "vip@gooddream.in",
            phone = "+91 98765 43210",
            userType = CrmUserType.REGISTERED_VIP,
            totalOrders = 3,
            lifetimeSpend = 125000.0,
            lastActivityEpochMs = 1710000000000L,
            addresses = listOf(
                SavedAddress(
                    id = "addr_1",
                    tag = "Home Villa",
                    fullName = "Aarav Sharma",
                    phoneNumber = "+91 98765 43210",
                    flatHouseNo = "Villa 42, Palm Meadows",
                    streetLocality = "Whitefield",
                    city = "Bengaluru",
                    state = "Karnataka",
                    pincode = "560066",
                    isDefault = true
                )
            ),
            notes = "Prefers ultra-firm pocket springs."
        )

        assertEquals("vip@gooddream.in", user.id)
        assertEquals("Aarav Sharma", user.name)
        assertEquals(CrmUserType.REGISTERED_VIP, user.userType)
        assertEquals(3, user.totalOrders)
        assertEquals(125000.0, user.lifetimeSpend, 0.01)
        assertEquals(1, user.addresses.size)
        assertEquals("Karnataka", user.addresses.first().state)
        assertEquals("Prefers ultra-firm pocket springs.", user.notes)
    }

    @Test
    fun testCrmUserAggregationAndLtvCalculation() {
        // 1. Mock Registered Users
        val registeredUsers = listOf(
            CrmUserRecord(
                id = "priya@gooddream.in",
                name = "Priya Nair",
                email = "priya@gooddream.in",
                phone = "+91 91234 56789",
                userType = CrmUserType.REGISTERED_VIP,
                notes = "High-priority architect client"
            )
        )

        // 2. Mock Orders
        val orders = listOf(
            OrderEntity(
                id = "ORD-001",
                customerName = "Priya Nair",
                customerPhone = "+91 91234 56789",
                customerEmail = "priya@gooddream.in",
                deliveryAddress = "Penthouse 12B, Marine Drive",
                city = "Mumbai",
                state = "Maharashtra",
                pincode = "400020",
                deliverySlot = "Morning (9 AM - 1 PM)",
                floorElevator = "Elevator Available",
                paymentMethod = "Razorpay (UPI / NetBanking)",
                paymentStatus = "Paid",
                totalAmount = 65000.0,
                itemsSummary = "1x SpringHaven Grand Royale King",
                status = "Delivered",
                createdAt = 1000L
            ),
            OrderEntity(
                id = "ORD-002",
                customerName = "Priya Nair",
                customerPhone = "+91 91234 56789",
                customerEmail = "priya@gooddream.in",
                deliveryAddress = "Penthouse 12B, Marine Drive",
                city = "Mumbai",
                state = "Maharashtra",
                pincode = "400020",
                deliverySlot = "Evening (4 PM - 8 PM)",
                floorElevator = "Elevator Available",
                paymentMethod = "Razorpay (UPI / NetBanking)",
                paymentStatus = "Paid",
                totalAmount = 45000.0,
                itemsSummary = "2x Orthopedic Contour Pillows",
                status = "Processing",
                createdAt = 2000L
            ),
            OrderEntity(
                id = "ORD-003",
                customerName = "Rohit Verma",
                customerPhone = "+91 98888 11111",
                customerEmail = "rohit@guest.com",
                deliveryAddress = "104 Emerald Heights",
                city = "Pune",
                state = "Maharashtra",
                pincode = "411001",
                deliverySlot = "Anytime",
                floorElevator = "Stairs (2nd Floor)",
                paymentMethod = "Cash on Delivery",
                paymentStatus = "Pending",
                totalAmount = 32000.0,
                itemsSummary = "1x Celestial Serenity Mattress",
                status = "Placed",
                createdAt = 1500L
            )
        )

        // 3. Mock Inquiries
        val inquiries = listOf(
            InquiryEntity(
                id = 1L,
                type = "NEEDS",
                referenceNumber = "INQ-001",
                customerName = "Sneha Roy",
                customerPhone = "+91 97777 22222",
                customerEmail = "sneha@interior.com",
                categoryOrProduct = "Hotel Suite",
                details = "Need quote for 25 king mattresses for bulk hotel project",
                createdAt = 1800L
            )
        )

        // Run merge logic matching GoodDreamViewModel.syncCrmUsers()
        val ordersByEmail = orders.filter { it.customerEmail.isNotBlank() }
            .groupBy { it.customerEmail.trim().lowercase() }
        val inquiriesByEmail = inquiries.filter { it.customerEmail.isNotBlank() }
            .groupBy { it.customerEmail.trim().lowercase() }

        val mergedMap = mutableMapOf<String, CrmUserRecord>()

        // 1. Registered VIPs
        registeredUsers.forEach { regUser ->
            val emailKey = regUser.email.trim().lowercase()
            val userOrders = ordersByEmail[emailKey] ?: emptyList()
            val totalSpend = userOrders.sumOf { it.totalAmount }
            val lastOrder = userOrders.maxByOrNull { it.createdAt }
            val latestActivity = maxOf(regUser.lastActivityEpochMs, lastOrder?.createdAt ?: 0L)

            mergedMap[emailKey] = regUser.copy(
                totalOrders = userOrders.size,
                lifetimeSpend = totalSpend,
                lastActivityEpochMs = latestActivity
            )
        }

        // 2. Order Clients (guest checkout)
        ordersByEmail.forEach { (emailKey, userOrders) ->
            if (!mergedMap.containsKey(emailKey)) {
                val latestOrder = userOrders.maxByOrNull { it.createdAt } ?: userOrders.first()
                val totalSpend = userOrders.sumOf { it.totalAmount }
                mergedMap[emailKey] = CrmUserRecord(
                    id = emailKey,
                    name = latestOrder.customerName,
                    email = emailKey,
                    phone = latestOrder.customerPhone,
                    userType = CrmUserType.ORDER_CLIENT,
                    totalOrders = userOrders.size,
                    lifetimeSpend = totalSpend,
                    lastActivityEpochMs = latestOrder.createdAt
                )
            }
        }

        // 3. Inquiry Leads
        inquiriesByEmail.forEach { (emailKey, userInquiries) ->
            if (!mergedMap.containsKey(emailKey)) {
                val latestInquiry = userInquiries.maxByOrNull { it.createdAt } ?: userInquiries.first()
                mergedMap[emailKey] = CrmUserRecord(
                    id = emailKey,
                    name = latestInquiry.customerName,
                    email = emailKey,
                    phone = latestInquiry.customerPhone,
                    userType = CrmUserType.INQUIRY_LEAD,
                    totalOrders = 0,
                    lifetimeSpend = 0.0,
                    lastActivityEpochMs = latestInquiry.createdAt,
                    notes = "Inquiry [${latestInquiry.type}]: ${latestInquiry.details.take(60)}"
                )
            }
        }

        val sorted = mergedMap.values.sortedWith(
            compareByDescending<CrmUserRecord> { it.lifetimeSpend }
                .thenByDescending { it.lastActivityEpochMs }
        )

        assertEquals(3, sorted.size)

        // Priya Nair should be #1 because lifetime spend = 65,000 + 45,000 = 110,000
        val priya = sorted[0]
        assertEquals("priya@gooddream.in", priya.email)
        assertEquals(CrmUserType.REGISTERED_VIP, priya.userType)
        assertEquals(2, priya.totalOrders)
        assertEquals(110000.0, priya.lifetimeSpend, 0.01)
        assertEquals(2000L, priya.lastActivityEpochMs)

        // Rohit Verma should be #2 (spend = 32,000)
        val rohit = sorted[1]
        assertEquals("rohit@guest.com", rohit.email)
        assertEquals(CrmUserType.ORDER_CLIENT, rohit.userType)
        assertEquals(1, rohit.totalOrders)
        assertEquals(32000.0, rohit.lifetimeSpend, 0.01)

        // Sneha Roy should be #3 (Inquiry lead, 0 spend)
        val sneha = sorted[2]
        assertEquals("sneha@interior.com", sneha.email)
        assertEquals(CrmUserType.INQUIRY_LEAD, sneha.userType)
        assertEquals(0, sneha.totalOrders)
        assertTrue(sneha.notes.contains("bulk hotel project", ignoreCase = true))
    }

    @Test
    fun testCrmUserFiltering() {
        val users = listOf(
            CrmUserRecord(
                id = "u1",
                name = "Aarav Sharma",
                email = "aarav@gmail.com",
                phone = "9876543210",
                userType = CrmUserType.REGISTERED_VIP
            ),
            CrmUserRecord(
                id = "u2",
                name = "Kavita Rao",
                email = "kavita@yahoo.com",
                phone = "9123456780",
                userType = CrmUserType.ORDER_CLIENT
            ),
            CrmUserRecord(
                id = "u3",
                name = "Devansh Patel",
                email = "devansh@outlook.com",
                phone = "9898989898",
                userType = CrmUserType.INQUIRY_LEAD
            )
        )

        // Query by search string "kavita"
        val query = "kavita"
        val searched = users.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.email.contains(query, ignoreCase = true) ||
            it.phone.contains(query)
        }
        assertEquals(1, searched.size)
        assertEquals("u2", searched.first().id)

        // Filter by user type "Registered VIP"
        val vips = users.filter { it.userType == CrmUserType.REGISTERED_VIP }
        assertEquals(1, vips.size)
        assertEquals("u1", vips.first().id)

        // Filter by user type "Inquiry Lead"
        val leads = users.filter { it.userType == CrmUserType.INQUIRY_LEAD }
        assertEquals(1, leads.size)
        assertEquals("u3", leads.first().id)
    }
}
