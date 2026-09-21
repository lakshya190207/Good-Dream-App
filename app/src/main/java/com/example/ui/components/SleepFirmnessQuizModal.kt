package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

data class QuizOption(
    val title: String,
    val description: String,
    val iconName: String,
    val tag: String
)

@Composable
fun SleepFirmnessQuizModal(
    products: List<ProductEntity>,
    onSelectProduct: (ProductEntity) -> Unit,
    onConsultAi: (String) -> Unit,
    onClose: () -> Unit,
    onQuizProfileSaved: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedPosition by remember { mutableStateOf<String?>(null) }
    var selectedHealthNeed by remember { mutableStateOf<String?>(null) }
    var selectedSetup by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStep) {
        if (currentStep == 4) {
            haptic.performLuxurySuccess()
            onQuizProfileSaved?.invoke(selectedHealthNeed ?: selectedPosition ?: "Orthopedic")
        }
    }

    val step1Options = listOf(
        QuizOption("Side Sleeper", "Needs shoulder & hip contouring with pressure relief", "🛌", "side"),
        QuizOption("Back Sleeper", "Requires ergonomic lumbar spine alignment & medium-firm support", "🧘", "back"),
        QuizOption("Stomach Sleeper", "Requires firmer surface to prevent lower spinal sag", "📐", "stomach"),
        QuizOption("Combination Sleeper", "Tosses and turns; needs responsive zero-motion pocket coils", "🔄", "combo")
    )

    val step2Options = listOf(
        QuizOption("Lower Back or Joint Pain", "Orthopedic spine alignment & reinforced posture support", "🩺", "ortho"),
        QuizOption("Motion Isolation for Couples", "Individual pocket springs prevent partner disturbance", "💑", "motion"),
        QuizOption("Cloud-Like Opulence", "Deep plush Euro-top cushioning for luxury rest", "👑", "plush"),
        QuizOption("Breathable & Temperature Neutral", "Enhanced airflow through breathable knitted jacquard", "❄️", "cooling")
    )

    val step3Options = listOf(
        QuizOption("Flagship Integrated Bed (15\")", "Complete engineered bed base + luxury mattress combo", "⭐", "springhaven"),
        QuizOption("Premium Standalone Mattress (8\"-10\")", "Fits your existing bed frame with luxury comfort", "🛏️", "standalone"),
        QuizOption("Bespoke Custom Dimensions", "Made-to-order sizing for antique or non-standard beds", "✂️", "custom")
    )

    // Calculate match
    val recommendedProduct = remember(selectedPosition, selectedHealthNeed, selectedSetup, products) {
        if (selectedSetup == "springhaven") {
            products.firstOrNull { it.isSpringhavenSeries } ?: products.firstOrNull()
        } else if (selectedHealthNeed == "ortho" || selectedPosition == "stomach") {
            products.firstOrNull { it.title.contains("Ortho", ignoreCase = true) } ?: products.firstOrNull()
        } else {
            products.firstOrNull { it.isSpringhavenSeries } ?: products.firstOrNull()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("sleep_firmness_quiz_dialog"),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
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
                                contentDescription = "Close Quiz",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sleep Sanctuary Finder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentStep <= 3) "Step $currentStep of 3: AI Firmness Guidance" else "Your Ideal Match Result",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currentStep <= 3) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$currentStep/3",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Quiz Progress Bar
                LinearProgressIndicator(
                    progress = { currentStep.coerceAtMost(3) / 3f },
                    modifier = Modifier.fillMaxWidth(),
                    color = SatinGoldAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Body content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "quiz_step_transition"
                    ) { step ->
                        when (step) {
                            1 -> {
                                QuizStepView(
                                    questionTitle = "What is your primary sleeping position?",
                                    questionSubtitle = "Your position dictates the pressure points on your neck, spine, and hips.",
                                    options = step1Options,
                                    selectedTag = selectedPosition,
                                    onSelect = {
                                        selectedPosition = it
                                        currentStep = 2
                                    }
                                )
                            }

                            2 -> {
                                QuizStepView(
                                    questionTitle = "What are your specific support & comfort needs?",
                                    questionSubtitle = "Choose the factor most important to your overnight rejuvenation.",
                                    options = step2Options,
                                    selectedTag = selectedHealthNeed,
                                    onSelect = {
                                        selectedHealthNeed = it
                                        currentStep = 3
                                    }
                                )
                            }

                            3 -> {
                                QuizStepView(
                                    questionTitle = "Which setup style best fits your bedroom?",
                                    questionSubtitle = "Select whether you desire an integrated luxury ensemble or a standalone mattress.",
                                    options = step3Options,
                                    selectedTag = selectedSetup,
                                    onSelect = {
                                        selectedSetup = it
                                        currentStep = 4 // Result step
                                    }
                                )
                            }

                            else -> {
                                // Result View
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = SatinGoldAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("✨", fontSize = 18.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "98% Match for Your Sleep Profile",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Text(
                                        text = "Your Engineered Comfort Match",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "Based on your $selectedPosition sleeping style and desire for $selectedHealthNeed, our master craftsmen recommend:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(20.dp))

                                    if (recommendedProduct != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .cushionPressEffect(pressedScale = 0.97f)
                                                .clickable {
                                                    onClose()
                                                    onSelectProduct(recommendedProduct)
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    val thumb = recommendedProduct.getImagesList().firstOrNull() ?: ""
                                                    LuxuryAsyncImage(
                                                        imageUrl = thumb,
                                                        contentDescription = recommendedProduct.title,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                Spacer(Modifier.height(12.dp))

                                                Text(
                                                    text = recommendedProduct.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Text(
                                                    text = recommendedProduct.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "₹${recommendedProduct.price.toInt()}",
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    Button(
                                                        onClick = {
                                                            onClose()
                                                            onSelectProduct(recommendedProduct)
                                                        },
                                                        modifier = Modifier.cushionPressEffect(pressedScale = 0.94f),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary
                                                        ),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text("View Setup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))

                                    // Secondary Actions
                                    OutlinedButton(
                                        onClick = {
                                            val query = "I took the Sleep Sanctuary Finder quiz. My position is $selectedPosition, my primary need is $selectedHealthNeed, and preferred style is $selectedSetup. Why is ${recommendedProduct?.title} ideal for me?"
                                            onClose()
                                            onConsultAi(query)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .cushionPressEffect(pressedScale = 0.95f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Ask AI Concierge to Explain This Match", fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    TextButton(
                                        onClick = { currentStep = 1 },
                                        modifier = Modifier.cushionPressEffect(pressedScale = 0.95f)
                                    ) {
                                        Text("Retake Quiz", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun QuizStepView(
    questionTitle: String,
    questionSubtitle: String,
    options: List<QuizOption>,
    selectedTag: String?,
    onSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = questionTitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = questionSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isSelected = selectedTag == option.tag
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cushionPressEffect(pressedScale = 0.96f)
                        .clickable {
                            haptic.performLuxuryAdjustment()
                            onSelect(option.tag)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = option.iconName, fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
