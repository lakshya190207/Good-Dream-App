package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Memory Foam Cushion Press Effect.
 * Gives interactive surfaces a soothing, soft compression on touch and gentle spring recovery,
 * mimicking resting on an ultra-plush memory foam mattress.
 */
fun Modifier.cushionPressEffect(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cushion_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Calming Restful Breathing Pulse Effect.
 * A 4-second continuous sine loop that mimics slow, calming diaphragmatic breathing,
 * bringing a peaceful, living presence to hero elements.
 */
fun Modifier.breatheEffect(
    minAlpha: Float = 0.92f,
    maxAlpha: Float = 1.0f,
    minScale: Float = 1.0f,
    maxScale: Float = 1.02f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe_loop")

    val scale = infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    val alpha = infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_alpha"
    )

    this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        this.alpha = alpha.value
    }
}

/**
 * Bouncy Heart Wishlist Icon with spring physics and smooth color transition.
 */
@Composable
fun HeartWishlistButton(
    isWishlisted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "wishlist_heart_button"
) {
    var previousWishlisted by remember { mutableStateOf(isWishlisted) }
    val scaleAnim = remember { Animatable(1.0f) }

    LaunchedEffect(isWishlisted) {
        if (isWishlisted != previousWishlisted) {
            previousWishlisted = isWishlisted
            // Play gentle pop and bounce
            scaleAnim.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    val iconColor by animateColorAsState(
        targetValue = if (isWishlisted) StatusError else ForestGreenPrimary,
        animationSpec = tween(durationMillis = 220),
        label = "heart_color"
    )

    IconButton(
        onClick = onToggle,
        modifier = modifier
            .testTag(testTag)
            .cushionPressEffect(pressedScale = 0.88f)
    ) {
        Icon(
            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isWishlisted) "Remove from Wishlist" else "Save to Wishlist",
            tint = iconColor,
            modifier = Modifier
                .size(20.dp)
                .scale(scaleAnim.value)
        )
    }
}

/**
 * Animated number badge for Top App Bar and Drawer with smooth digit transitions.
 */
@Composable
fun AnimatedBadgeNumber(
    count: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
            } else {
                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> height } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "badge_number_animation",
        modifier = modifier
    ) { targetCount ->
        Text(
            text = if (targetCount > 99) "99+" else "$targetCount",
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Gentle floating animation for cards and visual badges.
 */
fun Modifier.gentleFloatingEffect(
    distanceDp: Float = 4f,
    durationMillis: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_effect")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -distanceDp,
        targetValue = distanceDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    this.graphicsLayer {
        translationY = offsetY
    }
}

/**
 * Sleep Sanctuary & Comfort Guide Card.
 * Designed to make the user feel deeply comfortable, grounded, and informed about
 * ergonomics, sleep temperature, and restorative living.
 */
@Composable
fun ComfortSleepSanctuaryCard(
    onExploreComfort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val comfortTips = remember {
        listOf(
            "Natural Latex & Pocket Coils align your spine to eliminate morning lumbar stiffness.",
            "100% Breathable Egyptian Cotton preserves a cool 19°C micro-climate for deep REM rest.",
            "Zero-Motion Transfer pocket springs ensure total silence when your partner moves.",
            "Contour Gel Memory Foam gently cradles neck cervical curves to relieve tension.",
            "Multi-Zone Ergonomic Zoning distributes body weight evenly across 7 key zones."
        )
    }

    var tipIndex by remember { mutableStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .cushionPressEffect()
            .testTag("home_sleep_sanctuary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Good Dream Sanctuary™",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Engineered for Restorative Sleep",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Gentle night comfort badge with subtle breathing pulse
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.breatheEffect(minScale = 0.98f, maxScale = 1.02f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rest Score 99%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Comfort Metrics Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComfortMetricPill(
                        icon = Icons.Default.Air,
                        title = "Cool Airflow",
                        subtitle = "19°C Balance",
                        modifier = Modifier.weight(1f)
                    )
                    ComfortMetricPill(
                        icon = Icons.Default.AllInclusive,
                        title = "Zero Vibration",
                        subtitle = "No Disturbance",
                        modifier = Modifier.weight(1f)
                    )
                    ComfortMetricPill(
                        icon = Icons.Default.SelfImprovement,
                        title = "Spine Relief",
                        subtitle = "7-Zone Curve",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Rest Tip Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tipIndex = (tipIndex + 1) % comfortTips.size
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        AnimatedContent(
                            targetState = comfortTips[tipIndex],
                            transitionSpec = {
                                (fadeIn(tween(260)) + slideInVertically { it / 2 }) togetherWith
                                        (fadeOut(tween(180)) + slideOutVertically { -it / 2 })
                            },
                            label = "tip_transition",
                            modifier = Modifier.weight(1f)
                        ) { tipText ->
                            Text(
                                text = tipText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Next Tip",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComfortMetricPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
