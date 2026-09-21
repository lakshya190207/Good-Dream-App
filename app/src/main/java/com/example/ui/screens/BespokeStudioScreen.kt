package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BespokeStudioScreen(
    onBack: () -> Unit,
    onCommissionSuccess: () -> Unit,
    onCommissionMattress: (BespokeMattressConfiguration, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var config by remember { mutableStateOf(BespokeMattressConfiguration()) }
    var isExplodedView by remember { mutableStateOf(true) }
    var isCommissioning by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bespoke Mattress Architect",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Atelier Handcrafted Sleep Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = SatinGoldDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("bespoke_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ForestGreenPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "25-Yr Atelier Warranty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bespoke Valuation",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SatinGoldContainer
                                ) {
                                    Text(
                                        text = "TAILORED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SatinGoldDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = currencyFormatter.format(config.calculatePrice()),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "20% Advance: ${currencyFormatter.format(config.calculateCodAdvance())} (COD Option)",
                                fontSize = 11.sp,
                                color = ForestGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (isCommissioning) return@Button
                                isCommissioning = true
                                haptics.performLuxurySuccess()
                                onCommissionMattress(config) {
                                    isCommissioning = false
                                    onCommissionSuccess()
                                }
                            },
                            enabled = !isCommissioning,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("button_commission_bespoke"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isCommissioning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Commission Creation →",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive 3D Cutaway Anatomical Canvas
            item {
                BespokeCutawayCanvasCard(
                    config = config,
                    isExplodedView = isExplodedView,
                    onToggleExplodedView = {
                        haptics.performLuxuryAdjustment()
                        isExplodedView = !isExplodedView
                    }
                )
            }

            // Step 1: Core Architecture
            item {
                SectionHeader("01", "Core Architecture", "Engineered foundation for motion isolation & spinal support")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BespokeCoreType.entries.forEach { core ->
                        val isSelected = config.coreType == core
                        OptionCard(
                            title = core.title,
                            subtitle = core.subtitle,
                            description = core.description,
                            priceText = "Base ${currencyFormatter.format(core.basePrice)}",
                            badgeText = "Firmness ${core.firmnessScore}/10",
                            isSelected = isSelected,
                            onClick = {
                                haptics.performLuxuryClick()
                                config = config.copy(coreType = core)
                            }
                        )
                    }
                }
            }

            // Step 2: Comfort & Climate Layer
            item {
                SectionHeader("02", "Comfort & Climate Layer", "Pressure relief, cushioning, and active airflow control")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BespokeComfortLayer.entries.forEach { comfort ->
                        val isSelected = config.comfortLayer == comfort
                        OptionCard(
                            title = comfort.title,
                            subtitle = comfort.subtitle,
                            description = comfort.description,
                            priceText = if (comfort.additionalPrice > 0) "+${currencyFormatter.format(comfort.additionalPrice)}" else "Included",
                            badgeText = "Airflow: ${comfort.airflowRating}",
                            isSelected = isSelected,
                            onClick = {
                                haptics.performLuxuryClick()
                                config = config.copy(comfortLayer = comfort)
                            }
                        )
                    }
                }
            }

            // Step 3: Quilt & Jacquard Fabric
            item {
                SectionHeader("03", "Fabric & Outer Quilt", "Artisanal tactile cover woven for luxury elegance")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BespokeQuiltCover.entries.forEach { quilt ->
                        val isSelected = config.quiltCover == quilt
                        OptionCard(
                            title = quilt.title,
                            subtitle = quilt.subtitle,
                            description = quilt.description,
                            priceText = if (quilt.additionalPrice > 0) "+${currencyFormatter.format(quilt.additionalPrice)}" else "Included",
                            badgeText = null,
                            isSelected = isSelected,
                            onClick = {
                                haptics.performLuxuryClick()
                                config = config.copy(quiltCover = quilt)
                            }
                        )
                    }
                }
            }

            // Step 4: Dimensions & Height Profile
            item {
                SectionHeader("04", "Dimensions & Height Profile", "Standard master sizes or precision inch-level custom tailoring")
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose Standard Dimensions",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BespokeSizeStandard.entries.take(4).forEach { size ->
                                val isSelected = config.sizeStandard == size
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptics.performLuxuryClick()
                                        config = config.copy(sizeStandard = size)
                                    },
                                    label = { Text(size.label.split(" ")[0], fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForestGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BespokeSizeStandard.entries.drop(4).forEach { size ->
                                val isSelected = config.sizeStandard == size
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptics.performLuxuryClick()
                                        config = config.copy(sizeStandard = size)
                                    },
                                    label = { Text(size.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForestGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // If Custom size is selected, show Width and Length sliders
                        if (config.sizeStandard == BespokeSizeStandard.CUSTOM) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                text = "Precision Custom Sliders",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Width slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Width (Inches)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${config.customWidthInches}\"", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = config.customWidthInches.toFloat(),
                                onValueChange = { config = config.copy(customWidthInches = it.toInt()) },
                                valueRange = 30f..84f,
                                steps = 53
                            )

                            // Length slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Length (Inches)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${config.customLengthInches}\"", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = config.customLengthInches.toFloat(),
                                onValueChange = { config = config.copy(customLengthInches = it.toInt()) },
                                valueRange = 60f..90f,
                                steps = 29
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Thickness profile slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Mattress Profile (Thickness)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Standard is 8\" • Custom build available up to 14\"", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${config.customThicknessInches}\"", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SatinGoldDark)
                        }
                        Slider(
                            value = config.customThicknessInches.toFloat(),
                            onValueChange = { config = config.copy(customThicknessInches = it.toInt()) },
                            valueRange = 6f..14f,
                            steps = 7
                        )
                    }
                }
            }

            // Step 5: Bespoke Monogramming & Add-ons
            item {
                SectionHeader("05", "Bespoke Monogramming & Add-ons", "Embroidered family initials, monogram thread, and bedroom foundations")
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = SatinGoldDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Embroidered Name / Monogram (+₹1,500)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = config.customMonogramText,
                            onValueChange = { if (it.length <= 26) config = config.copy(customMonogramText = it) },
                            placeholder = { Text("e.g. THE STERLING SUITE or C.S.") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestGreenPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        if (config.customMonogramText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Embroidery Metallic Thread Color", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BespokeMonogramColor.entries.forEach { colorOption ->
                                    val isSelected = config.customMonogramColor == colorOption
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                haptics.performLuxuryClick()
                                                config = config.copy(customMonogramColor = colorOption)
                                            }
                                    ) {
                                        Text(
                                            text = colorOption.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Matching Bed Base Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Handcrafted Upholstered Bed Foundation Base",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Solid kiln-dried hardwood base wrapped in matching Forest Velvet (+₹18,000)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.includeFoundationBedBase,
                                onCheckedChange = {
                                    haptics.performLuxuryClick()
                                    config = config.copy(includeFoundationBedBase = it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ForestGreenPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Matching Comfort Pillows Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pair of Tailored Sanctuary Comfort Pillows",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Custom-filled pillows matching chosen comfort layer (Complimentary Included)",
                                    fontSize = 11.sp,
                                    color = ForestGreenPrimary
                                )
                            }
                            Switch(
                                checked = config.includeMatchingPillows,
                                onCheckedChange = {
                                    haptics.performLuxuryClick()
                                    config = config.copy(includeMatchingPillows = it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ForestGreenPrimary)
                            )
                        }
                    }
                }
            }

            // Spacing for sticky bottom bar
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BespokeCutawayCanvasCard(
    config: BespokeMattressConfiguration,
    isExplodedView: Boolean,
    onToggleExplodedView: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = SatinGoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive Layer Architecture",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onToggleExplodedView,
                    colors = ButtonDefaults.textButtonColors(contentColor = SatinGoldAccent)
                ) {
                    Icon(
                        imageVector = if (isExplodedView) Icons.Default.ViewAgenda else Icons.Default.Splitscreen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExplodedView) "Collapse View" else "Explode View",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Layer Stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isExplodedView) 10.dp else 2.dp)
            ) {
                // Layer 1: Quilt & Cover
                LayerBar(
                    layerNumber = "01",
                    layerName = "Top Quilt: ${config.quiltCover.title}",
                    detailText = "Hand-Tufted • Hypoallergenic • Temperature Regulating",
                    accentColor = SatinGoldAccent,
                    isExploded = isExplodedView
                )

                // Monogram preview badge if entered
                if (config.customMonogramText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0B2019))
                            .border(1.dp, SatinGoldAccent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✦ ${config.customMonogramText.trim().uppercase()} ✦",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = SatinGoldAccent,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                // Layer 2: Comfort Layer
                LayerBar(
                    layerNumber = "02",
                    layerName = "Comfort Topper: ${config.comfortLayer.title}",
                    detailText = "${config.comfortLayer.subtitle} • Airflow: ${config.comfortLayer.airflowRating}",
                    accentColor = Color(0xFF64B5F6),
                    isExploded = isExplodedView
                )

                // Layer 3: Core Architecture
                LayerBar(
                    layerNumber = "03",
                    layerName = "Core Foundation: ${config.coreType.title}",
                    detailText = "${config.coreType.subtitle} • Firmness Score: ${config.coreType.firmnessScore}/10",
                    accentColor = Color(0xFF81C784),
                    isExploded = isExplodedView
                )

                // Layer 4: Edge Wall Support
                LayerBar(
                    layerNumber = "04",
                    layerName = "Edge Support: Reinforced Velvet Perimeter",
                    detailText = "Anti-Roll High Density Border • Hand-Stitched Cord Piping",
                    accentColor = ForestGreenContainer,
                    isExploded = isExplodedView
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dimension and specification pill bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF081C15))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Specification: ${config.getFormattedDimensions()}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Artisan Built-to-Order",
                    fontSize = 10.sp,
                    color = SatinGoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LayerBar(
    layerNumber: String,
    layerName: String,
    detailText: String,
    accentColor: Color,
    isExploded: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF163E30),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (isExploded) 10.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = layerNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layerName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isExploded) {
                    Text(
                        text = detailText,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(stepNumber: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    subtitle: String,
    description: String,
    priceText: String,
    badgeText: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ForestGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreenPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = priceText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ForestGreenPrimary else SatinGoldDark
                    )
                }

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                if (badgeText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SatinGoldContainer
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SatinGoldDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
