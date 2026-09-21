package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveModal
import com.example.ui.viewmodel.MainTab

@Composable
fun GoodDreamDrawerContent(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onOpenModal: (ActiveModal) -> Unit,
    onCloseDrawer: () -> Unit,
    supportPhone: String,
    supportEmail: String,
    isDarkMode: Boolean = false,
    isAdminAuthenticated: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .testTag("app_navigation_drawer")
            .widthIn(max = 330.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Header Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkMode) Color(0xFF132B20) else ForestGreenPrimary)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SatinGoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bed,
                                    contentDescription = null,
                                    tint = ForestGreenDark,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Good Dream",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Home Decor Pvt. Ltd.",
                                    color = SatinGoldLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreenDark.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "“Comfort for a Better Tomorrow”",
                                color = SatinGoldAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                DrawerSectionTitle("CATALOG NAVIGATION")

                DrawerMenuItem(
                    icon = Icons.Default.Home,
                    title = "Home Dashboard",
                    subtitle = "2-Column Quick Hub",
                    isSelected = currentTab == MainTab.HOME,
                    onClick = {
                        onSelectTab(MainTab.HOME)
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.GridView,
                    title = "All 12 Product Categories",
                    subtitle = "SpringHaven, Mattresses, Sofas",
                    isSelected = currentTab == MainTab.PRODUCTS,
                    onClick = {
                        onSelectTab(MainTab.PRODUCTS)
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "New Launches",
                    subtitle = "Latest Luxury Additions",
                    isSelected = currentTab == MainTab.NEW_LAUNCHES,
                    onClick = {
                        onSelectTab(MainTab.NEW_LAUNCHES)
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Layers,
                    title = "Bespoke Mattress Architect",
                    subtitle = "3D Custom Layer Atelier",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.BESPOKE_STUDIO)
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = BorderSubtle
                )
                DrawerSectionTitle("SERVICES & CUSTOM CARE")

                DrawerMenuItem(
                    icon = Icons.Default.SmartToy,
                    title = "AI Sleep & Decor Concierge",
                    subtitle = "Fast Help with Sizing, Care & Comfort",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.AI_CHAT_BOT)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = "Your Needs & Custom Orders",
                    subtitle = "Bespoke Size & Material Inquiries",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.YOUR_NEEDS)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Service & Warranties",
                    subtitle = "Serial Verification & 15-Yr Policy",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.SERVICE_WARRANTIES)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.HomeRepairService,
                    title = "Repairs & Maintenance",
                    subtitle = "Direct Support Ticketing",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.REPAIRS)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.SupportAgent,
                    title = "Complaints & Disputes",
                    subtitle = "24-Hour SLA Escalation",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.COMPLAINTS)
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.CardGiftcard,
                    title = "Sponsor Rewards & Referral",
                    subtitle = "Earn ₹2,500 Store Credit",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.SPONSOR_REWARDS)
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = BorderSubtle
                )
                if (isAdminAuthenticated) {
                    DrawerSectionTitle("ADMIN & STORE CONTROL")

                    DrawerMenuItem(
                        icon = Icons.Default.Tune,
                        title = "Store Admin Panel",
                        subtitle = "Unlocked • Photo/Product CRUD & CMS",
                        onClick = {
                            onCloseDrawer()
                            onOpenModal(ActiveModal.ADMIN_PANEL)
                        }
                    )
                }

                DrawerMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "Legal, 25-Yr Terms & Privacy",
                    subtitle = "Warranty Schedule, DPDP & Refund Policy",
                    onClick = {
                        onCloseDrawer()
                        onOpenModal(ActiveModal.LEGAL_POLICIES)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                DrawerSectionTitle("DISPLAY & THEME VARIANT")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isDarkMode) "Dark Mode Variant" else "Light Theme Haven",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkMode) "Deep pine & luminous gold" else "Soft cream & forest green",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleTheme() },
                            modifier = Modifier.testTag("drawer_theme_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Contact info footer card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Customer Support & Flagship Boutique",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📞 $supportPhone",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "✉️ $supportEmail",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📍 Sanctuary Flagship Experience Centre:\nNo. 44, Good Dream Pavilion, Interior Boulevard,\nIndiranagar, Bengaluru, Karnataka 560038",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
