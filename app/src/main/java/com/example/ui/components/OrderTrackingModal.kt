package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

data class DeliveryStep(
    val stageNumber: Int,
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@Composable
fun OrderTrackingModal(
    initialOrderRef: String? = null,
    orders: List<com.example.data.model.OrderEntity> = emptyList(),
    inquiries: List<com.example.data.model.InquiryEntity> = emptyList(),
    isUserLoggedIn: Boolean = false,
    loggedInUserEmail: String? = null,
    isAdmin: Boolean = false,
    supportPhone: String = "+91 7014983696",
    onOpenLiveChat: (String) -> Unit = {},
    onOpenLogin: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultRef = initialOrderRef ?: orders.firstOrNull()?.id ?: inquiries.firstOrNull()?.referenceNumber ?: ""
    var trackingInput by remember(initialOrderRef, orders, inquiries) { mutableStateOf(defaultRef) }
    var activeTrackingId by remember(initialOrderRef, orders, inquiries) { mutableStateOf(defaultRef) }
    val scrollState = rememberScrollState()

    val matchedOrder = remember(activeTrackingId, orders) {
        if (activeTrackingId.isBlank()) null
        else orders.find { it.id.equals(activeTrackingId, ignoreCase = true) }
    }

    val matchedInquiry = remember(activeTrackingId, inquiries) {
        if (activeTrackingId.isBlank()) null
        else inquiries.find { it.referenceNumber.equals(activeTrackingId, ignoreCase = true) }
    }

    val isUnauthorizedAccess = remember(activeTrackingId, matchedOrder, matchedInquiry, isUserLoggedIn, loggedInUserEmail, isAdmin) {
        if (isAdmin || activeTrackingId.isBlank()) {
            false
        } else if (!isUserLoggedIn) {
            // Unauthenticated lookup of specific IDs is gated
            matchedOrder != null || matchedInquiry != null
        } else {
            val orderEmail = matchedOrder?.customerEmail
            val inqEmail = matchedInquiry?.customerEmail
            val orderMismatch = matchedOrder != null && !orderEmail.isNullOrBlank() && !orderEmail.equals(loggedInUserEmail, ignoreCase = true)
            val inqMismatch = matchedInquiry != null && !inqEmail.isNullOrBlank() && !inqEmail.equals(loggedInUserEmail, ignoreCase = true)
            orderMismatch || inqMismatch
        }
    }

    val steps = remember(activeTrackingId, matchedOrder, matchedInquiry) {
        if (matchedInquiry != null) {
            val status = matchedInquiry.status
            listOf(
                DeliveryStep(
                    stageNumber = 1,
                    title = "Request Received & Reference Generated",
                    description = "Inquiry logged into Good Dream Master Artisan & Client Care database.",
                    timestamp = "Received",
                    isCompleted = true,
                    isCurrent = status == "Received"
                ),
                DeliveryStep(
                    stageNumber = 2,
                    title = "Master Craftsman & Care Review",
                    description = "Technical specifications, warranty eligibility, or repair assessment underway.",
                    timestamp = if (status in listOf("In Review", "Contacted", "Resolved")) "Active" else "Pending",
                    isCompleted = status in listOf("In Review", "Contacted", "Resolved"),
                    isCurrent = status == "In Review"
                ),
                DeliveryStep(
                    stageNumber = 3,
                    title = "Client Contact & Consultation Call",
                    description = "Direct outreach via phone/email to confirm bespoke details or schedule service.",
                    timestamp = if (status in listOf("Contacted", "Resolved")) "Completed" else "Scheduled",
                    isCompleted = status in listOf("Contacted", "Resolved"),
                    isCurrent = status == "Contacted"
                ),
                DeliveryStep(
                    stageNumber = 4,
                    title = "Service Fulfillment & Certificate Issuance",
                    description = "Custom piece handcrafting, warranty registration, or repair completion finalized.",
                    timestamp = if (status == "Resolved") "Completed" else "In Progress",
                    isCompleted = status == "Resolved",
                    isCurrent = status == "Resolved"
                )
            )
        } else {
            val orderTime = matchedOrder?.createdAt ?: System.currentTimeMillis()
            val orderStatus = matchedOrder?.status?.lowercase() ?: "confirmed"
            val stageIdx = when {
                orderStatus in listOf("delivered", "resolved", "completed") -> 4
                orderStatus in listOf("dispatched", "fleet transit", "transit") -> 3
                orderStatus in listOf("crafting", "atelier crafting", "in review") -> 2
                else -> 1
            }

            val dateFormatter = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.ENGLISH)
            val date1 = dateFormatter.format(java.util.Date(orderTime))
            val date2 = dateFormatter.format(java.util.Date(orderTime + 24L * 3600 * 1000))
            val date3 = dateFormatter.format(java.util.Date(orderTime + 48L * 3600 * 1000))
            val date4 = dateFormatter.format(java.util.Date(orderTime + 96L * 3600 * 1000))

            listOf(
                DeliveryStep(
                    stageNumber = 1,
                    title = "Order Confirmed & Specifications Verified",
                    description = "Custom mattress dimensions and orthopedic layer specs verified by master craftsman.",
                    timestamp = date1,
                    isCompleted = stageIdx >= 1,
                    isCurrent = stageIdx == 1
                ),
                DeliveryStep(
                    stageNumber = 2,
                    title = "Atelier Crafting & Pressure Mapping",
                    description = "Pocket spring alignment, high-resilience foam layering & organic cotton stitching completed.",
                    timestamp = if (stageIdx >= 2) date2 else "Est: $date2",
                    isCompleted = stageIdx >= 2,
                    isCurrent = stageIdx == 2
                ),
                DeliveryStep(
                    stageNumber = 3,
                    title = "Dispatched via Climate-Regulated Fleet",
                    description = "Loaded in climate-monitored, zero-compression transport vehicles in pristine protective wraps.",
                    timestamp = if (stageIdx >= 3) date3 else "Est: $date3",
                    isCompleted = stageIdx >= 3,
                    isCurrent = stageIdx == 3
                ),
                DeliveryStep(
                    stageNumber = 4,
                    title = "Doorstep Delivery & Room-of-Choice Assembly",
                    description = "Two-person certified installation team positions your mattress and removes legacy packaging.",
                    timestamp = if (stageIdx >= 4) date4 else "Est: $date4",
                    isCompleted = stageIdx >= 4,
                    isCurrent = stageIdx == 4
                )
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("order_tracking_dialog"),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Order Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Live updates from factory to doorstep assembly",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Search Reference Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Enter Order or Inquiry Reference ID",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = trackingInput,
                                    onValueChange = { trackingInput = it },
                                    placeholder = { Text("e.g. GD-8492") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { activeTrackingId = trackingInput.trim() })
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { activeTrackingId = trackingInput.trim() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Track")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Sign-in prompt for guest visitors
                    if (!isUserLoggedIn && !isAdmin) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Sign In to Access Your Active Orders & Claims",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "In accordance with Good Dream client data protection standards, detailed tracking and certificates are isolated to verified members.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onOpenLogin,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                                ) {
                                    Text("Sign In to My Account →", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }

                    // Unauthorized Access Warning Banner
                    if (isUnauthorizedAccess) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Privacy Protected: Access Restricted",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isUserLoggedIn) {
                                        "This reference ID is registered to another customer account. You can only view and track records associated with your verified account (${loggedInUserEmail ?: ""})."
                                    } else {
                                        "Detailed tracking for this reference is protected under customer privacy. Please sign in with the registered email account to view this record."
                                    },
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                if (!isUserLoggedIn) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = onOpenLogin,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                                    ) {
                                        Text("Sign In to Verify Account →", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Status Summary Banner (Shown only if authorized)
                    if (!isUnauthorizedAccess && matchedOrder != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Live Placed Order Verified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SatinGoldAccent
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Customer: ${matchedOrder.customerName} • ${matchedOrder.city}, ${matchedOrder.state}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Items: ${matchedOrder.itemsSummary}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Status: ${matchedOrder.status} • Payment: ${matchedOrder.paymentMethod}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (!isUnauthorizedAccess && matchedInquiry != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${matchedInquiry.type} Request [#${matchedInquiry.referenceNumber}]",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SatinGoldAccent
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (matchedInquiry.status) {
                                            "Resolved" -> ForestGreenContainer
                                            "Contacted" -> Color(0xFFFEF3C7)
                                            "In Review" -> Color(0xFFE0F2FE)
                                            else -> SatinGoldContainer
                                        }
                                    ) {
                                        Text(
                                            text = matchedInquiry.status.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = when (matchedInquiry.status) {
                                                "Resolved" -> ForestGreenDark
                                                "Contacted" -> Color(0xFFB45309)
                                                "In Review" -> Color(0xFF0369A1)
                                                else -> SatinGoldDark
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Client: ${matchedInquiry.customerName} • ${matchedInquiry.customerPhone}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (matchedInquiry.categoryOrProduct.isNotBlank()) {
                                    Text(
                                        text = "Ref / Serial: ${matchedInquiry.categoryOrProduct}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Details: ${matchedInquiry.details}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!isUnauthorizedAccess) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = SatinGoldAccent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tracking: ${if (activeTrackingId.isNotBlank()) activeTrackingId else "Enter Reference"}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (matchedInquiry != null) "Inquiry Status: ${matchedInquiry.status}"
                                        else if (matchedOrder != null) "Order Status: ${matchedOrder.status}"
                                        else "Awaiting reference query",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // 4-Stage Stepper
                        Text(
                            text = "Fulfillment Milestones",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))

                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Indicator column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (step.isCompleted) MaterialTheme.colorScheme.primary
                                                else if (step.isCurrent) SatinGoldAccent
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (step.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${step.stageNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (step.isCurrent) TextPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (index < steps.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(48.dp)
                                                .background(
                                                    if (step.isCompleted) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                // Content column
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (step.isCurrent || step.isCompleted) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (step.isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = step.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (step.isCurrent) SatinGoldDark else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = step.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (matchedOrder != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_tracking_tax_invoice"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🏛️ Official GST Tax Invoice & Warranty",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Includes 25-Year Authenticity Certificate and itemized HSN 9404 tax breakdown.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        com.example.util.InvoicePrinterHelper.printOrderInvoice(
                                            context = context,
                                            order = matchedOrder
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Print / Save Tax Invoice (PDF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    // Contact Delivery Support Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Need Delivery Schedule Modifications?",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Call our dedicated logistics coordinator directly for time-slot preferences.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Call Coordinator", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val url = "https://api.whatsapp.com/send?phone=+917014983696&text=Inquiry%20regarding%20delivery%20schedule%20for%20order%20$activeTrackingId"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                            context.startActivity(dialIntent)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("WhatsApp", fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { onOpenLiveChat(activeTrackingId) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Live Chat with Care Specialist", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
}
