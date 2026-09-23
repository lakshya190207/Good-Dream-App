package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveModal

@Composable
fun GoodDreamModalDispatcher(
    activeModal: ActiveModal,
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    recentInquiryRef: String?,
    appConfigs: Map<String, String>,
    onClose: () -> Unit,
    onSubmitInquiry: (String, String, String, String, String, String) -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateConfig: (String, String) -> Unit,
    onResetCatalog: () -> Unit
) {
    when (activeModal) {
        ActiveModal.NONE -> {}

        ActiveModal.AI_CHAT_BOT -> {
            // Handled directly at top-level dialog in MainActivity
        }

        ActiveModal.YOUR_NEEDS -> {
            YourNeedsModal(
                categories = categories,
                onSubmit = { name, phone, email, cat, details ->
                    onSubmitInquiry("NEEDS", name, phone, email, cat, details)
                },
                onClose = onClose
            )
        }

        ActiveModal.REPAIRS -> {
            RepairsModal(
                onSubmit = { name, phone, email, prod, details ->
                    onSubmitInquiry("REPAIR", name, phone, email, prod, details)
                },
                onClose = onClose
            )
        }

        ActiveModal.COMPLAINTS -> {
            ComplaintsModal(
                onSubmit = { name, phone, email, prod, details ->
                    onSubmitInquiry("COMPLAINT", name, phone, email, prod, details)
                },
                onClose = onClose
            )
        }

        ActiveModal.SERVICE_WARRANTIES -> {
            ServiceWarrantiesModal(
                onClose = onClose
            )
        }

        ActiveModal.SPONSOR_REWARDS -> {
            SponsorRewardsModal(
                onClose = onClose
            )
        }

        ActiveModal.FEEDBACKS -> {
            FeedbacksModal(
                onSubmit = { name, rating, comment ->
                    onSubmitInquiry("FEEDBACK", name, "$rating Stars", "", "", comment)
                },
                onClose = onClose
            )
        }

        ActiveModal.PURCHASE_REWARDS -> {
            PurchaseRewardsModal(
                onClose = onClose
            )
        }

        ActiveModal.MESSAGE_FOR_YOU -> {
            MessageForYouModal(
                onClose = onClose
            )
        }

        ActiveModal.ADMIN_PANEL -> {
            ProductCrmStudioModal(
                categories = categories,
                products = products,
                onSaveProduct = onSaveProduct,
                onDeleteProduct = onDeleteProduct,
                onResetCatalog = onResetCatalog,
                onClose = onClose
            )
        }

        else -> {}
    }
}

