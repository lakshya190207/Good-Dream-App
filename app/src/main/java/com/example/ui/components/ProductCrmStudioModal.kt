package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SavedAddress
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.InvoicePrinterHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-featured Executive Catalog CRM Studio for Good Dream Home Decor.
 * Allows store administrators to:
 * - Add or Edit products in ANY of the 12 store categories.
 * - Manage multi-image galleries with live thumbnail previews and quick luxury presets.
 * - Attach and preview product showcase videos (MP4, YouTube, WebM).
 * - Browse, search, filter, and delete inventory items in real-time.
 * - Monitor placed customer orders and leads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCrmStudioModal(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    orders: List<OrderEntity> = emptyList(),
    inquiries: List<InquiryEntity> = emptyList(),
    crmUsers: List<CrmUserRecord> = emptyList(),
    supportSessions: List<SupportChatSession> = emptyList(),
    selectedChatSession: SupportChatSession? = null,
    adminChatMessages: List<SupportChatMessage> = emptyList(),
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onResetCatalog: () -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit = { _, _ -> },
    onUpdateInquiryStatus: (String, String) -> Unit = { _, _ -> },
    onSaveUser: (CrmUserRecord) -> Unit = {},
    onDeleteUser: (String) -> Unit = {},
    onSelectChatSession: (SupportChatSession) -> Unit = {},
    onSendAdminReply: (String, String) -> Unit = { _, _ -> },
    onSendDirectMessage: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onResolveChat: (String) -> Unit = {},
    onLoadSupportDesk: () -> Unit = {},
    onLogoutAdmin: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }
    var editingProductId by remember { mutableStateOf<String?>(null) }

    // Form States
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull()?.id ?: "cat_springhaven")
    }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("GD-PRD-${System.currentTimeMillis() % 10000}") }
    var price by remember { mutableStateOf("24999") }
    var originalPrice by remember { mutableStateOf("32000") }
    var thicknessInches by remember { mutableIntStateOf(10) }
    var firmness by remember { mutableStateOf("Medium Firm") }
    var dimensions by remember { mutableStateOf("78\" x 72\" King") }
    var material by remember { mutableStateOf("Swedish 7-Zone Pocket Coils & Natural Latex") }
    var warrantyYears by remember { mutableIntStateOf(10) }
    var isSpringhavenSeries by remember { mutableStateOf(false) }
    var isNewLaunch by remember { mutableStateOf(true) }
    var description by remember {
        mutableStateOf("Artisan-crafted sleep piece engineered with responsive pressure relief, breathable temperature regulation, and enduring zero-motion transfer stability.")
    }

    // Media Gallery States (Images)
    var imageInputUrl by remember { mutableStateOf("") }
    val imageList = remember {
        mutableStateListOf(
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1540518614846-7ede433c4ef0?q=80&w=1000&auto=format&fit=crop"
        )
    }

    // Video State
    var videoUrl by remember {
        mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }

    // Inventory Search & Filters
    var inventorySearchQuery by remember { mutableStateOf("") }
    var inventoryFilterCategory by remember { mutableStateOf<String?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var directMessageTargetUser by remember { mutableStateOf<CrmUserRecord?>(null) }
    var showDirectMessageDialog by remember { mutableStateOf(false) }

    fun loadProductForEditing(product: ProductEntity) {
        editingProductId = product.id
        selectedCategoryId = product.categoryId
        title = product.title
        subtitle = product.subtitle
        sku = product.sku
        price = product.price.toInt().toString()
        originalPrice = product.originalPrice.toInt().toString()
        thicknessInches = product.thicknessInches
        firmness = product.firmness
        dimensions = product.dimensions
        material = product.material
        warrantyYears = product.warrantyYears
        isSpringhavenSeries = product.isSpringhavenSeries
        isNewLaunch = product.isNewLaunch
        description = product.description
        videoUrl = product.videoUrl
        imageList.clear()
        imageList.addAll(product.getImagesList())
        activeTab = 0
    }

    fun clearForm() {
        editingProductId = null
        title = ""
        subtitle = ""
        sku = "GD-PRD-${System.currentTimeMillis() % 10000}"
        price = "19999"
        originalPrice = "26000"
        thicknessInches = 10
        firmness = "Medium Firm"
        dimensions = "78\" x 72\" King"
        material = "Organic European Latex & Micro-Pocket Springs"
        warrantyYears = 10
        isSpringhavenSeries = selectedCategoryId == "cat_springhaven"
        isNewLaunch = true
        description = "Handcrafted luxury piece designed for restorative sleep and comfort."
        videoUrl = ""
        imageList.clear()
        imageList.add("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop")
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("modal_product_crm_studio"),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Luxury CRM Header
                Surface(
                    color = ForestGreenDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SatinGoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = ForestGreenDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Good Dream™ CRM Studio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Catalog CMS • Multi-Image & Video Studio",
                                        fontSize = 11.sp,
                                        color = SatinGoldLight
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = {
                                        Toast.makeText(context, "Admin session locked.", Toast.LENGTH_SHORT).show()
                                        onLogoutAdmin()
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Studio",
                                        tint = SatinGoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lock",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close CRM",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Metrics Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CrmMetricBadge(
                                label = "Products",
                                value = "${products.size}",
                                modifier = Modifier.weight(1f)
                            )
                            CrmMetricBadge(
                                label = "Categories",
                                value = "${categories.size}",
                                modifier = Modifier.weight(1f)
                            )
                            CrmMetricBadge(
                                label = "Videos",
                                value = "${products.count { it.hasVideo() }}",
                                modifier = Modifier.weight(1f)
                            )
                            CrmMetricBadge(
                                label = "Cloud Sync",
                                value = "Live",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (editingProductId != null) Icons.Default.Edit else Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (editingProductId != null) "Edit Product" else "Add New Product",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live Inventory (${products.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Orders & Leads (${orders.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Users & Clients (${crmUsers.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = {
                            activeTab = 4
                            onLoadSupportDesk()
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Support Desk (${supportSessions.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }

                // Tab 0: Add or Edit Product Form
                if (activeTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (editingProductId != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SatinGoldContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Editing: $title",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ForestGreenDark
                                    )
                                    TextButton(onClick = { clearForm() }) {
                                        Text("Cancel Edit", fontSize = 11.sp, color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // SECTION 1: Target Category
                        CrmFormSectionCard(title = "1. TARGET STORE CATEGORY (ANY OF 12)") {
                            Text(
                                text = "Select which department this product belongs to:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories, key = { it.id }) { cat ->
                                    val isSelected = cat.id == selectedCategoryId
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) SatinGoldAccent else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedCategoryId = cat.id
                                            isSpringhavenSeries = cat.id == "cat_springhaven"
                                        }
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION 2: Core Identity & Pricing
                        CrmFormSectionCard(title = "2. TITLE, SKU & PRICING") {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Product Title *") },
                                placeholder = { Text("e.g. SpringHaven™ 15-Inch Emperor Bed") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = subtitle,
                                onValueChange = { subtitle = it },
                                label = { Text("Subtitle / Short Tagline") },
                                placeholder = { Text("e.g. Swedish 7-Zone Pocket Spring Sanctuary") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = sku,
                                    onValueChange = { sku = it },
                                    label = { Text("SKU Number") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { sku = "GD-${selectedCategoryId.replace("cat_", "").take(3).uppercase()}-${System.currentTimeMillis() % 10000}" },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Auto SKU", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { price = it },
                                    label = { Text("Selling Price (₹) *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = originalPrice,
                                    onValueChange = { originalPrice = it },
                                    label = { Text("Original MRP (₹)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val pVal = price.toDoubleOrNull() ?: 0.0
                            val origVal = originalPrice.toDoubleOrNull() ?: 0.0
                            if (origVal > pVal && pVal > 0) {
                                val discount = ((origVal - pVal) / origVal * 100).toInt()
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Discount: $discount% OFF • Customer Saves ₹${(origVal - pVal).toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // SECTION 3: Physical Specs & Badges
                        CrmFormSectionCard(title = "3. THICKNESS, FIRMNESS & SPECIFICATIONS") {
                            Text("Thickness (Inches):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(6, 8, 10, 12, 14, 15).forEach { thick ->
                                    FilterChip(
                                        selected = thicknessInches == thick,
                                        onClick = { thicknessInches = thick },
                                        label = { Text("$thick\"") }
                                    )
                                }
                            }

                            Text("Firmness Rating:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Plush (3/10)", "Medium Soft (5/10)", "Medium Firm (7/10)", "Ortho Firm (9/10)").forEach { f ->
                                    FilterChip(
                                        selected = firmness == f,
                                        onClick = { firmness = f },
                                        label = { Text(f, fontSize = 10.5.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = dimensions,
                                    onValueChange = { dimensions = it },
                                    label = { Text("Dimensions") },
                                    placeholder = { Text("e.g. 78\" x 72\" King") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = "$warrantyYears",
                                    onValueChange = { warrantyYears = it.toIntOrNull() ?: 10 },
                                    label = { Text("Warranty (Yrs)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }

                            OutlinedTextField(
                                value = material,
                                onValueChange = { material = it },
                                label = { Text("Core Material Architecture") },
                                placeholder = { Text("e.g. Belgian Latex + Swedish Titanium Springs") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSpringhavenSeries,
                                        onCheckedChange = { isSpringhavenSeries = it }
                                    )
                                    Text("SpringHaven Flagship", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isNewLaunch,
                                        onCheckedChange = { isNewLaunch = it }
                                    )
                                    Text("New Launch Flag", fontSize = 12.sp)
                                }
                            }
                        }

                        // SECTION 4: Multi-Image Gallery
                        CrmFormSectionCard(title = "4. MULTI-IMAGE GALLERY MANAGEMENT") {
                            Text(
                                text = "Current images in gallery (${imageList.size}):",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (imageList.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    itemsIndexed(imageList, key = { index, imgUrl -> "$index-$imgUrl" }) { index, imgUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        ) {
                                            LuxuryAsyncImage(
                                                imageUrl = imgUrl,
                                                contentDescription = "Image $index",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { imageList.removeAt(index) },
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = imageInputUrl,
                                    onValueChange = { imageInputUrl = it },
                                    label = { Text("Paste Image URL") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (imageInputUrl.isNotBlank()) {
                                            imageList.add(imageInputUrl.trim())
                                            imageInputUrl = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add")
                                }
                            }

                            Text("Quick Presets (Unsplash Luxury Bedding):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CrmPresetChip("Master Bed") {
                                    imageList.add("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop")
                                }
                                CrmPresetChip("Coils Layer") {
                                    imageList.add("https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1000&auto=format&fit=crop")
                                }
                                CrmPresetChip("Tufting") {
                                    imageList.add("https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?q=80&w=1000&auto=format&fit=crop")
                                }
                            }
                        }

                        // SECTION 5: Video Showcase URL
                        CrmFormSectionCard(title = "5. PRODUCT SHOWCASE VIDEO (MP4 / STREAM)") {
                            Text(
                                text = "Attach a video showing product craftsmanship, comfort tests, or bedroom styling:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("Video Stream URL (MP4, YouTube, WebM)") },
                                placeholder = { Text("https://example.com/videos/craftsmanship.mp4") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sample Demo Video", fontSize = 11.sp)
                                }

                                if (videoUrl.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open video URL", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SatinGoldAccent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ForestGreenDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview Video", color = ForestGreenDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // SECTION 6: Description & Publishing
                        CrmFormSectionCard(title = "6. CATALOG DESCRIPTION & PUBLISH") {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Full Product Description") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (title.isBlank()) {
                                        Toast.makeText(context, "Please enter a product title", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val finalPrice = price.toDoubleOrNull() ?: 19999.0
                                    val finalOrig = originalPrice.toDoubleOrNull() ?: (finalPrice * 1.25)
                                    val combinedImages = if (imageList.isEmpty()) {
                                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop"
                                    } else {
                                        imageList.joinToString("|")
                                    }

                                    val productEntity = ProductEntity(
                                        id = editingProductId ?: "prod_crm_${System.currentTimeMillis()}",
                                        categoryId = selectedCategoryId,
                                        title = title.trim(),
                                        subtitle = subtitle.trim().ifBlank { "Good Dream Luxury Bedding" },
                                        sku = sku.trim().ifBlank { "GD-GEN-${System.currentTimeMillis() % 10000}" },
                                        isSpringhavenSeries = isSpringhavenSeries,
                                        description = description.trim(),
                                        specificationsJson = "Thickness: $thicknessInches Inches|Firmness: $firmness|Dimensions: $dimensions|Material: $material|Warranty: $warrantyYears Years",
                                        price = finalPrice,
                                        originalPrice = finalOrig,
                                        imagesJson = combinedImages,
                                        isNewLaunch = isNewLaunch,
                                        warrantyYears = warrantyYears,
                                        thicknessInches = thicknessInches,
                                        material = material,
                                        dimensions = dimensions,
                                        firmness = firmness,
                                        videoUrl = videoUrl.trim(),
                                        createdAt = System.currentTimeMillis()
                                    )

                                    onSaveProduct(productEntity)
                                    Toast.makeText(context, "✓ Product published to live catalog!", Toast.LENGTH_LONG).show()
                                    clearForm()
                                    activeTab = 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = SatinGoldAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (editingProductId != null) "Update Live Product" else "Publish Product to Catalog",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Tab 1: Live Inventory Management
                if (activeTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        OutlinedTextField(
                            value = inventorySearchQuery,
                            onValueChange = { inventorySearchQuery = it },
                            placeholder = { Text("Search catalog by title, SKU, or specs...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = inventoryFilterCategory == null,
                                    onClick = { inventoryFilterCategory = null },
                                    label = { Text("All Categories (${products.size})") }
                                )
                            }
                            items(categories, key = { it.id }) { cat ->
                                val catCount = products.count { it.categoryId == cat.id }
                                FilterChip(
                                    selected = inventoryFilterCategory == cat.id,
                                    onClick = { inventoryFilterCategory = cat.id },
                                    label = { Text("${cat.name} ($catCount)") }
                                )
                            }
                        }

                        val filteredProducts = remember(products, inventorySearchQuery, inventoryFilterCategory) {
                            products.filter { prod ->
                                val matchesSearch = inventorySearchQuery.isBlank() ||
                                        prod.title.contains(inventorySearchQuery, ignoreCase = true) ||
                                        prod.sku.contains(inventorySearchQuery, ignoreCase = true) ||
                                        prod.description.contains(inventorySearchQuery, ignoreCase = true)
                                val matchesCat = inventoryFilterCategory == null || prod.categoryId == inventoryFilterCategory
                                matchesSearch && matchesCat
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Showing ${filteredProducts.size} Products",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(onClick = { showResetConfirmDialog = true }) {
                                Text("Factory Reset Catalog", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                CrmProductInventoryCard(
                                    product = product,
                                    categoryName = categories.find { it.id == product.categoryId }?.name ?: "Category",
                                    onEdit = { loadProductForEditing(product) },
                                    onDelete = { productToDelete = product }
                                )
                            }
                        }
                    }
                }

                // Tab 2: Orders & Customer Leads
                if (activeTab == 2) {
                    CrmOrdersAndLeadsView(
                        orders = orders,
                        inquiries = inquiries,
                        onUpdateOrderStatus = onUpdateOrderStatus,
                        onUpdateInquiryStatus = onUpdateInquiryStatus
                    )
                }

                // Tab 3: Users & Clients
                if (activeTab == 3) {
                    CrmUsersTabContent(
                        crmUsers = crmUsers,
                        onSaveUser = onSaveUser,
                        onDeleteUser = onDeleteUser,
                        onTriggerDirectMessage = { user ->
                            directMessageTargetUser = user
                            showDirectMessageDialog = true
                        }
                    )
                }

                // Tab 4: Live Support Desk
                if (activeTab == 4) {
                    LiveSupportDeskTabContent(
                        supportSessions = supportSessions,
                        selectedSession = selectedChatSession,
                        messages = adminChatMessages,
                        onSelectSession = onSelectChatSession,
                        onSendReply = onSendAdminReply,
                        onResolveChat = onResolveChat,
                        onNewDirectMessage = {
                            directMessageTargetUser = null
                            showDirectMessageDialog = true
                        }
                    )
                }
            }
        }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Remove from Catalog?") },
            text = { Text("Are you sure you want to delete '${productToDelete?.title}' (SKU: ${productToDelete?.sku}) from the store catalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { onDeleteProduct(it.id) }
                        productToDelete = null
                        Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Product", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Factory Catalog?") },
            text = { Text("This will restore the original 18 flagship Good Dream products and discard custom modifications.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetCatalog()
                        Toast.makeText(context, "Catalog restored to factory defaults", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDirectMessageDialog) {
        AdminDirectMessageDialog(
            targetUser = directMessageTargetUser,
            availableUsers = crmUsers,
            orders = orders,
            onDismiss = {
                showDirectMessageDialog = false
                directMessageTargetUser = null
            },
            onSend = { recipientEmail, recipientName, messageText, orderRef ->
                onSendDirectMessage(recipientEmail, recipientName, messageText, orderRef)
                showDirectMessageDialog = false
                directMessageTargetUser = null
                Toast.makeText(context, "Direct message dispatched with notification alert!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun CrmFormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SatinGoldDark
            )
            content()
        }
    }
}

@Composable
private fun CrmMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = SatinGoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun CrmPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CrmProductInventoryCard(
    product: ProductEntity,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val thumb = product.getImagesList().firstOrNull() ?: ""
                LuxuryAsyncImage(
                    imageUrl = thumb,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = categoryName.uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "SKU: ${product.sku} • ₹${product.price.toInt()}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "📸 ${product.getImagesList().size} Images",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.hasVideo()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SatinGoldContainer
                        ) {
                            Text(
                                text = "▶ VIDEO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = SatinGoldDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CrmOrdersAndLeadsView(
    orders: List<OrderEntity>,
    inquiries: List<InquiryEntity>,
    onUpdateOrderStatus: (String, String) -> Unit = { _, _ -> },
    onUpdateInquiryStatus: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var inquiryTypeFilter by remember { mutableStateOf("ALL") }

    val filteredInquiries = remember(inquiries, inquiryTypeFilter) {
        if (inquiryTypeFilter == "ALL") inquiries
        else inquiries.filter { it.type.equals(inquiryTypeFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "REAL-TIME CUSTOMER ORDERS (${orders.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SatinGoldDark
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No customer orders placed yet. Orders placed by users will appear here live with 20% advance & 80% COD breakdown.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.id,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (order.status.equals("Delivered", ignoreCase = true)) ForestGreenContainer else SatinGoldContainer
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (order.status.equals("Delivered", ignoreCase = true)) ForestGreenDark else SatinGoldDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Customer: ${order.customerName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )

                        Text(
                            text = "📍 ${order.deliveryAddress}, ${order.city}, ${order.state} - ${order.pincode} (${order.floorElevator})",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "📦 Items: ${order.itemsSummary}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Financial Split
                        val advance = (order.totalAmount * 0.20).toInt()
                        val balance = (order.totalAmount * 0.80).toInt()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Total Order Value: ₹${order.totalAmount.toInt()} (${order.paymentMethod})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (order.paymentMethod.contains("COD", ignoreCase = true) || order.paymentMethod.contains("Cash", ignoreCase = true)) {
                                    Text(
                                        text = "• 20% Booking Advance: ₹$advance (Received / Verified)",
                                        fontSize = 11.sp,
                                        color = ForestGreenDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "• 80% Balance on Delivery: ₹$balance (Due upon delivery inspection)",
                                        fontSize = 11.sp,
                                        color = SatinGoldDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Order Status Progression Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ORDER LIFECYCLE STAGE (TAP TO UPDATE & NOTIFY CLIENT):",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val stages = listOf("Confirmed", "Atelier Crafting", "Fleet Transit", "Delivered")
                                stages.forEach { stage ->
                                    val isCurrent = order.status.equals(stage, ignoreCase = true)
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = {
                                            if (!isCurrent) {
                                                onUpdateOrderStatus(order.id, stage)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = stage,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isCurrent) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (stage == "Delivered") ForestGreenContainer else SatinGoldContainer,
                                            selectedLabelColor = if (stage == "Delivered") ForestGreenDark else SatinGoldDark
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    InvoicePrinterHelper.printOrderInvoice(context, order)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tax Invoice", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}"))
                                    context.startActivity(dialIntent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call ${order.customerPhone}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOMER INQUIRIES & LEADS (${filteredInquiries.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SatinGoldDark
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val filterOptions = listOf("ALL", "NEEDS", "WARRANTY", "REPAIR", "COMPLAINT", "FEEDBACK")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filterOptions, key = { it }) { filter ->
                    FilterChip(
                        selected = inquiryTypeFilter == filter,
                        onClick = { inquiryTypeFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredInquiries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (inquiryTypeFilter == "ALL") "No customer inquiries or service requests yet."
                        else "No inquiries found matching filter '$inquiryTypeFilter'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredInquiries, key = { it.id }) { inq ->
                var showStatusMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = inq.referenceNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SatinGoldContainer
                                ) {
                                    Text(
                                        text = inq.type.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = SatinGoldDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Interactive Status Dropdown
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (inq.status) {
                                        "Resolved" -> ForestGreenContainer
                                        "Contacted" -> Color(0xFFFEF3C7)
                                        "In Review" -> Color(0xFFE0F2FE)
                                        else -> SatinGoldContainer
                                    },
                                    modifier = Modifier.clickable { showStatusMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inq.status.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = when (inq.status) {
                                                "Resolved" -> ForestGreenDark
                                                "Contacted" -> Color(0xFFB45309)
                                                "In Review" -> Color(0xFF0369A1)
                                                else -> SatinGoldDark
                                            }
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Change status",
                                            modifier = Modifier.size(14.dp),
                                            tint = when (inq.status) {
                                                "Resolved" -> ForestGreenDark
                                                "Contacted" -> Color(0xFFB45309)
                                                "In Review" -> Color(0xFF0369A1)
                                                else -> SatinGoldDark
                                            }
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showStatusMenu,
                                    onDismissRequest = { showStatusMenu = false }
                                ) {
                                    listOf("Received", "In Review", "Contacted", "Resolved").forEach { statusOption ->
                                        DropdownMenuItem(
                                            text = { Text(statusOption, fontSize = 12.sp) },
                                            onClick = {
                                                onUpdateInquiryStatus(inq.referenceNumber, statusOption)
                                                showStatusMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${inq.customerName} • ${inq.customerPhone}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )

                        if (inq.customerEmail.isNotBlank()) {
                            Text(
                                text = "✉ ${inq.customerEmail}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (inq.categoryOrProduct.isNotBlank()) {
                            Text(
                                text = "Ref / Serial: ${inq.categoryOrProduct}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = inq.details,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            if (inq.customerEmail.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${inq.customerEmail}")).apply {
                                            putExtra(Intent.EXTRA_SUBJECT, "Good Dream Concierge [#${inq.referenceNumber}]")
                                        }
                                        context.startActivity(emailIntent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Email Client", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${inq.customerPhone}"))
                                    context.startActivity(dialIntent)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Client", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrmUsersTabContent(
    crmUsers: List<CrmUserRecord>,
    onSaveUser: (CrmUserRecord) -> Unit,
    onDeleteUser: (String) -> Unit,
    onTriggerDirectMessage: (CrmUserRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var userToEdit by remember { mutableStateOf<CrmUserRecord?>(null) }
    var userToDelete by remember { mutableStateOf<CrmUserRecord?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedUserId by remember { mutableStateOf<String?>(null) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    val filteredUsers = remember(crmUsers, searchQuery, selectedFilter) {
        crmUsers.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.phone.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "VIP" -> user.userType == CrmUserType.REGISTERED_VIP
                "ORDERS" -> user.totalOrders > 0
                "LEADS" -> user.userType == CrmUserType.INQUIRY_LEAD
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // KPI Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForestGreenContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Clients", fontSize = 10.sp, color = TextSecondaryMuted)
                        Text("${crmUsers.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestGreenDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SatinGoldContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Registered VIP", fontSize = 10.sp, color = TextSecondaryMuted)
                        Text(
                            "${crmUsers.count { it.userType == CrmUserType.REGISTERED_VIP }}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Spend", fontSize = 10.sp, color = TextSecondaryMuted)
                        Text(
                            currencyFormatter.format(crmUsers.sumOf { it.lifetimeSpend }),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Action & Search Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, email, or phone...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Client", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Chips
        item {
            val filters = listOf("ALL" to "All (${crmUsers.size})", "VIP" to "Registered VIP", "ORDERS" to "Order Clients", "LEADS" to "Leads")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filters, key = { it.first }) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No clients found matching \"$searchQuery\"." else "No clients registered yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                val isExpanded = expandedUserId == user.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (user.userType == CrmUserType.REGISTERED_VIP) SatinGoldAccent.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with Initials
                            val initials = user.name.split(" ").filter { it.isNotBlank() }
                                .take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                                .ifBlank { "CL" }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (user.userType == CrmUserType.REGISTERED_VIP) ForestGreenDark
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = if (user.userType == CrmUserType.REGISTERED_VIP) SatinGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (user.userType) {
                                            CrmUserType.REGISTERED_VIP -> SatinGoldAccent.copy(alpha = 0.2f)
                                            CrmUserType.ORDER_CLIENT -> ForestGreenContainer
                                            CrmUserType.INQUIRY_LEAD -> MaterialTheme.colorScheme.surfaceVariant
                                            CrmUserType.ADMIN -> SatinGoldAccent.copy(alpha = 0.4f)
                                        }
                                    ) {
                                        Text(
                                            text = user.userType.label.uppercase(),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (user.userType) {
                                                CrmUserType.REGISTERED_VIP -> ForestGreenDark
                                                CrmUserType.ORDER_CLIENT -> ForestGreenDark
                                                CrmUserType.INQUIRY_LEAD -> MaterialTheme.colorScheme.onSurfaceVariant
                                                CrmUserType.ADMIN -> ForestGreenDark
                                            },
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "✉ ${user.email}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (user.phone.isNotBlank()) {
                                    Text(
                                        text = "📞 ${user.phone}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Actions
                            IconButton(onClick = { onTriggerDirectMessage(user) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Direct Message Customer", tint = ForestGreenDark, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { userToEdit = user }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { userToDelete = user }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Order & Spend Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${user.totalOrders} Orders", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (user.lifetimeSpend > 0) {
                                Surface(shape = RoundedCornerShape(6.dp), color = ForestGreenContainer) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Spend: ${currencyFormatter.format(user.lifetimeSpend)}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenDark
                                        )
                                    }
                                }
                            }

                            if (user.addresses.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(
                                        "${user.addresses.size} Addresses",
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(
                                onClick = { expandedUserId = if (isExpanded) null else user.id },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (isExpanded) "Collapse ▲" else "Details ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Expandable Details (Addresses, Notes)
                        if (isExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            if (user.notes.isNotBlank()) {
                                Text(
                                    text = "Notes: ${user.notes}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (user.addresses.isNotEmpty()) {
                                Text(
                                    text = "Delivery Address Book:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                user.addresses.forEach { addr ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = SatinGoldContainer) {
                                                    Text(addr.tag.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = ForestGreenDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("${addr.fullName} (${addr.phoneNumber})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Text("${addr.flatHouseNo}, ${addr.streetLocality}, ${addr.city}, ${addr.state} - ${addr.pincode}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Quick Contact Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onTriggerDirectMessage(user) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenDark)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp), tint = SatinGoldAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Push Notification Message", fontSize = 11.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${user.email}")).apply {
                                            putExtra(Intent.EXTRA_SUBJECT, "Good Dream Sanctuary Assistance")
                                        }
                                        context.startActivity(emailIntent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Email", fontSize = 11.sp)
                                }

                                if (user.phone.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                                            context.startActivity(dialIntent)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Create Client Dialog
    if (showCreateDialog) {
        AddOrEditUserDialog(
            user = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, email, phone, notes ->
                onSaveUser(
                    CrmUserRecord(
                        id = email,
                        name = name,
                        email = email,
                        phone = phone,
                        userType = CrmUserType.REGISTERED_VIP,
                        notes = notes
                    )
                )
                showCreateDialog = false
            }
        )
    }

    // Edit Client Dialog
    userToEdit?.let { user ->
        AddOrEditUserDialog(
            user = user,
            onDismiss = { userToEdit = null },
            onConfirm = { name, email, phone, notes ->
                onSaveUser(
                    user.copy(
                        name = name,
                        email = email,
                        phone = phone,
                        notes = notes
                    )
                )
                userToEdit = null
            }
        )
    }

    // Delete Client Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Purge Client Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete user account data for '${user.name}' (${user.email})? This action complies with Google Play data erasure rules and cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user.email)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Purge Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AddOrEditUserDialog(
    user: CrmUserRecord?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, phone: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var notes by remember { mutableStateOf(user?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user != null) "Edit Client Profile" else "Add New Client", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    enabled = user == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Administrative Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        errorMessage = "Please enter customer name."
                        return@Button
                    }
                    if (email.trim().isBlank() || !email.contains("@")) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    onConfirm(name.trim(), email.trim().lowercase(), phone.trim(), notes.trim())
                }
            ) {
                Text(if (user != null) "Update" else "Add Client")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LiveSupportDeskTabContent(
    supportSessions: List<SupportChatSession>,
    selectedSession: SupportChatSession?,
    messages: List<SupportChatMessage>,
    onSelectSession: (SupportChatSession) -> Unit,
    onSendReply: (String, String) -> Unit,
    onResolveChat: (String) -> Unit,
    onNewDirectMessage: () -> Unit = {}
) {
    var replyText by remember { mutableStateOf("") }
    val msgTimeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (supportSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Active Support Inquiries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customer live chat conversations will stream here in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNewDirectMessage,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = SatinGoldAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Direct Message Any Customer", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        } else {
            // Master-Detail Split: Carousel list of threads, bottom detailed chat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE CHAT SESSIONS (${supportSessions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SatinGoldDark
                )
                TextButton(
                    onClick = onNewDirectMessage,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = ForestGreenDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Direct Message Customer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreenDark)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(supportSessions, key = { it.id }) { session ->
                    val isSelected = selectedSession?.id == session.id
                    val isResolved = session.status.equals("RESOLVED", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier
                            .width(220.dp)
                            .clickable { onSelectSession(session) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.customerName.ifBlank { "Guest" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isResolved) TextSecondaryMuted.copy(alpha = 0.2f)
                                            else StatusSuccess.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = session.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isResolved) TextSecondaryMuted else StatusSuccess
                                    )
                                }
                            }

                            if (session.customerEmail.isNotBlank()) {
                                Text(
                                    text = session.customerEmail,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = session.lastMessage,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Conversation Pane
            if (selectedSession == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap a customer session above to open the live conversation.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Thread Header
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chat with ${selectedSession.customerName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    val orderNote = selectedSession.orderReference?.let { " • Order: $it" } ?: ""
                                    Text(
                                        text = "${selectedSession.customerEmail}$orderNote",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!selectedSession.status.equals("RESOLVED", ignoreCase = true)) {
                                    FilledTonalButton(
                                        onClick = { onResolveChat(selectedSession.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resolve", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Thread Messages
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (messages.isEmpty()) {
                                item {
                                    Text(
                                        text = "Waiting for customer messages...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            items(messages, key = { it.id }) { msg ->
                                val isAgent = msg.senderType == SupportSenderType.SUPPORT_AGENT
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isAgent) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isAgent) 12.dp else 2.dp,
                                            bottomEnd = if (isAgent) 2.dp else 12.dp
                                        ),
                                        color = if (isAgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(
                                                text = if (isAgent) "Support Specialist" else msg.senderName.ifBlank { "Customer" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAgent) SatinGoldAccent else MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.5.sp,
                                                color = if (isAgent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = msgTimeFormat.format(Date(msg.timestamp)),
                                                fontSize = 9.sp,
                                                color = if (isAgent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Reply Input
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Type reply to customer...", fontSize = 12.5.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (replyText.isNotBlank()) {
                                            val text = replyText.trim()
                                            replyText = ""
                                            onSendReply(selectedSession.id, text)
                                        }
                                    },
                                    enabled = replyText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Reply",
                                        tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else TextSecondaryMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDirectMessageDialog(
    targetUser: CrmUserRecord?,
    availableUsers: List<CrmUserRecord>,
    orders: List<OrderEntity>,
    onDismiss: () -> Unit,
    onSend: (recipientEmail: String, recipientName: String, messageText: String, orderReference: String?) -> Unit
) {
    var recipientEmail by remember { mutableStateOf(targetUser?.email ?: "") }
    var recipientName by remember { mutableStateOf(targetUser?.name ?: "") }
    var messageText by remember { mutableStateOf("") }
    var selectedOrderRef by remember { mutableStateOf<String?>(null) }
    var showUserPickerDropdown by remember { mutableStateOf(false) }

    // User's relevant orders
    val matchingOrders = remember(recipientEmail, orders) {
        if (recipientEmail.isBlank()) emptyList()
        else orders.filter { it.customerEmail.equals(recipientEmail.trim(), ignoreCase = true) }
    }

    val luxuryPresets = remember(recipientName) {
        val displayName = recipientName.ifBlank { "Valued Client" }
        listOf(
            "Order Dispatched" to "Dear $displayName, your handcrafted sleep piece has completed final artisan inspection in Jaipur and has been dispatched with live delivery tracking.",
            "Artisan Tailoring" to "Greetings $displayName, our Jaipur master craftsmen are currently handcrafting your bespoke commission. We will share your dispatch tracking shortly.",
            "Delivery Scheduling" to "Hello $displayName, our logistics concierge is preparing your shipment. Please let us know your preferred delivery window and date.",
            "Sleep Check-in" to "Dear $displayName, thank you for welcoming Good Dream into your sanctuary. How is your sleep experience? Our sleep specialists are here for any guidance."
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ForestGreenDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Direct Concierge Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Push direct to customer's notification panel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Informational Notice Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForestGreenContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, ForestGreenDark.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ForestGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "When sent, this message posts an immediate notification in the user's phone notification panel. Tapping the notification opens their in-app live chat so they can reply seamlessly.",
                            fontSize = 11.5.sp,
                            color = ForestGreenDark,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Recipient Info
                Text(
                    text = "RECIPIENT CLIENT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SatinGoldDark
                )

                if (targetUser == null && availableUsers.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showUserPickerDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (recipientEmail.isNotBlank()) "$recipientName ($recipientEmail)" else "Select client from CRM directory",
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = showUserPickerDropdown,
                            onDismissRequest = { showUserPickerDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            availableUsers.forEach { user ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(user.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        recipientEmail = user.email
                                        recipientName = user.name
                                        showUserPickerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text("Customer Email (Identifies Chat Session)") },
                    placeholder = { Text("client@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = targetUser == null
                )

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Customer Name") },
                    placeholder = { Text("Client Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = targetUser == null
                )

                // Associated Order (Optional)
                if (matchingOrders.isNotEmpty()) {
                    Text(
                        text = "ATTACH ORDER CONTEXT (OPTIONAL)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SatinGoldDark
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(matchingOrders, key = { it.id }) { order ->
                            val isSelected = selectedOrderRef == order.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedOrderRef = if (isSelected) null else order.id },
                                label = {
                                    Text(
                                        text = "${order.id.takeLast(8)} • ${order.status}",
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Luxury Quick Presets
                Text(
                    text = "LUXURY CONCIERGE PRESETS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SatinGoldDark
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(luxuryPresets, key = { it.first }) { (label, presetText) ->
                        OutlinedButton(
                            onClick = { messageText = presetText },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ForestGreenDark.copy(alpha = 0.4f))
                        ) {
                            Text(label, fontSize = 11.sp, color = ForestGreenDark, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Custom Message Input
                Text(
                    text = "MESSAGE CONTENT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SatinGoldDark
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Compose bespoke concierge message to user...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (recipientEmail.isNotBlank() && messageText.isNotBlank()) {
                                onSend(recipientEmail.trim(), recipientName.trim(), messageText.trim(), selectedOrderRef)
                            }
                        },
                        enabled = recipientEmail.isNotBlank() && messageText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send & Dispatch Notification",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

