package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Luxury floating Dynamic Island-style notification banner for added-to-cart events.
 * Eliminates standard Android snackbars/toasts in favor of an elegant Forest Green & Satin Gold
 * boutique experience with live product thumbnail, item count, pricing breakdown, trust perks,
 * animated countdown hairline, and instant 1-tap cart checkout navigation.
 */
@Composable
fun LuxuryAddToCartNotification(
    product: ProductEntity,
    quantity: Int = 1,
    totalCartCount: Int = 1,
    onViewCart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    displayDurationMs: Int = 4200
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    val formattedPrice = remember(product.price) {
        currencyFormatter.format(product.price).replace("INR", "₹").replace(".00", "")
    }

    val formattedSubtotal = remember(product.price, quantity) {
        currencyFormatter.format(product.price * quantity).replace("INR", "₹").replace(".00", "")
    }

    val primaryImageUrl = remember(product) {
        product.getImagesList().firstOrNull() ?: ""
    }

    // Auto-dismiss countdown animation
    val progress = remember { Animatable(1f) }

    LaunchedEffect(product.id, quantity) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = displayDurationMs, easing = LinearEasing)
        )
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ForestGreenDark,
        shadowElevation = 14.dp,
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    SatinGoldAccent.copy(alpha = 0.85f),
                    SatinGoldLight.copy(alpha = 0.95f),
                    SatinGoldAccent.copy(alpha = 0.7f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewCart() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail with Gold Trim
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSubtle)
                ) {
                    LuxuryAsyncImage(
                        imageUrl = primaryImageUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Checkmark Badge in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product Info & Price Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ADDED TO SANCTUARY CART",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = SatinGoldAccent,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (quantity > 1) "$quantity × $formattedPrice ($formattedSubtotal)" else formattedPrice,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SatinGoldLight,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "• 25-Year Warranty",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 1-Tap "View Cart" Action
                Button(
                    onClick = onViewCart,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SatinGoldAccent,
                        contentColor = ForestGreenDark
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (totalCartCount > 1) "Cart ($totalCartCount) →" else "View Cart →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Close / Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Animated Countdown Progress Hairline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.value)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SatinGoldAccent,
                                    SatinGoldLight
                                )
                            )
                        )
                )
            }
        }
    }
}