// 1. YOUR NEEDS / CUSTOM INQUIRY MODAL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YourNeedsModal(
    categories: List<CategoryEntity>,
    onSubmit: (String, String, String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "SpringHaven Series") }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_your_needs"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForestGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Your Needs & Bespoke Decor", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Custom sizing, materials & quotes", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Inquiry Logged Successfully!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Our interior design consultants will contact you within 2 hours with customized blueprints and pricing.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 60) name = it.trimStart() },
                        label = { Text("Your Full Name *") },
                        singleLine = true,
                        isError = name.isBlank() && name.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input -> 
                            // Only allow numbers, spaces, plus, hyphen (valid international / Indian phone chars, max 15 digits)
                            val filtered = input.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                            if (filtered.length <= 16) phone = filtered
                        },
                        label = { Text("Phone Number * (e.g. +91 98765 43210)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { if (it.length <= 80) email = it.trim() },
                        label = { Text("Email Address (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { if (it.length <= 500) details = it },
                        label = { Text("Specify Room, Dimensions & Needs") },
                        placeholder = { Text("e.g. 15\" King mattress with custom hydraulic teak frame") },
                        minLines = 3,
                        supportingText = { Text("${details.length}/500 chars", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) {
                    Text("Close")
                }
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSubmit(name, phone, email, selectedCategory, details)
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Submit Custom Inquiry")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    )
}

// 2. REPAIRS SERVICE TICKET MODAL
@Composable
private fun RepairsModal(
    onSubmit: (String, String, String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var productInfo by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_repairs"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForestGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Repairs & Maintenance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Certified technician home visit", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Service Request Dispatched", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "An authorized Good Dream engineer will contact you to schedule an on-site inspection.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = productInfo, onValueChange = { productInfo = it }, label = { Text("Product Model / Serial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = problemDescription, onValueChange = { problemDescription = it }, label = { Text("Issue Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSubmit(name, phone, "", productInfo, problemDescription)
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Dispatch Engineer")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}

// 3. COMPLAINTS SLA ESCALATION MODAL
@Composable
private fun ComplaintsModal(
    onSubmit: (String, String, String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_complaints"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForestGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Grievances & Complaints", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Guaranteed 24-Hour SLA Escalation", fontSize = 11.sp, color = SatinGoldDark)
                }
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Escalation Ticket Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your concern has been escalated directly to Senior Management. You will receive a resolution call within 24 hours.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Nature of Grievance") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Sanctuary Flagship Redressal Desk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GOOD DREAMS HOME DECOR PRIVATE LIMITED\nD-4, VIJAY VIHAR COLONY, NAYA KHEDA, Amba Bari, Jaipur, Jaipur- 302039, Rajasthan\nMarketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013\nEmail: gooddreamshomedecor@gmail.com",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) { Text("Dismiss") }
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSubmit(name, phone, "", "Grievance", details)
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Escalate Ticket")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}

// 4. SERVICE & WARRANTIES LOOKUP MODAL
@Composable
private fun ServiceWarrantiesModal(
    onClose: () -> Unit
) {
    var serialNumberInput by remember { mutableStateOf("") }
    var lookupResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_service_warranties"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Service & Warranties", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Serial verification & policy", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = serialNumberInput,
                    onValueChange = { serialNumberInput = it },
                    label = { Text("Product Serial / SKU / Phone") },
                    placeholder = { Text("e.g. GD-SH-1501 or phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (serialNumberInput.isNotBlank()) {
                            lookupResult = "✓ Verified Active Warranty\nProduct: Good Dream SpringHaven Master Series\nCoverage: 15 Years Comprehensive (Structural & Coils)\nStatus: Registered & Active"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Verify Warranty Status")
                }

                if (lookupResult != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lookupResult!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SpringHaven 25-Year Guarantee Commitment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "1. 25-Year Limited Warranty across Sofa, Bed, Sofa Cum Bed, Mattress, Gadda & Customized Furniture.\n2. Scheduled 5-Year periodic service maintenance program.\n3. Transportation subsidy: We bear up to ₹1,000 (round-trip).\n4. Inflation-indexed service charges referenced against CPI (IW/AL).\n5. Claims require original retail invoice and official service card.",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = TextSecondaryMuted
                )
            }
        },
        confirmButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) {
                Text("Done")
            }
        }
    )
}

// 5. SPONSOR REWARDS MODAL
@Composable
private fun SponsorRewardsModal(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_sponsor_rewards"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SatinGoldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = SatinGoldDark)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Sponsor Rewards", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Refer friends to earn luxury credit", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "₹2,500 Store Credit",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SatinGoldDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "For every friend who purchases a SpringHaven bed setup or mattress using your unique sponsor invitation.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Sponsor Code", "DREAM-LUXE-88"))
                            copied = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Your Sponsor Code", fontSize = 10.sp, color = TextSecondaryMuted)
                            Text("DREAM-LUXE-88", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = if (copied) "Copied!" else "Tap to Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) {
                Text("Close")
            }
        }
    )
}

// 6. FEEDBACKS MODAL
@Composable
private fun FeedbacksModal(
    onSubmit: (String, Int, String) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    var review by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_feedbacks"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Client Feedbacks & Ratings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Share your comfort experience", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = StatusError, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Thank You For Your Feedback!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Your review helps us refine our luxury craftsmanship for a better tomorrow.", textAlign = TextAlign.Center, fontSize = 12.sp, color = TextSecondaryMuted)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    // Star Rating Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i }) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i Stars",
                                    tint = SatinGoldAccent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = review,
                        onValueChange = { review = it },
                        label = { Text("Share your experience") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank() && review.isNotBlank()) {
                            onSubmit(name, rating, review)
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Submit Review")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}

// 7. PURCHASE REWARDS MODAL
@Composable
private fun PurchaseRewardsModal(
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_purchase_rewards"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SatinGoldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = SatinGoldDark)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Purchase & Rewards", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tier status & loyalty points", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = ForestGreenPrimary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Point Balance", fontSize = 11.sp, color = SatinGoldLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2,500 Gold Coins", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SatinGoldAccent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Worth ₹2,500 on your next bespoke furniture order.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Loyalty Perks", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• 1 Coin per ₹10 spent on luxury catalog products\n• Complimentary assembly on all purchases\n• Priority appointment with senior sleep ergonomic consultants", fontSize = 11.5.sp, lineHeight = 16.sp, color = TextSecondaryMuted)
            }
        },
        confirmButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) {
                Text("Close")
            }
        }
    )
}

// 8. MESSAGE FOR YOU MODAL
@Composable
private fun MessageForYouModal(
    onClose: () -> Unit
) {
    val messages = listOf(
        "Welcome to Good Dream Home Decor! Explore our flagship SpringHaven 15\" series.",
        "Festive Privilege: Complimentary silk pillows and room setup with any master suite.",
        "Your warranty certificates are saved automatically in your account hub."
    )

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("modal_message_for_you"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mail, contentDescription = null, tint = ForestGreenPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Messages For You", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Official announcements & notifications", fontSize = 11.sp, color = TextSecondaryMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                messages.forEachIndexed { idx, msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Notification #${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SatinGoldDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)) {
                Text("Understood")
            }
        }
    )
}

