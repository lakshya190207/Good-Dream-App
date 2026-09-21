package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

/**
 * Official Legal, Privacy, Cookie & Warranty Policies Modal.
 * Incorporates:
 * 1. SpringHaven 25-Year Guarantee & Terms of Service (Pages 1, 2, 3 of official warranty certificate)
 *    - 25 Years Warranty across Sofa, Bed, Mattress, Gadda, Customized Furniture
 *    - Timely Service Schedule (5, 10, 15, 20, 25 Years) with exact service fees
 *    - Transportation & Cost Adjustment rules (₹500 min, ₹1,000 company contribution)
 *    - Inflation Index / Cost Adjustment formula: Adjusted Fee = Base Fee * (Current Index / Base Year Index)
 *    - What's Covered vs What's Not Covered (Exclusions)
 *    - Warranty Transfer rules & Claim Process requiring original bill and service card
 * 2. Privacy Policy & Data Safety (Google Play compliant & DPDP Act)
 * 3. Cookie, Local Identifiers & Device Storage Policy (Google Play compliant)
 * 4. 100-Night Trial, Returns & Refund Policy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalPoliciesModal(
    initialTab: Int = 0, // 0: 25-Yr Terms, 1: Privacy Policy, 2: Cookie Policy, 3: Refund Policy
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("modal_legal_policies"),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Luxury Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ForestGreenPrimary, ForestGreenDark)
                                            )
                                        )
                                        .border(1.dp, SatinGoldAccent, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = SatinGoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "SpringHaven Legal & Policies",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SatinGoldAccent.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "OFFICIAL",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SatinGoldAccent,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "25-Yr Terms • Privacy • Cookies • Refund Policy",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4-Tab Scrollable / Compact Navigation
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = ForestGreenPrimary,
                            edgePadding = 8.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = SatinGoldAccent,
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        text = "25-Yr Terms",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        text = "Privacy Policy",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Text(
                                        text = "Cookie Policy",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = {
                                    Text(
                                        text = "Refund Policy",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }

                // Content Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> TermsAndWarrantyContent(onContactSupport = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=+918041239999&text=Hello%20SpringHaven!%20I%20have%20an%20inquiry%20regarding%20the%2025-Year%20Warranty%20and%20Service%20Schedule."))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918041239999"))
                                context.startActivity(dialIntent)
                            }
                        })
                        1 -> PrivacyPolicyContent()
                        2 -> CookiePolicyContent()
                        3 -> RefundPolicyContent()
                    }
                }

                // Bottom Footer with Quick Action
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Questions regarding coverage?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Concierge Desk: 1800-SPRINGHAVEN",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=+918041239999&text=Hello%20SpringHaven%20Support!%20I%20would%20like%20assistance%20with%20my%20warranty%20and%20service%20card."))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Support Desk: +91 80 4123 9999", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Contact Desk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

// -------------------------------------------------------------------------------------
// TAB 0: 25-YEAR SPRINGHAVEN GUARANTEE & TERMS OF SERVICE (FROM UPLOADED IMAGE)
// -------------------------------------------------------------------------------------
@Composable
private fun TermsAndWarrantyContent(
    onContactSupport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Certificate Card (Page 1 Front)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, SatinGoldAccent.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ForestGreenPrimary,
                            border = BorderStroke(1.5.dp, SatinGoldAccent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = SatinGoldAccent,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SpringHaven",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PREMIUM COMFORT, LIFETIME TRUST",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = SatinGoldDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categories Covered
                    Text(
                        text = "SOFA  |  BED  |  SOFA CUM BED  |  MATTRESS  |  GADDA\nIMPACT / COMPACT MATTRESS  |  CUSTOMIZED FURNITURE",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 25 Years Seal Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SatinGoldAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "25 YEARS GUARANTEE",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "★ 25 YEARS LIMITED WARRANTY ★",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatinGoldDark,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "25 Years Limited Warranty — Subject to Terms & Conditions",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Value Pillars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ValuePillarItem(icon = Icons.Default.Shield, title = "LONG LASTING\nDURABILITY", modifier = Modifier.weight(1f))
                        ValuePillarItem(icon = Icons.Default.Build, title = "TIMELY SERVICE\nSUPPORT", modifier = Modifier.weight(1f))
                        ValuePillarItem(icon = Icons.Default.Verified, title = "QUALITY\nASSURANCE", modifier = Modifier.weight(1f))
                        ValuePillarItem(icon = Icons.Default.Groups, title = "TRUST &\nCOMMITMENT", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Our Promise — Your Comfort. Your Trust — Our Responsibility.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Section: Timely Service Schedule & Charges (Page 2)
        item {
            PolicySectionHeader(
                icon = Icons.Default.AccessTime,
                title = "Timely Service Schedule & Charges",
                subtitle = "Page 2: Time-Bound Service Program & Mandatory Fees"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "To guarantee structural resilience and hygienic restorative sleep over the full 25-year lifetime, periodic scheduled maintenance is conducted by certified master technicians according to the time intervals below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Service Schedule Table
                    ServiceScheduleRow(step = "1", title = "1st Service", milestone = "5 Years", fee = "₹ 4,000 + Transport", isHighlight = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ServiceScheduleRow(step = "2", title = "2nd Service", milestone = "10 Years", fee = "₹ 5,000 + Transport", isHighlight = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ServiceScheduleRow(step = "3", title = "3rd Service", milestone = "15 Years", fee = "₹ 7,000 + Transport", isHighlight = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ServiceScheduleRow(step = "4", title = "4th Service", milestone = "20 Years", fee = "₹ 9,000 + Transport", isHighlight = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ServiceScheduleRow(step = "5", title = "Final Service", milestone = "25 Years", fee = "₹ 12,000 + Transport", isHighlight = true)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "* Additional charge applies for parts replacement, internal quilting re-foaming, or high-wear material renewal if deemed necessary during inspection.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section: Transportation / Cost Adjustment Rules
        item {
            PolicySectionHeader(
                icon = Icons.Default.LocalShipping,
                title = "Transportation & Cost Adjustment Rules",
                subtitle = "Official logistics coverage & customer responsibility"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransportRuleBullet("Minimum Transportation Charge: ₹ 500 (Round trip).")
                    TransportRuleBullet("SpringHaven Contribution: We bear up to ₹ 1,000 (Round trip) of all transportation costs.")
                    TransportRuleBullet("Customer Responsibility: Any transportation expenses above ₹ 1,000 will be paid by the customer.")
                    TransportRuleBullet("Distance / Area Adjustment: Outstation or remote zone locations beyond standard city jurisdiction will incur actual distance-based tariffs.")
                    TransportRuleBullet("Special Circumstances: Multi-floor manual stair carrying without elevator, crane hoisting, or specialized labor will incur actual extra costs.")
                }
            }
        }

        // Section: Inflation Index / Cost Adjustment Formula
        item {
            PolicySectionHeader(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Inflation Index / Cost Adjustment Formula",
                subtitle = "Dynamic fee indexing over the 25-year tenure"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "To account for long-term economic shifts across the 25-year lifespan, service charges undergo inflation adjustments referenced against official CPI (Consumer Price Index):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Adjusted Fee = Base Fee × (Current Index / Base Year Index)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Based on official CPI (IW - Industrial Workers) / CPI (AL - Agricultural Labourers) benchmarks.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Section: What's Covered vs What's Not Covered (Page 3)
        item {
            PolicySectionHeader(
                icon = Icons.AutoMirrored.Filled.Rule,
                title = "Warranty Coverage & Exclusions",
                subtitle = "Page 3: Terms, conditions and non-covered damages"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // What's Covered Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("What's Covered", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CoveragePoint(true, "Core structural breakage & collapse")
                        CoveragePoint(true, "Pocket/Bonnell spring failure")
                        CoveragePoint(true, "Sagging deeper than 1.5 inches")
                        CoveragePoint(true, "Internal frame & joint defects")
                        CoveragePoint(true, "High resilience core disintegration")
                    }
                }

                // What's Not Covered Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("What's Not Covered", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusError)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CoveragePoint(false, "Normal wear & tear / fabric fraying")
                        CoveragePoint(false, "Liquid spills, stains & burns")
                        CoveragePoint(false, "Improper bed slatted foundation")
                        CoveragePoint(false, "Termites, pests & rodent damage")
                        CoveragePoint(false, "Unauthorized 3rd-party repairs")
                    }
                }
            }
        }

        // Section: Warranty Transfer & Claim Process
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Warranty Transfer Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The 25-Year Limited Warranty is valid strictly for the original registered purchaser and is non-transferable to any secondary buyer upon resale, re-assignment, or gifting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mandatory Claim Process & Requirements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SatinGoldAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = SatinGoldDark, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Original Tax Invoice / Retail Bill",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "2. Official SpringHaven Physical or In-App Digital Service Card",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Both items are mandatory to initiate an active claim or schedule service visits.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 1: PRIVACY POLICY & DATA SAFETY (GOOGLE PLAY STORE COMPLIANT)
// -------------------------------------------------------------------------------------
@Composable
private fun PrivacyPolicyContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Good Dream & SpringHaven Privacy Commitment", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Last Updated: September 2026 • Google Play Store Data Safety & Indian DPDP Act Compliant", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        item {
            PolicyDisclosureCard(
                title = "1. Mandatory Authentication & Tenant Data Isolation",
                bullets = listOf(
                    "Mandatory Account Authentication: Prior to filling out or submitting bespoke inquiries, repair tickets, customer feedback, official complaints, or warranty activations, users must sign in via Email OTP or salted Passcode. This guarantees that all submitted data is verifiably linked to an authenticated identity, preventing unauthorized or spoofed records.",
                    "Strict Customer Data Isolation: Every customer's records (inquiries, repair requests, feedback submissions, orders, and warranty certificates) are strictly partitioned. Users can only access, track, and view their own personal records. Cross-account data queries are cryptographically blocked at both the database and repository layers.",
                    "Executive Privilege Safeguards: Only authenticated executive administrators (using dual-factor verified admin credentials) can access aggregated operational records within the Executive Studio solely for order fulfillment and white-glove concierge management."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "2. Comprehensive Information We Collect & Why",
                bullets = listOf(
                    "Customer Account & Verification: Full name, verified mobile number, email address, and encrypted passcode / OTP verification timestamps to secure customer accounts and deliver official certificates.",
                    "Bespoke Mattress Inquiries: Desired dimensions (length, width, thickness in inches), core firmness preferences, ergonomic comfort requirements, and custom tailoring notes for specialized mattress crafting.",
                    "Warranty & Guarantee Enrollment: Tax invoice/bill numbers, purchase/delivery dates, law label mattress serial/barcode numbers, and customer contact details to issue and enforce the immutable 25-Year Guarantee.",
                    "Service & Repair Tickets: Physical service address, mattress condition descriptions, issue photographs or notes, and technician scheduling preferences for doorstep inspections.",
                    "Concierge Feedback & Resolution: Customer satisfaction ratings, service evaluations, and grievance details submitted to uphold luxury service benchmarks.",
                    "Order Fulfillment Data: White-glove shipping addresses, selected sleep sanctuary accessories, order reference IDs, and payment status tokens for logistics dispatch.",
                    "Device & Performance Diagnostics: Non-identifying device telemetry (operating system build, crash reports, frame rendering metrics) solely to maintain 60/120fps fluid performance without collecting PII."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "3. How Your Data is Utilized",
                bullets = listOf(
                    "Order Delivery & White-Glove Dispatch: Coordinating factory craftsmanship, dispatching logistics partners, and streaming real-time delivery milestones.",
                    "25-Year Guarantee Verification: Maintaining the digital service ledger for scheduled 5-year doorstep inspection visits, pro-rata component swaps, and loyalty trade-in credits.",
                    "VIP Concierge Assistance: Facilitating authenticated resolution through direct phone, in-app messaging, or official WhatsApp concierge support.",
                    "Zero Third-Party Commercial Sale: Good Dream and SpringHaven will NEVER sell, lease, rent, trade, or monetize your personal or behavioral data to third-party advertisers or data brokers under any circumstances."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "4. Cryptographic Security & Device Safeguards",
                bullets = listOf(
                    "Hardware-Backed Keystore Encryption: Sensitive user session tokens, login flags, and credentials are encrypted at rest using AES-256-GCM backed by the Android Keystore system and Jetpack EncryptedSharedPreferences.",
                    "Cryptographic Salt & Hash: User passcodes are salted and hashed using cryptographic one-way functions before local persistence, guaranteeing that passcodes are never stored in plaintext.",
                    "End-to-End Transport Security (TLS 1.3): 100% of network data exchanges enforce HTTPS with modern TLS 1.3 encryption. Cleartext HTTP traffic is blocked system-wide in production builds.",
                    "Brute-Force Rate Limiting: Authentication endpoints and OTP verifications enforce 60-second cooldown timers and lockouts after consecutive failed attempts to prevent automated brute-force attacks.",
                    "Local Sandboxing & Zero PII Logging: Local databases reside inside private application sandboxes inaccessible to other apps on the device. Production build logs strip all Personally Identifiable Information (PII)."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "5. User Control, Data Portability & Erasure Rights",
                bullets = listOf(
                    "Full User Sovereignty: Under Google Play Store User Data rules and the Digital Personal Data Protection Act, you retain unrestricted control over your stored data.",
                    "Instant Local Data Purge: You can purge all cached records, active sessions, cart items, and custom preferences at any moment via the 'Delete Account & Clear Data' action in Account Settings.",
                    "Permanent Remote Deletion: To permanently delete all historical inquiries, warranty certificates, or customer service archives from our cloud databases, contact Lakshya190207@gmail.com or submit an authenticated ticket.",
                    "Data Rectification: You may update or correct your prefilled contact details and shipping addresses directly within your account profile at any time."
                )
            )
        }

        // Statutory Grievance Redressal Officer (Rule 4(4) of Consumer Protection E-Commerce Rules 2020)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "6. Statutory Grievance Redressal Officer",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SatinGoldAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "MANDATORY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatinGoldDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "In compliance with Rule 4(4) of the Consumer Protection (E-Commerce) Rules, 2020 and the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021, the contact details of the designated Nodal Grievance Redressal Officer are provided below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Officer Name:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("Mr. Rajeshwar Sharma", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Designation:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("Nodal Grievance Redressal Officer", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Entity Name:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("Good Dream Luxury Home Decor Pvt. Ltd.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Physical Address:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("Plot 42, KIADB Industrial Area, Phase II, Whitefield, Bengaluru, Karnataka - 560066, India", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Official Email:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("grievance@gooddream.in", fontSize = 11.5.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Telephone / Phone:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(110.dp))
                                Text("+91 80 4123 9999 (Mon–Sat, 10 AM–6 PM IST)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ForestGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Statutory Timelines: Acknowledgment within 48 hours • Complete Resolution within 30 days.",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 2: COOKIES, DEVICE STORAGE & LOCAL IDENTIFIERS POLICY (GOOGLE PLAY COMPLIANT)
// -------------------------------------------------------------------------------------
@Composable
private fun CookiePolicyContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ForestGreenContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Cookies, Storage & Local Identifiers Policy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenDark)
                        Text("Last Updated: September 2026 • Compliant with Google Play Store & EU ePrivacy Guidelines", fontSize = 10.sp, color = TextSecondaryMuted)
                    }
                }
            }
        }

        item {
            PolicyDisclosureCard(
                title = "1. How Cookies & Device Storage Work in Mobile Apps",
                bullets = listOf(
                    "Unlike standard desktop web browsers that rely primarily on text cookies, the Good Dream native Android app operates using secure Android OS sandboxed storage, encrypted SharedPreferences, SQLite Room databases, and temporary media caches.",
                    "When secure external payment gateways (UPI, NetBanking, Card checkout) or web documentation are accessed via WebViews, standard PCI-DSS compliant HTTP session cookies are utilized to protect transaction security."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "2. Storage Categories & Technologies We Use",
                bullets = listOf(
                    "Essential Security Storage (EncryptedSharedPreferences): Stores encrypted hardware-backed user auth tokens and security keys. Required for core account authentication.",
                    "Local Shopping & Warranty Database (SQLite Room): Caches product catalog items, cart selections ('cart_items'), wishlist preferences, and verified SpringHaven 25-Year Warranty records on your device so the app works reliably offline without data loss.",
                    "Media & Image Cache (Coil Disk Cache): Caches high-definition luxury mattress imagery inside your device's private cache folder ('context.cacheDir') to eliminate repeat downloads, save cellular bandwidth, and deliver fluid 60/120fps scrolling.",
                    "Diagnostic Performance Telemetry (Firebase Crashlytics): Anonymous, non-identifying installation tokens used strictly to diagnose memory leaks, crashes, and ANR (Application Not Responding) events."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "3. Zero Advertising Tracking & Zero Selling",
                bullets = listOf(
                    "We do NOT use advertising cookies, tracking beacons, cross-app ad identifiers (AAID), or marketing data brokers.",
                    "Your device identifiers and cache are never shared with or sold to data aggregation brokers."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "4. User Control & How to Clear Storage / Cookies",
                bullets = listOf(
                    "In-App 1-Tap Wipe: You can clear all cached images, local databases, cart state, and session tokens anytime by tapping 'Delete Account & Clear Data' in the Account screen.",
                    "Android System Settings: You can also reset all app storage via Android Settings > Apps > Good Dream > Storage & cache > 'Clear Storage' or 'Clear Cache'.",
                    "Third-Party Web Cookies: Banking payment gateway cookies expire automatically when checkout completes."
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 3: 100-NIGHT TRIAL & REFUND POLICY
// -------------------------------------------------------------------------------------
@Composable
private fun RefundPolicyContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SatinGoldAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, tint = SatinGoldDark, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100-Night Sleep Sanctuary Trial & Refund Terms", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Experience our master craftsmanship risk-free in the comfort of your bedroom.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            PolicyDisclosureCard(
                title = "1. The 100-Night Sleep Trial Program",
                bullets = listOf(
                    "Eligible Standard Mattresses: Applies to all standard-sized Good Dream and SpringHaven mattresses.",
                    "21-Night Adjustment Period: Because your spinal column and posture require 3 to 4 weeks to adjust to new ergonomic multi-zone support, we require sleeping on your mattress for at least 21 nights before initiating a return.",
                    "Returns up to 100 Nights: If you remain unsatisfied after 21 nights, you may request a 100% mattress collection and refund before day 100."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "2. Return Inspection & Hygiene Conditions",
                bullets = listOf(
                    "Sanitary Inspection: To protect our logistics partners, returned mattresses must be free from biological stains, liquid spills, pet damage, burns, or physical fabric tears.",
                    "Protector Recommended: We strongly recommend utilizing a waterproof mattress protector throughout the 100-night trial.",
                    "Original Tags: The law tags and factory serial label must remain intact on the mattress."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "3. Bespoke & Customized Furniture Exemption",
                bullets = listOf(
                    "Customized Dimensions: Non-standard custom cut mattresses, custom headboards, bespoke teak beds, and customized sofa-cum-beds are engineered exclusively to your exact room specifications.",
                    "Strictly Non-Refundable: Once wood cutting or foam fabric fabrication has commenced, customized furniture orders cannot be cancelled or returned.",
                    "Pre-dispatch confirmation is mandatory on all bespoke dimension orders."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "4. Order Cancellation & Transport Cost Deductions",
                bullets = listOf(
                    "Pre-Dispatch Cancellation: 100% full refund with zero deductions if cancelled prior to factory dispatch.",
                    "Cancellation In-Transit: Orders cancelled after the delivery vehicle has departed the fulfillment hub will incur actual round-trip transportation deduction (₹500 to ₹1,000).",
                    "Damaged on Delivery: If any damage occurs during shipping, our White-Glove team will immediately issue an on-the-spot replacement at zero charge."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "5. Refund Timeline & Processing",
                bullets = listOf(
                    "Refund Approval: Once the returned goods pass physical hygiene inspection, refund is approved within 48 hours.",
                    "Payout Duration: Funds are credited directly to your original payment method (UPI, Net Banking, Credit/Debit Card) within 5 to 7 business days.",
                    "Cash on Delivery orders are refunded via direct IMPS/NEFT bank transfer upon providing bank details."
                )
            )
        }

        item {
            PolicyDisclosureCard(
                title = "6. Cash on Delivery (COD) Policy: 20% Advance & 80% Upon Delivery",
                bullets = listOf(
                    "20% Advance Booking Deposit: To confirm custom factory fabrication and allocate white-glove logistics, all Cash on Delivery (COD) orders require an immediate 20% booking deposit paid online via UPI, Debit/Credit Card, or Net Banking.",
                    "80% Balance Upon Delivery: The remaining 80% balance is strictly payable to our White-Glove delivery specialist only after you inspect the unboxed mattress inside your bedroom.",
                    "Accepted Payment Modes on Delivery: The 80% balance can be settled via Cash, instant UPI QR code scan, or portable POS card terminal upon in-room setup.",
                    "100-Night Trial Coverage: The 20% advance booking deposit is fully protected under our 100-Night Sleep Sanctuary Trial and 100% money-back policy."
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// HELPER SUBCOMPONENTS
// -------------------------------------------------------------------------------------

@Composable
private fun ValuePillarItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ForestGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PolicySectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServiceScheduleRow(
    step: String,
    title: String,
    milestone: String,
    fee: String,
    isHighlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (isHighlight) SatinGoldAccent else ForestGreenPrimary,
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlight) ForestGreenDark else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Milestone: $milestone",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isHighlight) SatinGoldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = fee,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isHighlight) SatinGoldDark else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TransportRuleBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            fontWeight = FontWeight.Bold,
            color = SatinGoldAccent,
            fontSize = 14.sp
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CoveragePoint(isIncluded: Boolean, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (isIncluded) "✓ " else "× ",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isIncluded) ForestGreenPrimary else StatusError
        )
        Text(
            text = text,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PolicyDisclosureCard(
    title: String,
    bullets: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", color = SatinGoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
