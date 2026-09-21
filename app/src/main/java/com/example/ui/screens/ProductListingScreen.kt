package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.components.HeartWishlistButton
import com.example.ui.components.LuxuryAsyncImage
import com.example.ui.components.ProductListingSkeleton
import com.example.ui.components.breatheEffect
import com.example.ui.components.cushionPressEffect
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

val PREDICTIVE_FILTER_TAGS = listOf(
    "🛌 Orthopedic",
    "👑 King (72\"x78\")",
    "🌿 Belgian Latex",
    "🌀 Pocket Springs",
    "💰 Under ₹35,000",
    "💰 Under ₹50,000",
    "☁️ Ultra Plush",
    "🧱 Extra Firm"
)

@Composable
fun ProductListingScreen(
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    wishlistIds: Set<String>,
    onToggleWishlist: (String) -> Unit,
    onSelectProduct: (ProductEntity) -> Unit,
    onSelectCategory: (CategoryEntity?) -> Unit,
    onBackToCategories: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    userSleepProfile: String? = null,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        ProductListingSkeleton(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedSort by remember { mutableStateOf("Featured") }
    var filterOnlyNew by remember { mutableStateOf(false) }
    var selectedPredictiveTag by remember { mutableStateOf<String?>(null) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onSearchChange(spokenText)
                haptic.performLuxuryClick()
            }
        }
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    // Filter products by category, newness, name, category query, or predictive tags
    val filteredProducts = remember(products, selectedCategory, searchQuery, selectedSort, filterOnlyNew, selectedPredictiveTag, categoryMap) {
        var list = products

        if (selectedCategory != null) {
            list = list.filter { it.categoryId == selectedCategory.id }
        }

        if (filterOnlyNew) {
            list = list.filter { it.isNewLaunch }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter { product ->
                val catName = categoryMap[product.categoryId]?.name.orEmpty()
                product.title.contains(q, ignoreCase = true) ||
                product.subtitle.contains(q, ignoreCase = true) ||
                product.material.contains(q, ignoreCase = true) ||
                catName.contains(q, ignoreCase = true)
            }
        }

        if (selectedPredictiveTag != null) {
            list = list.filter { product ->
                when (selectedPredictiveTag) {
                    "🛌 Orthopedic" -> product.firmness.contains("Orthopedic", ignoreCase = true) ||
                            product.firmness.contains("Firm", ignoreCase = true) ||
                            product.title.contains("Orthopedic", ignoreCase = true) ||
                            product.description.contains("Orthopedic", ignoreCase = true)

                    "👑 King (72\"x78\")" -> product.dimensions.contains("72", ignoreCase = true) ||
                            product.dimensions.contains("King", ignoreCase = true) ||
                            product.title.contains("King", ignoreCase = true)

                    "🌿 Belgian Latex" -> product.material.contains("Latex", ignoreCase = true) ||
                            product.description.contains("Latex", ignoreCase = true) ||
                            product.title.contains("Latex", ignoreCase = true)

                    "🌀 Pocket Springs" -> product.material.contains("Spring", ignoreCase = true) ||
                            product.material.contains("Pocket", ignoreCase = true) ||
                            product.title.contains("Spring", ignoreCase = true)

                    "💰 Under ₹35,000" -> product.price <= 35000.0

                    "💰 Under ₹50,000" -> product.price <= 50000.0

                    "☁️ Ultra Plush" -> product.firmness.contains("Soft", ignoreCase = true) ||
                            product.firmness.contains("Plush", ignoreCase = true) ||
                            product.description.contains("Plush", ignoreCase = true)

                    "🧱 Extra Firm" -> product.firmness.contains("Firm", ignoreCase = true)

                    else -> true
                }
            }
        }

        when (selectedSort) {
            "Price: Low to High" -> list.sortedBy { it.price }
            "Price: High to Low" -> list.sortedByDescending { it.price }
            else -> list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Persistent Breadcrumb Navigation Trail pinned at the top
        PlpBreadcrumbTrail(
            selectedCategory = selectedCategory,
            categories = categories,
            onNavigateHome = onNavigateHome,
            onNavigateToCategories = onBackToCategories,
            onSelectCategory = onSelectCategory,
            modifier = Modifier.fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Top Header: Navigation & Title
        item(span = { GridItemSpan(2) }, contentType = "plp_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedCategory != null) {
                        IconButton(
                            onClick = onBackToCategories,
                            modifier = Modifier
                                .testTag("plp_back_button")
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Categories",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCategory?.name ?: "All Collections",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filteredProducts.size} items available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onRefresh != null) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .testTag("plp_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Catalog",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar with Category & Product Filtering and Clear Action
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plp_search_input"),
                    placeholder = { Text("Search by product name or category (e.g. mattress, sofa, cotton)...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.testTag("plp_clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak mattress, collection, or material...")
                                        }
                                        voiceSearchLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .testTag("plp_voice_search_button")
                                    .cushionPressEffect(pressedScale = 0.90f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = SatinGoldAccent
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = filterOnlyNew,
                            onClick = { filterOnlyNew = !filterOnlyNew },
                            label = { Text("✨ New Launches", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }

                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Predictive Smart Quick-Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "SMART PREDICTIVE FILTERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = SatinGoldAccent,
                            fontSize = 10.sp
                        )
                    }

                    if (selectedPredictiveTag != null) {
                        Text(
                            text = "Reset",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { selectedPredictiveTag = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PREDICTIVE_FILTER_TAGS, key = { it }) { tag ->
                        val isSelected = selectedPredictiveTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performLuxuryClick()
                                selectedPredictiveTag = if (isSelected) null else tag
                            },
                            label = {
                                Text(
                                    text = tag,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary,
                                selectedLabelColor = SatinGoldAccent,
                                selectedLeadingIconColor = SatinGoldAccent,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = SatinGoldAccent,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        }

        // Empty state
        if (filteredProducts.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                        .testTag("plp_empty_search_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No products match \"$searchQuery\"" else "No products found in this selection",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching by a product name (e.g. OrthoRest) or category (e.g. Mattresses, Living Room, Bedding)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("plp_empty_clear_search_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Clear Search",
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2-Column Product Grid Cards
        items(filteredProducts, key = { it.id }, contentType = { "product_grid_card" }) { product ->
            val isSleepMatch = remember(product, userSleepProfile) {
                !userSleepProfile.isNullOrBlank() && (
                    product.firmness.contains(userSleepProfile, ignoreCase = true) ||
                    product.title.contains(userSleepProfile, ignoreCase = true) ||
                    product.subtitle.contains(userSleepProfile, ignoreCase = true) ||
                    (userSleepProfile.contains("ortho", ignoreCase = true) && 
                        (product.title.contains("ortho", ignoreCase = true) || product.description.contains("ortho", ignoreCase = true)))
                )
            }
            ProductGridCard(
                product = product,
                isWishlisted = wishlistIds.contains(product.id),
                isSleepProfileMatch = isSleepMatch,
                onToggleWishlist = { onToggleWishlist(product.id) },
                onClick = { onSelectProduct(product) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
}

/**
 * Persistent Breadcrumb Navigation Trail at the top of Product Listing Screens (PLP).
 * Enables users to easily jump back to parent categories (Categories Hub) or Home,
 * as well as quick-switch between sibling product categories via dropdown.
 */
@Composable
fun PlpBreadcrumbTrail(
    selectedCategory: CategoryEntity?,
    categories: List<CategoryEntity>,
    onNavigateHome: (() -> Unit)?,
    onNavigateToCategories: () -> Unit,
    onSelectCategory: (CategoryEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("plp_breadcrumb_trail"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level 1: Home
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .cushionPressEffect(pressedScale = 0.94f)
                    .clickable { onNavigateHome?.invoke() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("plp_breadcrumb_home")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Home",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Separator >
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(10.dp)
            )

            // Level 2: Categories (Parent)
            val isCategoriesLeaf = selectedCategory == null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isCategoriesLeaf) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        } else {
                            Modifier
                                .cushionPressEffect(pressedScale = 0.94f)
                                .clickable { onNavigateToCategories() }
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("plp_breadcrumb_categories")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = if (isCategoriesLeaf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Categories",
                    fontSize = 12.sp,
                    fontWeight = if (isCategoriesLeaf) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCategoriesLeaf) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }

            // Level 3: Current Category with Quick-Switch Dropdown
            if (selectedCategory != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(10.dp)
                )

                var dropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .cushionPressEffect(pressedScale = 0.94f)
                            .clickable { dropdownExpanded = true }
                            .testTag("plp_breadcrumb_current_category")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedCategory.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Category",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Dropdown menu to jump to any parent or sibling category
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Collections (Categories Hub)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                dropdownExpanded = false
                                onNavigateToCategories()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat.name,
                                        fontWeight = if (cat.id == selectedCategory.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (cat.id == selectedCategory.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    if (cat.id == selectedCategory.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onSelectCategory(cat)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSleepProfileMatch: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    val formattedPrice = remember(product.price) {
        currencyFormatter.format(product.price).replace("INR", "₹").replace(".00", "")
    }
    val formattedOriginal = remember(product.originalPrice) {
        currencyFormatter.format(product.originalPrice).replace("INR", "₹").replace(".00", "")
    }

    Card(
        modifier = modifier
            .testTag("product_card_${product.id}")
            .fillMaxWidth()
            .cushionPressEffect(pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (product.isSpringhavenSeries) {
                    Modifier.border(1.5.dp, SatinGoldAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val primaryImage = product.getImagesList().firstOrNull() ?: ""
                LuxuryAsyncImage(
                    imageUrl = primaryImage,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize()
                )

                // SpringHaven Luxury Badge with gentle breathe pulse
                if (product.isSpringhavenSeries) {
                    Surface(
                        color = ForestGreenPrimary,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .breatheEffect(minScale = 0.97f, maxScale = 1.03f)
                    ) {
                        Text(
                            text = "SpringHaven 15\"",
                            color = SatinGoldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else if (product.isNewLaunch) {
                    Surface(
                        color = SatinGoldAccent,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "NEW LAUNCH",
                            color = ForestGreenDark,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Sleep Profile Match Badge
                if (isSleepProfileMatch) {
                    Surface(
                        color = SatinGoldDark,
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Sleep Match",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Wishlist Toggle with Spring Bounce Physics
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    HeartWishlistButton(
                        isWishlisted = isWishlisted,
                        onToggle = {
                            haptic.performLuxuryClick()
                            onToggleWishlist()
                        },
                        testTag = "wishlist_toggle_${product.id}",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Info Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedPrice,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (product.originalPrice > product.price) {
                        Text(
                            text = formattedOriginal,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Warranty badge
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${product.warrantyYears}-Year Warranty",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
