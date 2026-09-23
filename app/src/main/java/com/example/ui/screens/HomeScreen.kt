package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.config.AppConfig
import com.example.ui.components.ComfortSleepSanctuaryCard
import com.example.ui.components.LuxuryAsyncImage
import com.example.ui.components.OfferBannerSkeleton
import com.example.ui.components.breatheEffect
import com.example.ui.components.cushionPressEffect
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveModal
import com.example.ui.viewmodel.MainTab

data class HomeActionItem(
    val indexNumber: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTagId: String,
    val isHighlighted: Boolean = false,
    val badgeLabel: String? = null,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToTab: (MainTab) -> Unit,
    onOpenModal: (ActiveModal) -> Unit,
    appConfig: AppConfig = AppConfig(),
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Curated high-value bespoke services without duplicating primary navigation tabs
    val homeActions = remember(onNavigateToTab, onOpenModal) {
        listOf(
            HomeActionItem(
                indexNumber = "01",
                title = "Your Needs",
                subtitle = "Bespoke Custom Inquiry",
                icon = Icons.AutoMirrored.Filled.Assignment,
                testTagId = "home_card_01_your_needs",
                badgeLabel = "CUSTOM",
                onClick = { onOpenModal(ActiveModal.YOUR_NEEDS) }
            ),
            HomeActionItem(
                indexNumber = "02",
                title = "Sponsor Rewards",
                subtitle = "Earn ₹2,500 Store Credit",
                icon = Icons.Default.CardGiftcard,
                testTagId = "home_card_02_sponsor_rewards",
                badgeLabel = "₹2,500",
                isHighlighted = true,
                onClick = { onOpenModal(ActiveModal.SPONSOR_REWARDS) }
            ),
            HomeActionItem(
                indexNumber = "03",
                title = "10-Yr Warranty",
                subtitle = "Digital Vault & Claims",
                icon = Icons.Default.VerifiedUser,
                testTagId = "home_card_03_service_warranties",
                badgeLabel = "10-Yr",
                onClick = { onOpenModal(ActiveModal.SERVICE_WARRANTIES) }
            ),
            HomeActionItem(
                indexNumber = "04",
                title = "Repairs & Care",
                subtitle = "Technician Dispatch",
                icon = Icons.Default.Build,
                testTagId = "home_card_04_repairs",
                onClick = { onOpenModal(ActiveModal.REPAIRS) }
            ),
            HomeActionItem(
                indexNumber = "05",
                title = "Feedbacks",
                subtitle = "Verified Client Reviews",
                icon = Icons.Default.RateReview,
                testTagId = "home_card_05_feedbacks",
                onClick = { onOpenModal(ActiveModal.FEEDBACKS) }
            ),
            HomeActionItem(
                indexNumber = "06",
                title = "Concierge Support",
                subtitle = "24h Priority Escalation",
                icon = Icons.Default.SupportAgent,
                testTagId = "home_card_06_complaints",
                badgeLabel = "24h SLA",
                onClick = { onOpenModal(ActiveModal.COMPLAINTS) }
            )
        )
    }

    // Strictly 2-column layout conforming to layout rigidity
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High-engagement Hero Banner (Promoting SpringHaven Series & Trust)
        item(span = { GridItemSpan(2) }) {
            if (isLoading) {
                OfferBannerSkeleton()
            } else {
                HomeHeroShowcaseCard(
                    onExploreSpringHaven = { onNavigateToTab(MainTab.NEW_LAUNCHES) },
                    onInquireCustom = { onOpenModal(ActiveModal.YOUR_NEEDS) }
                )
            }
        }

        // Trust Pillars Strip (10-Yr Warranty | White-Glove Delivery | Handcrafted)
        item(span = { GridItemSpan(2) }) {
            HomeTrustPillarsStrip()
        }

        // Unified Precision Sleep & Bespoke Suite (3D Atelier, Sleep Tools & AI Concierge)
        item(span = { GridItemSpan(2) }) {
            HomeSanctuarySuiteCard(
                onOpenBespokeStudio = { onOpenModal(ActiveModal.BESPOKE_STUDIO) },
                onOpenSleepQuiz = { onOpenModal(ActiveModal.SLEEP_QUIZ) },
                onOpenComparison = { onOpenModal(ActiveModal.COMPARE_PRODUCTS) },
                onOpenOrderTracking = { onOpenModal(ActiveModal.ORDER_TRACKING) },
                onOpenAiConcierge = { onOpenModal(ActiveModal.AI_CHAT_BOT) }
            )
        }

        // Section Header Label
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "BESPOKE SERVICES & CARE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SatinGoldDark
                    )
                    Text(
                        text = "Client Sanctuary Care",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 12 Distinct Rounded Rectangular Action Cards in 2-Column Grid
        items(homeActions, key = { it.testTagId }, contentType = { "home_action_card" }) { item ->
            HomeFeatureGridCard(item = item)
        }

        // Sanctuary Boutique & Flagship Experience Centre Footer Card
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_sanctuary_address_footer"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SatinGoldAccent.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SatinGoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SatinGoldDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SANCTUARY FLAGSHIP BOUTIQUE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SatinGoldDark
                            )
                            Text(
                                text = "Experience Good Dream Comfort",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "GOOD DREAMS HOME DECOR PRIVATE LIMITED\nD-4, VIJAY VIHAR COLONY, NAYA KHEDA, Amba Bari, Jaipur, Jaipur- 302039, Rajasthan\nMarketed by: H P PRODUCTS, Address: P.NO. 4, BADHARNA, BAJRANG VIHAR 5, Jaipur, Rajasthan, 302013",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mon - Sat: 10:00 AM - 8:00 PM • Sunday by Appointment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Experience Studio Available",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { onOpenModal(ActiveModal.YOUR_NEEDS) }
                        ) {
                            Text(
                                text = "Book Visit →",
                                color = SatinGoldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeroShowcaseCard(
    onExploreSpringHaven: () -> Unit,
    onInquireCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .cushionPressEffect(pressedScale = 0.985f)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onExploreSpringHaven)
            .testTag("home_hero_showcase_card"),
        colors = CardDefaults.cardColors(containerColor = ForestGreenDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            // Serene Luxury Master Bedroom Visual Backdrop - Zero-latency instant local rendering
            Image(
                painter = painterResource(id = R.drawable.hero_master_bedroom),
                contentDescription = "Master Bedroom Haven",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Luxury Forest Green & Satin Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                ForestGreenDark.copy(alpha = 0.82f),
                                ForestGreenDark.copy(alpha = 0.97f)
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SatinGoldAccent,
                        modifier = Modifier.breatheEffect(minScale = 0.97f, maxScale = 1.03f)
                    ) {
                        Text(
                            text = "FLAGSHIP COLLECTION",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ForestGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = SatinGoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "White-Glove Setup",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "SpringHaven™ Series",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )

                    Text(
                        text = "15-Inch Ultra-Luxury Orthopedic Bedding with Swedish Multi-Zone Pocket Coils & Natural Cool-Gel Memory Foam.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExploreSpringHaven,
                            colors = ButtonDefaults.buttonColors(containerColor = SatinGoldAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .cushionPressEffect(pressedScale = 0.95f)
                        ) {
                            Text(
                                text = "Explore Flagship",
                                color = ForestGreenDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onInquireCustom,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.cushionPressEffect(pressedScale = 0.95f)
                        ) {
                            Text(
                                text = "Custom Sizing",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTrustPillarsStrip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_trust_pillars_strip"),
        shape = RoundedCornerShape(14.dp),
        color = SatinGoldContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TrustItem(icon = Icons.Default.Security, title = "10-Yr Guarantee")
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = SatinGoldDark.copy(alpha = 0.25f)
            )
            TrustItem(icon = Icons.Default.LocalShipping, title = "Free Delivery")
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = SatinGoldDark.copy(alpha = 0.25f)
            )
            TrustItem(icon = Icons.Default.WorkspacePremium, title = "Handcrafted Pure")
        }
    }
}

@Composable
private fun TrustItem(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HomeFeatureGridCard(
    item: HomeActionItem,
    modifier: Modifier = Modifier
) {
    val borderColor = if (item.isHighlighted) MaterialTheme.colorScheme.secondary else Color.Transparent
    val borderWidth = if (item.isHighlighted) 1.5.dp else 0.dp

    Card(
        modifier = modifier
            .testTag(item.testTagId)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 128.dp)
            .cushionPressEffect(pressedScale = 0.965f)
            .shadow(elevation = if (item.isHighlighted) 4.dp else 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon and Index Badge + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (item.isHighlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (item.isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.badgeLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (item.isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = item.badgeLabel,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isHighlighted) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = if (item.isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Section: Title and Subtitle
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = if (item.isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeSanctuarySuiteCard(
    onOpenBespokeStudio: () -> Unit,
    onOpenSleepQuiz: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenOrderTracking: () -> Unit,
    onOpenAiConcierge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_sleep_tools_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PRECISION SLEEP & BESPOKE SUITE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SatinGoldDark
                    )
                    Text(
                        text = "Sanctuary Studio & Advisory",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bespoke 3D Mattress Architect Strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .cushionPressEffect()
                    .clickable(onClick = onOpenBespokeStudio)
                    .testTag("home_bespoke_atelier_banner"),
                shape = RoundedCornerShape(14.dp),
                color = ForestGreenDark,
                border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                                text = "Bespoke Mattress Architect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SatinGoldAccent
                            ) {
                                Text(
                                    text = "3D ATELIER",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ForestGreenDark,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Custom pocket springs, latex, inch sizes & monogram",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Clean Quick Tool Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sleep Firmness Quiz Tile
                SleepToolTile(
                    title = "Sleep Quiz",
                    subtitle = "Firmness",
                    badge = "3-STEP",
                    icon = Icons.Default.Bedtime,
                    badgeColor = SatinGoldAccent,
                    textColor = ForestGreenDark,
                    onClick = onOpenSleepQuiz,
                    modifier = Modifier.weight(1f)
                )

                // Side-by-Side Mattress Comparison Tile
                SleepToolTile(
                    title = "Compare",
                    subtitle = "Matrix",
                    badge = "SPECS",
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    badgeColor = ForestGreenContainer,
                    textColor = ForestGreenPrimary,
                    onClick = onOpenComparison,
                    modifier = Modifier.weight(1f)
                )

                // Order Tracking Tile
                SleepToolTile(
                    title = "Tracking",
                    subtitle = "Status",
                    badge = "LIVE",
                    icon = Icons.Default.LocalShipping,
                    badgeColor = SatinGoldContainer,
                    textColor = SatinGoldDark,
                    onClick = onOpenOrderTracking,
                    modifier = Modifier.weight(1f)
                )

                // DreamCare AI Concierge Tile
                SleepToolTile(
                    title = "Concierge",
                    subtitle = "AI Assist",
                    badge = "SMART",
                    icon = Icons.Default.SmartToy,
                    badgeColor = ForestGreenPrimary,
                    textColor = Color.White,
                    onClick = onOpenAiConcierge,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_ai_concierge_banner")
                )
            }
        }
    }
}

@Composable
private fun SleepToolTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    badgeColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .cushionPressEffect(pressedScale = 0.96f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor
            ) {
                Text(
                    text = badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}



