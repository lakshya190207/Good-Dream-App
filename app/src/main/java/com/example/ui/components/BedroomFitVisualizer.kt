package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class BedroomRoomPreset(val label: String, val widthFeet: Float, val lengthFeet: Float, val description: String) {
    MASTER_SUITE("Master Suite", 14f, 16f, "14' × 16' (Generous luxury master bedroom)"),
    GUEST_ROOM("Guest Room", 11f, 13f, "11' × 13' (Standard secondary/guest bedroom)"),
    STUDIO("Compact Studio", 10f, 10f, "10' × 10' (Urban apartment or studio space)"),
    CUSTOM("Custom Sizing", 12f, 14f, "Interactive parametric room dimension sliders")
}

enum class BedSizePreset(val label: String, val widthInches: Int, val lengthInches: Int) {
    KING("King 72\"×78\"", 72, 78),
    QUEEN("Queen 60\"×78\"", 60, 78),
    SINGLE("Single 36\"×75\"", 36, 75)
}

/**
 * Interactive 2D Architectural Bedroom Scale & Dimension Fit Visualizer.
 * Solves customer hesitation by rendering a real-time architectural blueprint
 * with room perimeter walls, nightstands, door-swing clearance, and color-coded walkways.
 */
@Composable
fun BedroomFitVisualizer(
    initialBedSize: BedSizePreset = BedSizePreset.KING,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedPreset by remember { mutableStateOf(BedroomRoomPreset.MASTER_SUITE) }
    var selectedBedSize by remember { mutableStateOf(initialBedSize) }
    var customRoomWidthFeet by remember { mutableFloatStateOf(12f) }
    var customRoomLengthFeet by remember { mutableFloatStateOf(14f) }

    val effectiveWidthFeet = if (selectedPreset == BedroomRoomPreset.CUSTOM) customRoomWidthFeet else selectedPreset.widthFeet
    val effectiveLengthFeet = if (selectedPreset == BedroomRoomPreset.CUSTOM) customRoomLengthFeet else selectedPreset.lengthFeet

    val roomWidthInches = effectiveWidthFeet * 12f
    val roomLengthInches = effectiveLengthFeet * 12f

    // Clearance Calculations (Bed against top wall with 6" headboard offset and 20" flanking nightstands)
    val sideWallClearanceInches = ((roomWidthInches - selectedBedSize.widthInches) / 2f).coerceAtLeast(0f)
    val walkwayBesideNightstandInches = (sideWallClearanceInches - 20f).coerceAtLeast(0f)
    val footClearanceInches = (roomLengthInches - (selectedBedSize.lengthInches + 6f)).coerceAtLeast(0f)

    val isSpacious = sideWallClearanceInches >= 32f && footClearanceInches >= 36f
    val isModerate = sideWallClearanceInches >= 24f && footClearanceInches >= 28f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bedroom_fit_visualizer_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
        border = BorderStroke(1.2.dp, SatinGoldAccent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ROOM ARCHITECT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SatinGoldAccent
                        )
                        Text(
                            text = "Will It Fit Your Bedroom?",
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
                        text = "2D BLUEPRINT",
                        color = SatinGoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Room Type Preset Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BedroomRoomPreset.values().forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SatinGoldAccent else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (isSelected) SatinGoldAccent else Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .weight(1f)
                            .cushionPressEffect(pressedScale = 0.95f)
                            .clickable {
                                haptic.performLuxuryClick()
                                selectedPreset = preset
                            }
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) ForestGreenDark else Color.White.copy(alpha = 0.85f),
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // Custom Sliders if Custom Preset Selected
            AnimatedVisibility(visible = selectedPreset == BedroomRoomPreset.CUSTOM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Room Width: ${customRoomWidthFeet.toInt()} ft (${(customRoomWidthFeet * 12).toInt()}\")",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "Room Length: ${customRoomLengthFeet.toInt()} ft (${(customRoomLengthFeet * 12).toInt()}\")",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = customRoomWidthFeet,
                            onValueChange = { customRoomWidthFeet = it },
                            valueRange = 9f..20f,
                            steps = 10,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = SatinGoldAccent,
                                activeTrackColor = SatinGoldAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Slider(
                            value = customRoomLengthFeet,
                            onValueChange = { customRoomLengthFeet = it },
                            valueRange = 9f..24f,
                            steps = 14,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = SatinGoldAccent,
                                activeTrackColor = SatinGoldAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mattress Size Selector Tabs
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    BedSizePreset.values().forEach { bed ->
                        val isSelected = selectedBedSize == bed
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ForestGreenPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .cushionPressEffect(pressedScale = 0.96f)
                                .clickable {
                                    haptic.performLuxuryClick()
                                    selectedBedSize = bed
                                }
                        ) {
                            Text(
                                text = bed.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Architectural Blueprint Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF071912))
                    .border(1.dp, SatinGoldAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArchitecturalBlueprint(
                        roomWidthInches = roomWidthInches,
                        roomLengthInches = roomLengthInches,
                        bedWidthInches = selectedBedSize.widthInches.toFloat(),
                        bedLengthInches = selectedBedSize.lengthInches.toFloat(),
                        isSpacious = isSpacious,
                        isModerate = isModerate
                    )
                }

                // Dynamic Verdict Tag
                val verdictBadgeColor = when {
                    isSpacious -> Color(0xFF064E3B)
                    isModerate -> Color(0xFF78350F)
                    else -> Color(0xFF7F1D1D)
                }
                val verdictText = when {
                    isSpacious -> "✓ Ideal Fit • ${sideWallClearanceInches.toInt()}\" Side & ${footClearanceInches.toInt()}\" Foot Clearance"
                    isModerate -> "✓ Good Fit • ${sideWallClearanceInches.toInt()}\" Side & ${footClearanceInches.toInt()}\" Foot Clearance"
                    else -> "⚠️ Tight Fit • Consider Queen (60\"×78\") for better walkways"
                }

                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = verdictBadgeColor,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = verdictText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clearance Dimensions Metrics Breakdown
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Side Walkway",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "${sideWallClearanceInches.toInt()} Inches",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sideWallClearanceInches >= 30f) SatinGoldAccent else Color(0xFFFCA5A5)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Foot Walkway",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "${footClearanceInches.toInt()} Inches",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (footClearanceInches >= 36f) SatinGoldAccent else Color(0xFFFCA5A5)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nightstand Gap",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "${walkwayBesideNightstandInches.toInt()} Inches",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (walkwayBesideNightstandInches >= 12f) SatinGoldAccent else Color(0xFFFCA5A5)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom 2D Architectural Scale Canvas Renderer.
 * Draws room boundary walls, architectural blueprint grid, door swing arc,
 * bed frame with headboard, nightstands, and dimension lines.
 */
private fun DrawScope.drawArchitecturalBlueprint(
    roomWidthInches: Float,
    roomLengthInches: Float,
    bedWidthInches: Float,
    bedLengthInches: Float,
    isSpacious: Boolean,
    isModerate: Boolean
) {
    val canvasW = size.width
    val canvasH = size.height

    // Calculate scaling factor to fit within canvas with padding
    val padding = 24f
    val availW = canvasW - (padding * 2f)
    val availH = canvasH - (padding * 2f)

    val scaleX = availW / roomWidthInches
    val scaleY = availH / roomLengthInches
    val scale = minOf(scaleX, scaleY)

    val drawRoomW = roomWidthInches * scale
    val drawRoomL = roomLengthInches * scale

    val roomLeft = (canvasW - drawRoomW) / 2f
    val roomTop = (canvasH - drawRoomL) / 2f

    // 1. Draw Architectural Grid
    val gridSize = 16f
    var x = roomLeft
    while (x <= roomLeft + drawRoomW) {
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(x, roomTop),
            end = Offset(x, roomTop + drawRoomL),
            strokeWidth = 1f
        )
        x += gridSize
    }
    var y = roomTop
    while (y <= roomTop + drawRoomL) {
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(roomLeft, y),
            end = Offset(roomLeft + drawRoomW, y),
            strokeWidth = 1f
        )
        y += gridSize
    }

    // 2. Draw Room Perimeter Walls
    drawRect(
        color = Color(0xFFC5A059),
        topLeft = Offset(roomLeft, roomTop),
        size = Size(drawRoomW, drawRoomL),
        style = Stroke(width = 3f)
    )

    // 3. Draw Door Swing Clearance Arc in bottom-left corner
    val doorWidthInches = 36f * scale
    val doorArcRect = Size(doorWidthInches * 2f, doorWidthInches * 2f)
    drawArc(
        color = Color.White.copy(alpha = 0.25f),
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(roomLeft - doorWidthInches, roomTop + drawRoomL - doorWidthInches),
        size = doorArcRect,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
    )

    // 4. Draw Centered Bed Frame against top wall (with 6" headboard offset)
    val drawBedW = bedWidthInches * scale
    val drawBedL = bedLengthInches * scale
    val bedLeft = roomLeft + (drawRoomW - drawBedW) / 2f
    val bedTop = roomTop + (6f * scale)

    // Headboard
    val headboardH = 6f * scale
    drawRoundRect(
        color = Color(0xFF1B4D3E),
        topLeft = Offset(bedLeft, roomTop),
        size = Size(drawBedW, headboardH),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Bed Mattress Frame
    drawRoundRect(
        color = Color(0xFF133E2F),
        topLeft = Offset(bedLeft, bedTop),
        size = Size(drawBedW, drawBedL),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFFC5A059).copy(alpha = 0.8f),
        topLeft = Offset(bedLeft, bedTop),
        size = Size(drawBedW, drawBedL),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5f)
    )

    // Pillows
    val pillowW = (drawBedW - 8f) / 2f
    val pillowH = 18f * scale
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(bedLeft + 2f, bedTop + 4f),
        size = Size(pillowW, pillowH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(bedLeft + pillowW + 6f, bedTop + 4f),
        size = Size(pillowW, pillowH),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // 5. Draw Dual Nightstands (20" x 20" scale)
    val nsSize = 20f * scale
    val leftNsX = bedLeft - nsSize - 4f
    val rightNsX = bedLeft + drawBedW + 4f
    if (leftNsX >= roomLeft) {
        drawRoundRect(
            color = Color(0xFF163E30),
            topLeft = Offset(leftNsX, roomTop),
            size = Size(nsSize, nsSize),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFFC5A059).copy(alpha = 0.5f),
            topLeft = Offset(leftNsX, roomTop),
            size = Size(nsSize, nsSize),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 1f)
        )
    }
    if (rightNsX + nsSize <= roomLeft + drawRoomW) {
        drawRoundRect(
            color = Color(0xFF163E30),
            topLeft = Offset(rightNsX, roomTop),
            size = Size(nsSize, nsSize),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFFC5A059).copy(alpha = 0.5f),
            topLeft = Offset(rightNsX, roomTop),
            size = Size(nsSize, nsSize),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 1f)
        )
    }

    // 6. Draw Walkway Clearance Markers (Green/Amber/Red guidelines)
    val markerColor = when {
        isSpacious -> Color(0xFF10B981)
        isModerate -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    // Foot of Bed Walkway Indicator
    val footStartY = bedTop + drawBedL
    val footEndY = roomTop + drawRoomL
    val footCenterX = roomLeft + (drawRoomW / 2f)
    drawLine(
        color = markerColor.copy(alpha = 0.75f),
        start = Offset(footCenterX, footStartY + 4f),
        end = Offset(footCenterX, footEndY - 4f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )

    // Left & Right Walkway Indicators
    val sideCenterY = bedTop + (drawBedL / 2f)
    drawLine(
        color = markerColor.copy(alpha = 0.6f),
        start = Offset(roomLeft + 4f, sideCenterY),
        end = Offset(bedLeft - 4f, sideCenterY),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = markerColor.copy(alpha = 0.6f),
        start = Offset(bedLeft + drawBedW + 4f, sideCenterY),
        end = Offset(roomLeft + drawRoomW - 4f, sideCenterY),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
}
