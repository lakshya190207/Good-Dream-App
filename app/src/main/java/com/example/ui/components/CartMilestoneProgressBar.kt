package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class CartMilestone(
    val threshold: Double,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val valueLabel: String,
    val icon: ImageVector
)

val CART_MILESTONES = listOf(
    CartMilestone(
        threshold = 25000.0,
        title = "White-Glove Installation",
        shortLabel = "Free Setup",
        subtitle = "Complimentary room placement & old mattress removal",
        valueLabel = "₹2,500 Value",
        icon = Icons.Outlined.LocalShipping
    ),
    CartMilestone(
        threshold = 50000.0,
        title = "Mulberry Silk Sleep Mask",
        shortLabel = "Silk Mask & Mist",
        subtitle = "Pure mulberry silk eye mask & lavender sleep mist",
        valueLabel = "₹2,499 Value",
        icon = Icons.Outlined.Bedtime
    ),
    CartMilestone(
        threshold = 75000.0,
        title = "Dual Contour Pillows",
        shortLabel = "Pillow Pair",
        subtitle = "Pair of orthopedic memory foam contour pillows",
        valueLabel = "₹6,999 Value",
        icon = Icons.Outlined.CardGiftcard
    )
)

/**
 * Luxury Gamified Cart Milestone & Complimentary Gift Progress Meter.
 * Displays real-time progress toward unlocking White-Glove Setup, Mulberry Silk Masks,
 * and Orthopedic Contour Pillows with smooth animations and dynamic urgency teasers.
 */
@Composable
fun CartMilestoneProgressBar(
    currentTotal: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    val maxThreshold = 75000.0
    val targetProgress = (currentTotal / maxThreshold).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cart_milestone_progress"
    )

    // Calculate next unlockable milestone
    val nextMilestone = CART_MILESTONES.firstOrNull { currentTotal < it.threshold }
    val amountNeeded = nextMilestone?.let { it.threshold - currentTotal } ?: 0.0
    val unlockedCount = CART_MILESTONES.count { currentTotal >= it.threshold }

    var miniCelebrationTrigger by remember { mutableIntStateOf(0) }
    var previousUnlockedCount by remember { mutableIntStateOf(unlockedCount) }

    LaunchedEffect(unlockedCount) {
        if (unlockedCount > previousUnlockedCount) {
            miniCelebrationTrigger++
        }
        previousUnlockedCount = unlockedCount
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
            border = BorderStroke(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        SatinGoldAccent.copy(alpha = 0.8f),
                        SatinGoldLight.copy(alpha = 0.95f),
                        SatinGoldAccent.copy(alpha = 0.6f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {
            // Header: Status or Next Milestone Teaser
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SatinGoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unlockedCount == 3) Icons.Default.Stars else Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (unlockedCount == 3) "ALL VIP PRIVILEGES UNLOCKED" else "SANCTUARY REWARDS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = SatinGoldAccent,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (nextMilestone != null) {
                                "Add ${currencyFormatter.format(amountNeeded).replace("INR", "₹").replace(".00", "")} for ${nextMilestone.shortLabel}"
                            } else {
                                "🎉 All 3 Complimentary Gifts Activated!"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SatinGoldAccent.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$unlockedCount / 3 Unlocked",
                        color = SatinGoldLight,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Stage Progress Bar with 3 Nodes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )

                // Filled Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SatinGoldAccent,
                                    SatinGoldLight,
                                    Color(0xFFFFDF78)
                                )
                            )
                        )
                )

                // Milestone Nodes along the line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CART_MILESTONES.forEach { milestone ->
                        val isUnlocked = currentTotal >= milestone.threshold
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isUnlocked) SatinGoldAccent else ForestGreenPrimary)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isUnlocked) SatinGoldLight else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Unlocked",
                                    tint = ForestGreenDark,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Milestone Gift Cards in Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CART_MILESTONES.forEach { milestone ->
                    val isUnlocked = currentTotal >= milestone.threshold
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUnlocked) SatinGoldAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (isUnlocked) SatinGoldAccent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.width(170.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = milestone.icon,
                                    contentDescription = null,
                                    tint = if (isUnlocked) SatinGoldAccent else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isUnlocked) ForestGreenPrimary else Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = if (isUnlocked) "UNLOCKED ✓" else "AT ₹${(milestone.threshold / 1000).toInt()}k",
                                        color = if (isUnlocked) SatinGoldLight else Color.White.copy(alpha = 0.7f),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.8f),
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )

                            Text(
                                text = milestone.valueLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SatinGoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (miniCelebrationTrigger > 0) {
        GoldFoilCelebrationCanvas(
            trigger = miniCelebrationTrigger,
            particleCount = 50,
            burstMode = CelebrationBurstMode.CENTER_EXPLOSION
        )
    }
}
}
