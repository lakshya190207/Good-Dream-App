package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderEntity
import com.example.ui.components.CelebrationBurstMode
import com.example.ui.components.GoldFoilCelebrationCanvas
import com.example.ui.components.cushionPressEffect
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimelineStatus {
    COMPLETED,
    IN_PROGRESS,
    UPCOMING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSuccessScreen(
    order: OrderEntity?,
    onTrackOrder: (String) -> Unit,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var celebrationTrigger by remember { mutableIntStateOf(1) }

    // Celebrate order confirmation with tactile haptic resonance
    LaunchedEffect(Unit) {
        haptics.performLuxurySuccess()
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH)
    }

    val estimatedDeliveryDate = remember(order?.createdAt) {
        val date = Date((order?.createdAt ?: System.currentTimeMillis()) + (4L * 24 * 60 * 60 * 1000))
        dateFormatter.format(date)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_order_success"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Order Confirmed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.performLuxuryClick()
                            onTrackOrder(order?.id ?: "GD-ORD-2026-00000")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_track_order"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Track Delivery →", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.performLuxuryClick()
                            val shareText = """
                                🏛️ GOOD DREAM SANCTUARY — OFFICIAL ORDER CONFIRMATION
                                ==================================================
                                Order Reference ID: ${order?.id ?: "GD-ORD-2026-99124"}
                                Client: ${order?.customerName ?: "Valued Sanctuary Patron"}
                                Estimated Delivery: $estimatedDeliveryDate
                                Delivery Slot: ${order?.deliverySlot ?: "Standard Slot"}

                                Handcrafted Sanctuary Items:
                                ${order?.itemsSummary ?: "SpringHaven™ Handcrafted Mattress"}

                                Total Valuation: ${currencyFormatter.format(order?.totalAmount ?: 48999.0)}
                                Payment Mode: ${order?.paymentMethod ?: "UPI"}
                                Delivery Destination:
                                ${order?.deliveryAddress ?: ""}, ${order?.city ?: ""}, ${order?.state ?: ""} - ${order?.pincode ?: ""}

                                Warranty: 25-Year SpringHaven™ Lifetime Orthopedic Guarantee
                                Customer Support: gooddreamshomedecor@gmail.com
                                ==================================================
                                Handcrafted with organic pride by Good Dream Sanctuary.
                            """.trimIndent()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Good Dream Sanctuary Order Confirmation - ${order?.id}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Sanctuary Order Confirmation"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SatinGoldAccent)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Sanctuary Order Receipt", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (order != null) {
                        OutlinedButton(
                            onClick = {
                                haptics.performLuxuryClick()
                                com.example.util.InvoicePrinterHelper.printOrderInvoice(
                                    context = context,
                                    order = order
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("button_download_invoice"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Official Tax Invoice (PDF)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.performLuxuryClick()
                            onContinueShopping()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Sanctuary Home", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebratory Animated Gold Seal of Authenticity (interactive celebration trigger)
                CelebratoryGoldSeal(
                    onClick = {
                        haptics.performLuxurySuccess()
                        celebrationTrigger++
                    }
                )

            Text(
                text = "Thank You, ${order?.customerName ?: "Valued Customer"}!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Your Good Dream handcrafted bedding is officially booked. Our master artisans are preparing your order for custom crafting and doorstep delivery.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            // Order Confirmation Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order Reference ID", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = order?.id ?: "GD-ORD-2026-99124",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SatinGoldAccent
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Delivery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = estimatedDeliveryDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ForestGreenPrimary
                        )
                    }

                    val isCod = order?.paymentMethod?.contains("COD", ignoreCase = true) == true ||
                            order?.paymentMethod?.contains("Cash on Delivery", ignoreCase = true) == true

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payment Status", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isCod) "COD (20% Advance Paid)" else (order?.paymentMethod ?: "Paid via UPI"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isCod) ForestGreenPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isCod) {
                        val totalVal = order?.totalAmount ?: 0.0
                        val advancePaid = (totalVal * 0.20).toLong()
                        val balanceDue = totalVal.toLong() - advancePaid

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cash on Delivery Policy Applied",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "• 20% Advance Booking Paid Online: ${currencyFormatter.format(advancePaid)}\n• 80% Balance Due Upon Delivery: ${currencyFormatter.format(balanceDue)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestGreenPrimary,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "Pay the remaining 80% balance to our delivery team via Cash, UPI QR, or Card machine only after in-room unboxing and inspection.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Customer & Delivery Snapshot
                    Text(
                        text = "Delivery Destination:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order?.customerName ?: ""}\n${order?.deliveryAddress ?: ""}\n${order?.city ?: ""}, ${order?.state ?: ""} - ${order?.pincode ?: ""}\nPhone: ${order?.customerPhone ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Items & Total
                    Text(
                        text = "Crafted Items:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = order?.itemsSummary ?: "SpringHaven™ Handcrafted Mattress",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCod) "Total Order Value" else "Total Valuation Paid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currencyFormatter.format(order?.totalAmount ?: 48999.0),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 4-Stage Live Delivery Journey Timeline
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order Delivery Journey",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SatinGoldContainer
                        ) {
                            Text(
                                text = "LIVE TRACKER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    TimelineStepItem(
                        stepNumber = 1,
                        title = "Order Reserved & Confirmed",
                        description = "Order recorded. Dedicated team assigned to oversee your bespoke bedding creation.",
                        timeEstimate = "Completed Just Now",
                        status = TimelineStatus.COMPLETED
                    )

                    TimelineStepItem(
                        stepNumber = 2,
                        title = "Atelier Crafting & Orthopedic Inspection",
                        description = "Hand-assembly of pocket springs and organic zero-VOC Belgian latex layers. Multi-point pressure mapping certified.",
                        timeEstimate = "In Progress • Est. 24–48 hrs",
                        status = TimelineStatus.IN_PROGRESS
                    )

                    TimelineStepItem(
                        stepNumber = 3,
                        title = "Dispatched via Delivery Fleet",
                        description = "Enclosed in triple-layer sterile breathable wraps and transit-monitored.",
                        timeEstimate = "Scheduled • Day 3",
                        status = TimelineStatus.UPCOMING
                    )

                    TimelineStepItem(
                        stepNumber = 4,
                        title = "In-Room Delivery & Setup",
                        description = "Delivery specialists carry mattress directly to your bedroom, unwrap, inspect, and remove legacy packaging.",
                        timeEstimate = "Estimated: $estimatedDeliveryDate",
                        status = TimelineStatus.UPCOMING,
                        isLast = true
                    )
                }
            }

            // Guarantee & Support Badge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = SatinGoldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "25-Year SpringHaven Guarantee Enrolled",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your digital warranty registration is automatically linked to your mobile number.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Hardware-Accelerated Gold Foil Confetti Celebration Shower
        GoldFoilCelebrationCanvas(
            trigger = celebrationTrigger,
            particleCount = 100,
            burstMode = CelebrationBurstMode.CENTER_EXPLOSION
        )
    }
}
}

@Composable
private fun CelebratoryGoldSeal(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seal_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .size(96.dp)
            .cushionPressEffect(pressedScale = 0.90f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Gold Halo
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SatinGoldAccent.copy(alpha = 0.22f))
        )

        // Middle Emerald Circle
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            // Gold Ring outline
            Surface(
                shape = CircleShape,
                color = ForestGreenDark,
                border = BorderStroke(2.dp, SatinGoldAccent),
                modifier = Modifier.size(74.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Authenticity Guaranteed",
                        tint = SatinGoldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineStepItem(
    stepNumber: Int,
    title: String,
    description: String,
    timeEstimate: String,
    status: TimelineStatus,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            TimelineStatus.COMPLETED -> ForestGreenPrimary
                            TimelineStatus.IN_PROGRESS -> SatinGoldAccent
                            TimelineStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    TimelineStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    TimelineStatus.IN_PROGRESS -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    TimelineStatus.UPCOMING -> {
                        Text(
                            text = stepNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(
                            if (status == TimelineStatus.COMPLETED) ForestGreenPrimary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (status) {
                        TimelineStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        TimelineStatus.IN_PROGRESS -> SatinGoldContainer
                        TimelineStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (status) {
                            TimelineStatus.COMPLETED -> "VERIFIED"
                            TimelineStatus.IN_PROGRESS -> "IN PROGRESS"
                            TimelineStatus.UPCOMING -> "SCHEDULED"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            TimelineStatus.COMPLETED -> ForestGreenDark
                            TimelineStatus.IN_PROGRESS -> ForestGreenDark
                            TimelineStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeEstimate,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status == TimelineStatus.IN_PROGRESS) SatinGoldDark else ForestGreenPrimary
            )
        }
    }
}
