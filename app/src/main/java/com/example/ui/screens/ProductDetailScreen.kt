package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ProductEntity
import com.example.ui.components.BedroomFitVisualizer
import com.example.ui.components.BedSizePreset
import com.example.ui.components.FullscreenFabricInspectorModal
import com.example.ui.components.HeartWishlistButton
import com.example.ui.components.LuxuryAsyncImage
import com.example.ui.components.OrthopedicSpinePressureVisualizer
import com.example.ui.components.breatheEffect
import com.example.ui.components.cushionPressEffect
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductDetailScreen(
    product: ProductEntity,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onInquireProduct: (ProductEntity) -> Unit,
    onBack: () -> Unit,
    onOpenCart: (() -> Unit)? = null,
    onOpenBespokeStudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isRecentlyAddedToCart by remember { mutableStateOf(false) }
    var isFabricInspectorOpen by remember { mutableStateOf(false) }

    val images = remember(product) { product.getImagesList() }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var selectedQuantity by remember { mutableStateOf(1) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }
    val formattedPrice = currencyFormatter.format(product.price).replace("INR", "₹").replace(".00", "")
    val formattedOriginal = currencyFormatter.format(product.originalPrice).replace("INR", "₹").replace(".00", "")
    val specifications = remember(product) { product.getSpecificationsMap() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("pdp_back_button")
                            .cushionPressEffect(pressedScale = 0.88f)
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Native Luxury Share Sheet Button
                        IconButton(
                            onClick = {
                                haptic.performLuxuryClick()
                                val collectionName = if (product.isSpringhavenSeries) "SpringHaven Luxury Edition" else "Sanctuary Rest Collection"
                                val shareMessage = buildString {
                                    append("✨ Discover the Good Dream ${product.title}\n")
                                    append("💎 Collection: $collectionName\n")
                                    append("🛏️ Dimensions: ${product.dimensions} • Firmness: ${product.firmness}\n")
                                    append("🏷️ Price: $formattedPrice\n\n")
                                    append("Experience luxury rest at Good Dream Sanctuary:\nhttps://good-dream-75b67.web.app")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, product.title)
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Sanctuary Piece"))
                            },
                            modifier = Modifier
                                .cushionPressEffect(pressedScale = 0.88f)
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .testTag("pdp_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        HeartWishlistButton(
                            isWishlisted = isWishlisted,
                            onToggle = {
                                haptic.performLuxuryClick()
                                onToggleWishlist()
                            },
                            testTag = "pdp_wishlist_button",
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                        )
                        if (onOpenCart != null) {
                            IconButton(
                                onClick = onOpenCart,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingCart,
                                    contentDescription = "Open Cart",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Action Bar: Inquire & Add to Cart
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Inquire Button (Custom inquiry / Phase 1 lead capture)
                    OutlinedButton(
                        onClick = { onInquireProduct(product) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .cushionPressEffect(pressedScale = 0.95f)
                            .testTag("pdp_inquire_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Add to Cart Button with In-Context Feedback
                    Button(
                        onClick = {
                            haptic.performLuxurySuccess()
                            onAddToCart(selectedQuantity)
                            isRecentlyAddedToCart = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2200)
                                isRecentlyAddedToCart = false
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .cushionPressEffect(pressedScale = 0.95f)
                            .testTag("pdp_add_to_cart_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        AnimatedContent(
                            targetState = isRecentlyAddedToCart,
                            label = "add_to_cart_state"
                        ) { added ->
                            if (added) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SatinGoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Added to Cart ✓", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Cart", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {

            // Primary Image Showcase Box with Fluid Animated Crossfade & Fullscreen Atelier Zoom Trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceSubtle)
                    .clickable {
                        haptic.performLuxuryClick()
                        isFabricInspectorOpen = true
                    }
            ) {
                val currentImageUrl = images.getOrNull(selectedImageIndex) ?: ""
                AnimatedContent(
                    targetState = currentImageUrl,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)))
                            .togetherWith(fadeOut(animationSpec = tween(180)))
                    },
                    label = "pdp_image_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetUrl ->
                    LuxuryAsyncImage(
                        imageUrl = targetUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // SpringHaven Banner with soft breathe pulse
                if (product.isSpringhavenSeries) {
                    Surface(
                        color = ForestGreenPrimary,
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .breatheEffect(minScale = 0.97f, maxScale = 1.03f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SPRINGHAVEN LUXURY • 15\" INTEGRATED BED SETUP",
                                color = SatinGoldAccent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Floating Atelier Fabric Inspector Trigger Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom Fabric",
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "350% Zoom • Inspect Weave",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Image Thumbnail Strip
            if (images.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(images) { index, imgUrl ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selectedImageIndex == index) 2.dp else 1.dp,
                                    color = if (selectedImageIndex == index) SatinGoldAccent else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .cushionPressEffect(pressedScale = 0.92f)
                                .clickable {
                                    haptic.performLuxuryClick()
                                    selectedImageIndex = index
                                }
                        ) {
                            LuxuryAsyncImage(
                                imageUrl = imgUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Title, Subtitle, Price Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formattedPrice,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (product.originalPrice > product.price) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formattedOriginal,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val savingsPercent = if (product.originalPrice > product.price) {
                                (((product.originalPrice - product.price) / product.originalPrice) * 100).toInt()
                            } else 0
                            if (savingsPercent > 0) {
                                Text(
                                    text = "$savingsPercent% OFF VIP PRICE",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Quantity Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedQuantity > 1) {
                                        haptic.performLuxuryAdjustment()
                                        selectedQuantity--
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .cushionPressEffect(pressedScale = 0.88f)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                            }

                            AnimatedContent(
                                targetState = selectedQuantity,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut()
                                        )
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> height } + fadeOut()
                                        )
                                    }
                                },
                                label = "pdp_quantity_stepper"
                            ) { qty ->
                                Text(
                                    text = "$qty",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedQuantity < 10) {
                                        haptic.performLuxuryAdjustment()
                                        selectedQuantity++
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .cushionPressEffect(pressedScale = 0.88f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Bespoke Sleep Atelier Customizer Card
            if (onOpenBespokeStudio != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .cushionPressEffect(pressedScale = 0.96f)
                        .clickable {
                            haptic.performLuxuryClick()
                            onOpenBespokeStudio()
                        }
                        .testTag("pdp_bespoke_atelier_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
                    border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF163E30)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Need Custom Sizing or Firmness?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = SatinGoldAccent) {
                                    Text(
                                        text = "BESPOKE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ForestGreenDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Design your custom mattress layer by layer in our 3D Studio →",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Feature A: WhatsApp VIP Concierge Consultation Card
            val pdpContext = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .cushionPressEffect(pressedScale = 0.96f)
                    .clickable {
                        val message = "Hello Good Dream Team! I am interested in ${product.title} (SKU: ${product.sku}, $formattedPrice). Could you please assist me with custom sizing, firmness ratings, and delivery timelines?"
                        val encodedMessage = Uri.encode(message)
                        val url = "https://api.whatsapp.com/send?phone=+918041239999&text=$encodedMessage"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            pdpContext.startActivity(intent)
                        } catch (e: Exception) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918041239999"))
                            pdpContext.startActivity(dialIntent)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp Concierge",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VIP WhatsApp Consultation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SatinGoldAccent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "FAST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Chat with our sleep specialist about custom sizes, firmness & delivery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Feature: Craftsmanship Video Showcase Card (if available)
            if (product.hasVideo()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clickable {
                            try {
                                val videoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(product.videoUrl))
                                pdpContext.startActivity(videoIntent)
                            } catch (e: Exception) {
                                // Graceful fallback
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = SatinGoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Craftsmanship Video Showcase",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SatinGoldAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "HD TOUR",
                                    color = SatinGoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Video Banner with Play Scrim Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (images.isNotEmpty()) {
                                LuxuryAsyncImage(
                                    imageUrl = images.first(),
                                    contentDescription = "Video Preview Thumbnail",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .matchParentSize()
                                )
                            }

                            // Dark gradient scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )

                            // Center Play Button & CTA
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(SatinGoldAccent)
                                        .cushionPressEffect(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = ForestGreenDark,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to Stream Master Tour",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Watch the internal architecture, edge reinforcement, and Belgian cashmere quilting in motion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Description Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Craftsmanship & Overview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Complete Specs Key-Value Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Technical Specifications",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Base specs
                    SpecTableRow("Dimensions", product.dimensions)
                    SpecTableRow("Thickness / Profile", "${product.thicknessInches} Inches")
                    SpecTableRow("Core Material", product.material)
                    SpecTableRow("Firmness Rating", product.firmness)
                    SpecTableRow("Warranty Coverage", "${product.warrantyYears} Years Direct Replacement")

                    // Dynamic specs
                    specifications.forEach { (key, value) ->
                        SpecTableRow(key, value)
                    }
                }
            }

            // Interactive 3D Orthopedic Spine Alignment & Pressure Map Simulator
            OrthopedicSpinePressureVisualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            // Interactive 2D Architectural Bedroom Scale & Dimension Fit Visualizer
            val initialBedSize = remember(product.dimensions) {
                when {
                    product.dimensions.contains("Queen", ignoreCase = true) || product.dimensions.contains("60") -> BedSizePreset.QUEEN
                    product.dimensions.contains("Single", ignoreCase = true) || product.dimensions.contains("36") -> BedSizePreset.SINGLE
                    else -> BedSizePreset.KING
                }
            }
            BedroomFitVisualizer(
                initialBedSize = initialBedSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            // Good Dream Trust & Warranty Badge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "${product.warrantyYears}-Year Good Dream Warranty",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Comprehensive structural protection & sagging guarantee backed by Good Dream Home Decor Private Limited.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Statutory Legal Metrology & Consumer Protection Disclosures (Legal Metrology Rules 2011 & E-Commerce Rules 2020)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Legal Metrology Disclosures",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "INDIA COMPLIANT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Mandatory consumer disclosures under the Legal Metrology (Packaged Commodities) Rules 2011 and Consumer Protection (E-Commerce) Rules 2020:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SpecTableRow("Country of Origin", "India")
                    SpecTableRow("Manufacturer & Packer", "Good Dream Luxury Home Decor Pvt. Ltd.\nPlot 42, KIADB Industrial Area, Phase II, Whitefield, Bengaluru, Karnataka - 560066")
                    SpecTableRow("Generic Name", "Luxury Orthopedic Mattress")
                    SpecTableRow("Net Quantity", "1 Unit Mattress")
                    SpecTableRow("Physical Dimensions", "${product.dimensions} × ${product.thicknessInches}\" Profile")
                    SpecTableRow("Maximum Retail Price", "$formattedOriginal (Inclusive of all Taxes / 18% GST)")
                    SpecTableRow("Customer Care Helpline", "1800-425-9999 (Toll-Free) | care@gooddream.in")
                    SpecTableRow("Grievance Redressal", "Nodal Grievance Officer | grievance@gooddream.in\nAck: 48h • Resolution: 30 days")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (isFabricInspectorOpen) {
        FullscreenFabricInspectorModal(
            product = product,
            initialImageIndex = selectedImageIndex,
            onDismiss = { isFabricInspectorOpen = false }
        )
    }
}

@Composable
private fun SpecTableRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.3f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
