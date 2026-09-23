package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.SatinGoldAccent
import com.example.ui.theme.StatusError

@Composable
private fun AuthRequiredWarningCard() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Sign In Required to Submit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "To safeguard client privacy and link this record to your verified account, please sign in before submitting.",
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// 1. CUSTOM BEDDING INQUIRY & CONSULTATION SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomInquiryScreen(
    categories: List<CategoryEntity>,
    prefilledName: String? = null,
    prefilledEmail: String? = null,
    onSubmit: (name: String, phone: String, email: String, category: String, details: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(prefilledName) { mutableStateOf(prefilledName ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember(prefilledEmail) { mutableStateOf(prefilledEmail ?: "") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "SpringHaven Series") }
    var customDimensions by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_custom_inquiry"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Custom Bedding Consultation",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isSubmitted) {
            SubmissionSuccessView(
                title = "Consultation Request Received!",
                message = "Thank you $name. Our Master Bedding Consultant will call you at $phone within 2 hours to discuss custom dimensions and provide an instant bespoke quotation.",
                onAction = onBack,
                actionText = "Return to Showroom"
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (prefilledEmail.isNullOrBlank()) {
                    AuthRequiredWarningCard()
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DesignServices, contentDescription = null, tint = SatinGoldAccent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Bespoke Handcrafted Sizing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Need non-standard dimensions, European king, or custom firmness? Our artisans craft any mattress specification.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Your Contact Information", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("inquiry_name_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("Mobile Number (10 Digits) *") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("inquiry_phone_input")
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address (Optional)") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Category & Specifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Select Target Category:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customDimensions,
                            onValueChange = { customDimensions = it },
                            label = { Text("Custom Dimensions (Length x Width x Height)") },
                            placeholder = { Text("e.g. 84\" x 78\" x 14\"") },
                            leadingIcon = { Icon(Icons.Outlined.Straighten, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Specific Sleep Requirements / Notes") },
                            placeholder = { Text("Describe required firmness, back support needs, or existing bed frame style...") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("inquiry_details_input")
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10) {
                            val fullNotes = "Category: $selectedCategory | Dimensions: $customDimensions | Notes: $details"
                            onSubmit(name, phone, email, selectedCategory, fullNotes)
                            isSubmitted = true
                        }
                    },
                    enabled = !prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("inquiry_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (prefilledEmail.isNullOrBlank()) "Sign In to Submit Consultation" else "Submit Consultation Request →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// 2. MATTRESS REPAIR & RESTORATION SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairRequestScreen(
    prefilledName: String? = null,
    prefilledEmail: String? = null,
    onSubmit: (name: String, phone: String, email: String, model: String, details: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(prefilledName) { mutableStateOf(prefilledName ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember(prefilledEmail) { mutableStateOf(prefilledEmail ?: "") }
    var modelOrSku by remember { mutableStateOf("") }
    var issueType by remember { mutableStateOf("Coil / Spring Sagging") }
    var purchaseYear by remember { mutableStateOf("2024") }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_repair_request"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mattress Repair & Restoration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isSubmitted) {
            SubmissionSuccessView(
                title = "Repair Request Scheduled!",
                message = "Your request has been logged. A certified Good Dream service technician will visit your location to inspect the mattress and coordinate zero-hassle restoration under your 25-Year Guarantee.",
                onAction = onBack,
                actionText = "Done"
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (prefilledEmail.isNullOrBlank()) {
                    AuthRequiredWarningCard()
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Customer Contact", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) }, label = { Text("Mobile Phone Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Mattress Details & Issue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(value = modelOrSku, onValueChange = { modelOrSku = it }, label = { Text("Mattress Model / SKU *") }, placeholder = { Text("e.g. SpringHaven™ 15-Inch (GD-MAT-01)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        Text("Type of Restoration Needed:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val issues = listOf("Coil / Spring Sagging", "Edge Border Deformation", "Cover Stitching Repair", "Core Foam Replacement", "General Inspection")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(issues, key = { it }) { iss ->
                                FilterChip(selected = issueType == iss, onClick = { issueType = iss }, label = { Text(iss, fontSize = 11.sp) })
                            }
                        }

                        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detailed Description of Issue *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                }

                Button(
                    onClick = {
                        if (!prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10 && modelOrSku.isNotBlank()) {
                            onSubmit(name, phone, email, modelOrSku, "Issue: $issueType | $details")
                            isSubmitted = true
                        }
                    },
                    enabled = !prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10 && modelOrSku.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (prefilledEmail.isNullOrBlank()) "Sign In to Request Repair" else "Submit Repair Request →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// 3. COMPLAINTS & GRIEVANCE REDRESSAL SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintScreen(
    prefilledName: String? = null,
    prefilledEmail: String? = null,
    onSubmit: (name: String, phone: String, email: String, orderRef: String, details: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(prefilledName) { mutableStateOf(prefilledName ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember(prefilledEmail) { mutableStateOf(prefilledEmail ?: "") }
    var orderRef by remember { mutableStateOf("") }
    var complaintCategory by remember { mutableStateOf("Delivery / Dispatch Delay") }
    var details by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_complaint"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Client Care & Grievance Redressal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isSubmitted) {
            SubmissionSuccessView(
                title = "Grievance Ticket Registered",
                message = "Your grievance has been escalated directly to our Executive Care Office. You will receive a resolution update within 24 business hours.",
                onAction = onBack,
                actionText = "Done"
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (prefilledEmail.isNullOrBlank()) {
                    AuthRequiredWarningCard()
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Grievance Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val categories = listOf("Delivery / Dispatch Delay", "Wrong Dimension Received", "Showroom Experience", "Packaging / Transit Damage", "Warranty Dispute")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it }) { cat ->
                                FilterChip(selected = complaintCategory == cat, onClick = { complaintCategory = cat }, label = { Text(cat, fontSize = 11.sp) })
                            }
                        }

                        OutlinedTextField(value = orderRef, onValueChange = { orderRef = it }, label = { Text("Order ID or Invoice Number") }, placeholder = { Text("e.g. GD-ORD-2026-88291") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) }, label = { Text("Contact Phone Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Grievance Explanation *") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    }
                }

                Button(
                    onClick = {
                        if (!prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10 && details.isNotBlank()) {
                            onSubmit(name, phone, email, orderRef, "Category: $complaintCategory | Details: $details")
                            isSubmitted = true
                        }
                    },
                    enabled = !prefilledEmail.isNullOrBlank() && name.isNotBlank() && phone.length >= 10 && details.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text(
                        text = if (prefilledEmail.isNullOrBlank()) "Sign In to Escalate Grievance" else "Escalate & Submit Grievance →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// 4. SHOWROOM & PRODUCT REVIEW SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    prefilledName: String? = null,
    prefilledEmail: String? = null,
    onSubmit: (name: String, rating: Int, comment: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(prefilledName) { mutableStateOf(prefilledName ?: "") }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Cloud Comfort") }
    var isSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_feedback"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customer Experience Review",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isSubmitted) {
            SubmissionSuccessView(
                title = "Thank You for Your Review!",
                message = "Your rating ($rating Stars) and feedback have been shared with our craft team. Authentic reviews help fellow sleep seekers choose the perfect sanctuary mattress.",
                onAction = onBack,
                actionText = "Done"
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (prefilledEmail.isNullOrBlank()) {
                    AuthRequiredWarningCard()
                }

                Text(
                    text = "How was your Good Dream sleep experience?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 5-Star Rating Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$star Stars",
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Text(
                    text = when (rating) {
                        5 -> "Exceptional Luxury Sanctuary (5/5)"
                        4 -> "Very Comfortable & Restful (4/5)"
                        3 -> "Good Quality Bedding (3/5)"
                        else -> "Needs Improvement"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SatinGoldAccent
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Highlight Tag:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val tags = listOf("Cloud Comfort", "Back Pain Relief", "Express Delivery", "Cooling Technology", "True 25-Yr Durability")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tags, key = { it }) { t ->
                                FilterChip(selected = selectedTag == t, onClick = { selectedTag = t }, label = { Text(t, fontSize = 11.sp) })
                            }
                        }

                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Write your review commentary *") }, placeholder = { Text("Tell us about your mattress comfort, delivery experience, and sleep quality...") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    }
                }

                Button(
                    onClick = {
                        if (!prefilledEmail.isNullOrBlank() && name.isNotBlank() && comment.isNotBlank()) {
                            onSubmit(name, rating, "Tag: $selectedTag | $comment")
                            isSubmitted = true
                        }
                    },
                    enabled = !prefilledEmail.isNullOrBlank() && name.isNotBlank() && comment.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (prefilledEmail.isNullOrBlank()) "Sign In to Post Review" else "Post Experience Review →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// 5. 25-YEAR SPRINGHAVEN WARRANTY & SERVICE SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyServiceScreen(
    prefilledName: String? = null,
    prefilledEmail: String? = null,
    onSubmit: (name: String, phone: String, email: String, invoice: String, serial: String, date: String) -> Unit = { _, _, _, _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var customerName by remember(prefilledName) { mutableStateOf(prefilledName ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember(prefilledEmail) { mutableStateOf(prefilledEmail ?: "") }
    var invoiceNumber by remember { mutableStateOf("") }
    var mattressSerial by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var registrationSuccess by remember { mutableStateOf(false) }
    var generatedCertRef by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_warranty_service"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "25-Year SpringHaven Guarantee",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Register Warranty") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Guarantee Coverage") })
            }

            if (activeTab == 0) {
                if (registrationSuccess) {
                    SubmissionSuccessView(
                        title = "25-Year Guarantee Enrolled!",
                        message = "Certificate #$generatedCertRef has been created for $customerName ($phone). Digital verification certificate sent to registered email $email.",
                        onAction = { registrationSuccess = false },
                        actionText = "Register Another"
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (prefilledEmail.isNullOrBlank()) {
                            AuthRequiredWarningCard()
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Verified, contentDescription = null, tint = SatinGoldAccent, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Official Warranty Registration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Register your mattress within 30 days of delivery to activate complete 25-Year coverage.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) }, label = { Text("Phone Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address (for Digital Certificate Delivery) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number *") }, placeholder = { Text("e.g. GD-INV-9921") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = mattressSerial, onValueChange = { mattressSerial = it }, label = { Text("Mattress Barcode / Serial Number") }, placeholder = { Text("Found on law label tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = { Text("Date of Delivery / Purchase") }, placeholder = { Text("e.g. 15 Sep 2026") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (!prefilledEmail.isNullOrBlank() && customerName.isNotBlank() && phone.length >= 10 && invoiceNumber.isNotBlank()) {
                                    val generatedRef = "GD-WARR-${System.currentTimeMillis() % 1000000}"
                                    generatedCertRef = generatedRef
                                    onSubmit(customerName, phone, email, invoiceNumber, mattressSerial, purchaseDate)
                                    registrationSuccess = true
                                }
                            },
                            enabled = !prefilledEmail.isNullOrBlank() && customerName.isNotBlank() && phone.length >= 10 && invoiceNumber.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                if (prefilledEmail.isNullOrBlank()) "Sign In Required to Activate Guarantee" else "Activate 25-Year Guarantee →",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Guarantee Coverage Info Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CoverageCard(period = "Years 1 to 2", coverage = "100% Free Replacement / Repair", details = "Zero charges for parts, labour, and doorstep logistics for any manufacturing or coil defect.")
                    CoverageCard(period = "Years 3 to 10", coverage = "Pro-Rata Repair & Component Swap", details = "Customer pays only actual component cost with free technician inspection and installation.")
                    CoverageCard(period = "Years 11 to 25", coverage = "Guaranteed Loyalty Trade-In Credit", details = "Eligible for 30% to 50% exchange credit value towards the purchase of a new flagship SpringHaven model.")
                }
            }
        }
    }
}

// 6. VIP REWARDS & SANCTUARY PRIVILEGES SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsAndOffersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_rewards_offers"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VIP Rewards & Privileges",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Diamond, contentDescription = null, tint = SatinGoldAccent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Architect & Designer Privilege Program", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you an Interior Designer, Architect, or Luxury Villa Furnisher? Join the Good Dream Trade Guild to receive 15% trade commission, priority manufacturing slots, and bespoke tailoring.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Active Member Perks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    PerkRow(icon = Icons.Outlined.CardGiftcard, title = "₹5,000 Welcome Voucher", desc = "Applied automatically at checkout for orders above ₹35,000")
                    PerkRow(icon = Icons.Outlined.Bed, title = "Complimentary Memory Foam Pillow Pair", desc = "Included with all 12\" and 15\" SpringHaven mattress orders")
                    PerkRow(icon = Icons.Outlined.SupportAgent, title = "24/7 Dedicated Sleep Specialist", desc = "Direct VIP line for mattress care, rotating instructions, and firmness adjustments")
                }
            }
        }
    }
}

// Reusable Helper Views
@Composable
private fun SubmissionSuccessView(
    title: String,
    message: String,
    onAction: () -> Unit,
    actionText: String
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoverageCard(period: String, coverage: String, details: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(period, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SatinGoldAccent)
                Text("Official Term", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(coverage, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PerkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SatinGoldAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
