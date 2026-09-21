package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class SleepPosture(val label: String, val description: String) {
    BACK("Back Sleep", "Lumbar lordosis & sacral weight distribution"),
    SIDE("Side Sleep", "Shoulder sinkage & pelvic horizontal alignment"),
    STOMACH("Stomach Sleep", "Thoracic support & zero hyper-extension")
}

enum class MattressTech(val label: String) {
    CONVENTIONAL("Conventional Mattress"),
    GOOD_DREAM_7ZONE("Good Dream 7-Zone System")
}

/**
 * Interactive 3D Orthopedic Spine Alignment & Pressure Map Simulator.
 * Demonstrates real-time anatomical biomechanics, spinal vertebrae deviation,
 * and pressure dispersion comparison between conventional bedding and Good Dream's
 * Swedish 7-zone active pocket coil + natural latex core.
 */
@Composable
fun OrthopedicSpinePressureVisualizer(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedPosture by remember { mutableStateOf(SleepPosture.BACK) }
    var selectedTech by remember { mutableStateOf(MattressTech.GOOD_DREAM_7ZONE) }

    // Breathing pulse for optimal pressure points
    val infiniteTransition = rememberInfiniteTransition(label = "pressure_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("orthopedic_spine_visualizer_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
        border = BorderStroke(1.2.dp, SatinGoldAccent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Clinical Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF163E30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CLINICAL ERGONOMICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SatinGoldAccent
                        )
                        Text(
                            text = "Spine Alignment & Pressure Map",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SatinGoldAccent.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "LIVE SIMULATOR",
                        color = SatinGoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Posture Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepPosture.values().forEach { posture ->
                    val isSelected = selectedPosture == posture
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SatinGoldAccent else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SatinGoldAccent else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .cushionPressEffect(pressedScale = 0.95f)
                            .clickable {
                                haptic.performLuxuryClick()
                                selectedPosture = posture
                            }
                    ) {
                        Text(
                            text = posture.label,
                            color = if (isSelected) ForestGreenDark else Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Technology Selector Tabs
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    MattressTech.values().forEach { tech ->
                        val isSelected = selectedTech == tech
                        val activeColor = if (tech == MattressTech.GOOD_DREAM_7ZONE) ForestGreenPrimary else Color(0xFF334155)
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSelected) activeColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .cushionPressEffect(pressedScale = 0.96f)
                                .clickable {
                                    haptic.performLuxuryClick()
                                    selectedTech = tech
                                }
                        ) {
                            Text(
                                text = tech.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Anatomical Pressure Map Canvas Simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF071912))
                    .border(1.dp, SatinGoldAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawBiomechanicsSimulation(
                        posture = selectedPosture,
                        tech = selectedTech,
                        pulseAlpha = pulseAlpha
                    )
                }

                // Callout Tag
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = if (selectedTech == MattressTech.GOOD_DREAM_7ZONE) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (selectedTech == MattressTech.GOOD_DREAM_7ZONE)
                            "✓ 22 kPa Peak Pressure • 100% S-Curve Alignment"
                        else
                            "⚠️ 78 kPa Concentrated Stress • Spine Sagging",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Posture Insight Description
            Text(
                text = selectedPosture.description,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Biometric Metrics Comparison Matrix
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    BiomechanicMetricRow(
                        label = "Peak Contact Pressure",
                        conventionalVal = "78 kPa (Severe)",
                        goodDreamVal = "22 kPa (72% Drop)",
                        isGood = selectedTech == MattressTech.GOOD_DREAM_7ZONE
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BiomechanicMetricRow(
                        label = "Spinal Column Posture",
                        conventionalVal = "8° Lateral Tilt",
                        goodDreamVal = "0° Neutral Line",
                        isGood = selectedTech == MattressTech.GOOD_DREAM_7ZONE
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BiomechanicMetricRow(
                        label = "Zone Support Response",
                        conventionalVal = "Single Uniform Sag",
                        goodDreamVal = "7-Zone Active Adaptive",
                        isGood = selectedTech == MattressTech.GOOD_DREAM_7ZONE
                    )
                }
            }
        }
    }
}

@Composable
private fun BiomechanicMetricRow(
    label: String,
    conventionalVal: String,
    goodDreamVal: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isGood) goodDreamVal else conventionalVal,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGood) SatinGoldAccent else Color(0xFFFCA5A5)
        )
    }
}

/**
 * Custom 2D/3D Anatomical Biomechanics Canvas Renderer.
 * Draws mattress core substrate, pressure distribution heatmap gradient ellipses,
 * and individual spinal vertebrae nodes indicating real-time ergonomic posture.
 */
