package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductEntity
import com.example.ui.theme.*

/**
 * Immersive Fullscreen Atelier Fabric Inspector Modal.
 * Enables clients to inspect microscopic textile craftsmanship, Belgian damask weave,
 * natural latex cores, and quilting patterns up to 350% magnification with fluid pinch-to-zoom,
 * double-tap toggles, pan tracking, and preset zoom snapping.
 */
@Composable
fun FullscreenFabricInspectorModal(
    product: ProductEntity,
    initialImageIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val images = remember(product) { product.getImagesList().ifEmpty { listOf("") } }
    var currentImageIndex by remember { mutableIntStateOf(initialImageIndex.coerceIn(0, images.size - 1)) }

    // Multi-touch Zoom & Pan State
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "zoom_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040A07))
                .testTag("fullscreen_fabric_inspector_modal")
        ) {
            // Interactive Pan & Zoom Image Canvas
            val currentImageUrl = images.getOrNull(currentImageIndex).orEmpty()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentImageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptic.performLuxuryClick()
                                if (rawScale > 1.2f) {
                                    rawScale = 1f
                                    rawOffsetX = 0f
                                    rawOffsetY = 0f
                                } else {
                                    rawScale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(currentImageIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (rawScale * zoom).coerceIn(1f, 3.5f)
                            rawScale = newScale
                            if (newScale > 1f) {
                                val maxPan = 400f * (newScale - 1f)
                                rawOffsetX = (rawOffsetX + pan.x).coerceIn(-maxPan, maxPan)
                                rawOffsetY = (rawOffsetY + pan.y).coerceIn(-maxPan, maxPan)
                            } else {
                                rawOffsetX = 0f
                                rawOffsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                LuxuryAsyncImage(
                    imageUrl = currentImageUrl,
                    contentDescription = "Craftsmanship Magnified View",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 80.dp)
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = rawOffsetX,
                            translationY = rawOffsetY
                        )
                )
            }

            // Top Floating HUD (Close, Title, Zoom Pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close Button
                    IconButton(
                        onClick = {
                            haptic.performLuxuryClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, SatinGoldAccent.copy(alpha = 0.6f), CircleShape)
                            .cushionPressEffect(pressedScale = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Inspector",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Title & Index
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ATELIER FABRIC INSPECTOR",
                            color = SatinGoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${currentImageIndex + 1} of ${images.size}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Zoom Percentage Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "🔍 ${(animatedScale * 100).toInt()}%",
                            color = SatinGoldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Floating HUD (Craftsmanship Info, Preset Zoom Controls, Thumbnails)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preset Zoom Snap Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(1.0f to "100%", 2.0f to "200%", 3.5f to "350% MAX")
                    presets.forEach { (targetZoom, label) ->
                        val isCurrent = (rawScale - targetZoom).let { it >= -0.1f && it <= 0.1f }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) SatinGoldAccent else Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) SatinGoldAccent else Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .cushionPressEffect(pressedScale = 0.94f)
                                .clickable {
                                    haptic.performLuxuryClick()
                                    rawScale = targetZoom
                                    if (targetZoom == 1.0f) {
                                        rawOffsetX = 0f
                                        rawOffsetY = 0f
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                color = if (isCurrent) ForestGreenDark else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Craftsmanship Specs Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ForestGreenDark.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${product.material} • ${product.firmness} Profile",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Thumbnail Scrub Strip (if multi-image)
                if (images.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(images, key = { idx, url -> "$idx-$url" }) { idx, url ->
                            val isSelected = currentImageIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SatinGoldAccent else Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .cushionPressEffect(pressedScale = 0.92f)
                                    .clickable {
                                        haptic.performLuxuryClick()
                                        currentImageIndex = idx
                                        rawScale = 1f
                                        rawOffsetX = 0f
                                        rawOffsetY = 0f
                                    }
                            ) {
                                LuxuryAsyncImage(
                                    imageUrl = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
