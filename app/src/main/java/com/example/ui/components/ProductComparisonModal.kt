package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductEntity
import com.example.ui.theme.*

data class ComparisonRow(
    val featureName: String,
    val value1: String,
    val value2: String,
    val isHighlighted: Boolean = false
)

@Composable
fun ProductComparisonModal(
    mattressProducts: List<ProductEntity>,
    onSelectProduct: (ProductEntity) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var product1 by remember(mattressProducts) {
        mutableStateOf(
            mattressProducts.firstOrNull { it.isSpringhavenSeries } ?: mattressProducts.firstOrNull()
        )
    }

    var product2 by remember(mattressProducts) {
        mutableStateOf(
            mattressProducts.firstOrNull { it.id != product1?.id } ?: mattressProducts.getOrNull(1) ?: mattressProducts.firstOrNull()
        )
    }

    var showPickerForProduct1 by remember { mutableStateOf(false) }
    var showPickerForProduct2 by remember { mutableStateOf(false) }

    val rows = remember(product1, product2) {
        val p1 = product1
        val p2 = product2
        listOf(
            ComparisonRow(
                featureName = "Total Setup Height",
                value1 = if (p1 != null) "${p1.thicknessInches}\" Deep Luxury Profile" else "N/A",
                value2 = if (p2 != null) "${p2.thicknessInches}\" Deep Luxury Profile" else "N/A",
                isHighlighted = true
            ),
            ComparisonRow(
                featureName = "Firmness Rating",
                value1 = p1?.firmness ?: "Medium Firm",
                value2 = p2?.firmness ?: "Medium Firm"
            ),
            ComparisonRow(
                featureName = "Core Engineering & Materials",
                value1 = p1?.material ?: "Premium Materials",
                value2 = p2?.material ?: "Premium Materials"
            ),
            ComparisonRow(
                featureName = "Mattress Dimensions",
                value1 = p1?.dimensions ?: "78\" x 72\"",
                value2 = p2?.dimensions ?: "78\" x 72\""
            ),
            ComparisonRow(
                featureName = "Sleep Trial Period",
                value1 = "100 Nights Risk-Free",
                value2 = "100 Nights Risk-Free"
            ),
            ComparisonRow(
                featureName = "Warranty Guarantee",
                value1 = if (p1 != null) "${p1.warrantyYears}-Year Craftsmanship" else "5-Year Guarantee",
                value2 = if (p2 != null) "${p2.warrantyYears}-Year Craftsmanship" else "5-Year Guarantee",
                isHighlighted = true
            ),
            ComparisonRow(
                featureName = "Price & Value",
                value1 = if (p1 != null) "₹${p1.price.toInt()} (M.R.P. ₹${p1.originalPrice.toInt()})" else "N/A",
                value2 = if (p2 != null) "₹${p2.price.toInt()} (M.R.P. ₹${p2.originalPrice.toInt()})" else "N/A"
            ),
            ComparisonRow(
                featureName = "Delivery Service",
                value1 = if (p1?.isSpringhavenSeries == true) "White-Glove In-Room Assembly" else "Complimentary Express Delivery",
                value2 = if (p2?.isSpringhavenSeries == true) "White-Glove In-Room Assembly" else "Complimentary Express Delivery"
            ),
            ComparisonRow(
                featureName = "Motion Isolation",
                value1 = if (p1?.isSpringhavenSeries == true) "99% Zero-Disturbance Coils" else "High-Density Motion Damping",
                value2 = if (p2?.isSpringhavenSeries == true) "99% Zero-Disturbance Coils" else "High-Density Motion Damping"
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_comparison_dialog"),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
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
                                contentDescription = "Close Comparison",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Side-by-Side Mattress Comparison",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Compare specs, thickness & engineering before you choose",
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
                    // Two Product Header Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Product 1 Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    val thumb = product1?.getImagesList()?.firstOrNull() ?: ""
                                    LuxuryAsyncImage(
                                        imageUrl = thumb,
                                        contentDescription = product1?.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product1?.title ?: "Select Mattress",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Switch",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { showPickerForProduct1 = true }
                                            .padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "₹${product1?.price?.toInt() ?: 0}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        product1?.let {
                                            onClose()
                                            onSelectProduct(it)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Select", fontSize = 12.sp)
                                }
                            }
                        }

                        // Product 2 Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SatinGoldAccent)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    val thumb = product2?.getImagesList()?.firstOrNull() ?: ""
                                    LuxuryAsyncImage(
                                        imageUrl = thumb,
                                        contentDescription = product2?.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product2?.title ?: "Select Mattress",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Switch",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SatinGoldDark,
                                        modifier = Modifier
                                            .clickable { showPickerForProduct2 = true }
                                            .padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "₹${product2?.price?.toInt() ?: 0}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        product2?.let {
                                            onClose()
                                            onSelectProduct(it)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Select", fontSize = 12.sp, color = TextPrimaryDark)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Comparison Table
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            rows.forEachIndexed { index, row ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (row.isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = row.featureName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = row.value1,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = row.value2,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (index < rows.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 100-Night Sleep Trial Guarantee Banner
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Both mattresses include Good Dream's 100-Night Risk-Free In-Home Trial with 100% money-back guarantee.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

    if (showPickerForProduct1) {
        AlertDialog(
            onDismissRequest = { showPickerForProduct1 = false },
            title = { Text("Choose Mattress 1", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mattressProducts.forEach { prod ->
                        val isSelected = prod.id == product1?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    product1 = prod
                                    showPickerForProduct1 = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "₹${prod.price.toInt()} • ${prod.firmness} • ${prod.thicknessInches}\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerForProduct1 = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPickerForProduct2) {
        AlertDialog(
            onDismissRequest = { showPickerForProduct2 = false },
            title = { Text("Choose Mattress 2", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mattressProducts.forEach { prod ->
                        val isSelected = prod.id == product2?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    product2 = prod
                                    showPickerForProduct2 = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "₹${prod.price.toInt()} • ${prod.firmness} • ${prod.thicknessInches}\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerForProduct2 = false }) {
                    Text("Close")
                }
            }
        )
    }
}