private fun DrawScope.drawBiomechanicsSimulation(
    posture: SleepPosture,
    tech: MattressTech,
    pulseAlpha: Float
) {
    val width = size.width
    val height = size.height
    val bedTopY = height * 0.65f
    val isGoodDream = tech == MattressTech.GOOD_DREAM_7ZONE

    // 1. Draw Mattress Substrate
    drawRect(
        color = Color(0xFF0F261D),
        topLeft = Offset(0f, bedTopY),
        size = androidx.compose.ui.geometry.Size(width, height - bedTopY)
    )

    // Draw mattress surface contour with active zone demarcation
    val surfaceColor = if (isGoodDream) SatinGoldAccent.copy(alpha = 0.6f) else Color(0xFF64748B)
    drawLine(
        color = surfaceColor,
        start = Offset(0f, bedTopY),
        end = Offset(width, bedTopY),
        strokeWidth = 3f
    )

    // Draw 7 Zone divisions if Good Dream
    if (isGoodDream) {
        val zoneWidth = width / 7f
        for (i in 1..6) {
            drawLine(
                color = SatinGoldAccent.copy(alpha = 0.25f),
                start = Offset(zoneWidth * i, bedTopY),
                end = Offset(zoneWidth * i, height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }
    }

    // 2. Anatomical Body Landmarks (Head -> Shoulders -> Lumbar -> Hips -> Legs)
    val headX = width * 0.16f
    val shoulderX = width * 0.32f
    val lumbarX = width * 0.48f
    val hipX = width * 0.64f
    val feetX = width * 0.88f

    // Vertebrae baseline elevation
    val headY = bedTopY - 32f
    val shoulderY = if (isGoodDream) bedTopY - 26f else bedTopY - 14f // Good Dream allows balanced sinkage
    val lumbarY = if (isGoodDream) bedTopY - 28f else bedTopY - 38f   // Conventional leaves gap (straining spine)
    val hipY = if (isGoodDream) bedTopY - 24f else bedTopY - 10f      // Conventional excessive sinkage
    val legY = bedTopY - 26f

    // 3. Pressure Heatmap Glows (Radial Gradients)
    if (isGoodDream) {
        // Uniform gentle emerald & gold dispersion
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF10B981).copy(alpha = 0.45f * pulseAlpha), Color.Transparent),
                center = Offset(shoulderX, bedTopY),
                radius = 70f
            ),
            center = Offset(shoulderX, bedTopY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC5A059).copy(alpha = 0.40f * pulseAlpha), Color.Transparent),
                center = Offset(lumbarX, bedTopY),
                radius = 65f
            ),
            center = Offset(lumbarX, bedTopY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF10B981).copy(alpha = 0.50f * pulseAlpha), Color.Transparent),
                center = Offset(hipX, bedTopY),
                radius = 75f
            ),
            center = Offset(hipX, bedTopY)
        )
    } else {
        // Conventional: Severe red/amber localized pressure hotspots at shoulders & hips
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEF4444).copy(alpha = 0.75f), Color(0xFFF97316).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(shoulderX, bedTopY),
                radius = 80f
            ),
            center = Offset(shoulderX, bedTopY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEF4444).copy(alpha = 0.85f), Color(0xFFDC2626).copy(alpha = 0.40f), Color.Transparent),
                center = Offset(hipX, bedTopY),
                radius = 90f
            ),
            center = Offset(hipX, bedTopY)
        )
    }

    // 4. Draw Spinal Vertebrae Connection Curve
    val spinePath = Path().apply {
        moveTo(headX, headY)
        cubicTo(
            (headX + shoulderX) / 2f, (headY + shoulderY) / 2f,
            shoulderX, shoulderY,
            shoulderX, shoulderY
        )
        cubicTo(
            (shoulderX + lumbarX) / 2f, (shoulderY + lumbarY) / 2f,
            lumbarX, lumbarY,
            lumbarX, lumbarY
        )
        cubicTo(
            (lumbarX + hipX) / 2f, (lumbarY + hipY) / 2f,
            hipX, hipY,
            hipX, hipY
        )
        cubicTo(
            (hipX + feetX) / 2f, (hipY + legY) / 2f,
            feetX, legY,
            feetX, legY
        )
    }

    val spineColor = if (isGoodDream) Color(0xFF34D399) else Color(0xFFEF4444)
    drawPath(
        path = spinePath,
        color = spineColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // 5. Draw Individual Anatomical Vertebrae Nodes (7 Spine Segments: C1-C7, T1-T12, L1-L5, S1-S5)
    val nodes = listOf(
        Offset(headX, headY),
        Offset((headX + shoulderX) / 2f, (headY + shoulderY) / 2f),
        Offset(shoulderX, shoulderY),
        Offset((shoulderX + lumbarX) / 2f, (shoulderY + lumbarY) / 2f),
        Offset(lumbarX, lumbarY),
        Offset((lumbarX + hipX) / 2f, (lumbarY + hipY) / 2f),
        Offset(hipX, hipY),
        Offset((hipX + feetX) / 2f, (hipY + legY) / 2f),
        Offset(feetX, legY)
    )

    for (node in nodes) {
        drawCircle(
            color = if (isGoodDream) Color.White else Color(0xFFFCA5A5),
            radius = 4.5f,
            center = node
        )
        drawCircle(
            color = spineColor,
            radius = 2.5f,
            center = node
        )
    }
}
