package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.data.local.SavedAddress

enum class CrmUserType(val label: String) {
    REGISTERED_VIP("Registered VIP"),
    ORDER_CLIENT("Order Client"),
    INQUIRY_LEAD("Inquiry Lead"),
    ADMIN("Administrator")
}

@Immutable
data class CrmUserRecord(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val userType: CrmUserType,
    val totalOrders: Int = 0,
    val lifetimeSpend: Double = 0.0,
    val lastActivityEpochMs: Long = 0L,
    val addresses: List<SavedAddress> = emptyList(),
    val notes: String = ""
)
