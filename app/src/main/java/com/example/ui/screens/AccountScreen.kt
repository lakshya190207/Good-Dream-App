package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveModal

@Composable
fun AccountScreen(
    wishlistCount: Int,
    cartCount: Int,
    onOpenModal: (ActiveModal) -> Unit,
    onOpenCart: () -> Unit,
    onOpenWishlist: () -> Unit,
    supportPhone: String,
    supportEmail: String,
    privacyPolicyUrl: String = "https://gooddreamhomedecor.com/privacy-policy",
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    isUserLoggedIn: Boolean = false,
    loggedInUserEmail: String? = null,
    loggedInUserName: String? = null,
    isAdminAuthenticated: Boolean = false,
    onLoginUser: () -> Unit = {},
    onLogoutUser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF132B20) else ForestGreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
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
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SatinGoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isUserLoggedIn) {
                                        val name = loggedInUserName ?: "GD"
                                        val parts = name.trim().split(" ")
                                        if (parts.size >= 2) "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
                                        else name.take(2).uppercase()
                                    } else "GD",
                                    color = ForestGreenDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = if (isUserLoggedIn) (loggedInUserName ?: "Good Dream Member") else "Welcome, Guest",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isUserLoggedIn && !loggedInUserEmail.isNullOrBlank()) {
                                    Text(
                                        text = loggedInUserEmail,
                                        color = SatinGoldLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SatinGoldContainer
                                ) {
                                    Text(
                                        text = if (isUserLoggedIn) "⭐ PRIVILEGED SANCTUARY MEMBER" else "GUEST VISITOR",
                                        color = ForestGreenDark,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Action button: Logout if logged in, or Sign In CTA if guest
                        if (isUserLoggedIn) {
                            IconButton(
                                onClick = { showLogoutConfirmDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Prompt guest to sign in with Email OTP
                    if (!isUserLoggedIn) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onLoginUser,
                            colors = ButtonDefaults.buttonColors(containerColor = SatinGoldAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = ForestGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In with Email OTP",
                                color = ForestGreenDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ForestGreenMedium)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AccountStatItem(
                            label = "In Cart",
                            value = "$cartCount Items",
                            onClick = onOpenCart
                        )
                        AccountStatItem(
                            label = "Wishlist",
                            value = "$wishlistCount Saved",
                            onClick = onOpenWishlist
                        )
                        AccountStatItem(
                            label = "Rewards",
                            value = if (isUserLoggedIn) "2,500 Pts" else "0 Pts",
                            onClick = { onOpenModal(ActiveModal.PURCHASE_REWARDS) }
                        )
                    }
                }
            }
        }

        // Display & Theme Sanctuary Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_theme_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isDarkMode) "Dark Sanctuary Mode" else "Light Haven Mode",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDarkMode) "Deep pine & luminous gold accent" else "Soft cream & forest green palette",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("account_theme_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }


        // Section 1: Orders & Sleep Experience
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ORDERS & SLEEP EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SatinGoldDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        AccountMenuRow(
                            icon = Icons.Default.LocalShipping,
                            title = "Track White-Glove Order",
                            subtitle = "Live delivery milestones & driver details",
                            onClick = { onOpenModal(ActiveModal.ORDER_TRACKING) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.Bedtime,
                            title = "Sleep Sanctuary Firmness Quiz",
                            subtitle = "Find your tailored orthopedic match",
                            onClick = { onOpenModal(ActiveModal.SLEEP_QUIZ) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            title = "Mattress Comparison Matrix",
                            subtitle = "Side-by-side specs, coils & trials",
                            onClick = { onOpenModal(ActiveModal.COMPARE_PRODUCTS) }
                        )
                    }
                }
            }
        }

        // Section 2: Care, Inquiries & Support
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CARE, INQUIRIES & SUPPORT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SatinGoldDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        AccountMenuRow(
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            title = "Your Needs & Custom Inquiry",
                            subtitle = "Bespoke dimensions & quotes",
                            onClick = { onOpenModal(ActiveModal.YOUR_NEEDS) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.VerifiedUser,
                            title = "Service & Warranties",
                            subtitle = "Warranty registration & lookup",
                            onClick = { onOpenModal(ActiveModal.SERVICE_WARRANTIES) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.Build,
                            title = "Repairs & Maintenance",
                            subtitle = "Service engineer visit request",
                            onClick = { onOpenModal(ActiveModal.REPAIRS) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.SupportAgent,
                            title = "Customer Grievances & Complaints",
                            subtitle = "24-Hour SLA ticket support",
                            onClick = { onOpenModal(ActiveModal.COMPLAINTS) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.RateReview,
                            title = "Feedbacks & Reviews",
                            subtitle = "Share your comfort experience",
                            onClick = { onOpenModal(ActiveModal.FEEDBACKS) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.CardGiftcard,
                            title = "Sponsor Rewards Program",
                            subtitle = "Referral code & gift points",
                            onClick = { onOpenModal(ActiveModal.SPONSOR_REWARDS) }
                        )
                    }
                }
            }
        }

        // Section 3: Policies & Legal Terms
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "POLICIES & LEGAL TERMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SatinGoldDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        AccountMenuRow(
                            icon = Icons.Default.VerifiedUser,
                            title = "Terms of Service (25-Yr Guarantee)",
                            subtitle = "SpringHaven 25-year warranty schedule & terms",
                            onClick = { onOpenModal(ActiveModal.TERMS_OF_SERVICE) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy & Data Safety",
                            subtitle = "Review data collection, DPDP & privacy protection",
                            onClick = { onOpenModal(ActiveModal.PRIVACY_POLICY) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.Storage,
                            title = "Cookie & Storage Policy",
                            subtitle = "Local cache, Room database & disclosures",
                            onClick = { onOpenModal(ActiveModal.COOKIE_POLICY) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                            title = "Refund & Cancellation Policy",
                            subtitle = "100-night trial, return eligibility & timelines",
                            onClick = { onOpenModal(ActiveModal.REFUND_POLICY) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        AccountMenuRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account & Clear Data",
                            subtitle = "Purge local sessions, cart, wishlist & cache",
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }

        // Section 4: Staff & Administration (Strictly visible only if authenticated with official Admin credentials)
        if (isAdminAuthenticated) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "STAFF & ADMINISTRATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SatinGoldDark,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenModal(ActiveModal.ADMIN_PANEL) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SatinGoldContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ForestGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = SatinGoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Catalog Admin CMS Studio",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = ForestGreenDark
                                )
                                Text(
                                    text = "Access granted • Manage products, images & orders",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = ForestGreenDark,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Sign Out from Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of ${loggedInUserEmail ?: "your Good Dream account"}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogoutUser()
                        Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete Account & Personal Data",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "In compliance with Google Play Store User Data & Privacy policies, confirming this action will immediately purge all your locally saved cart items, wishlist records, inquiry references, and offline cache from this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "To request permanent deletion of cloud orders, invoices, or warranty ledger records from our servers without the app, you may also use our web portal or email Lakshya190207@gmail.com:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:Lakshya190207@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Account & Data Deletion Request - Good Dream")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Dear Good Dream Privacy Officer,\n\nPlease permanently delete my registered account and associated personal data.\n\nRegistered Email: ${loggedInUserEmail ?: ""}\nRegistered Name: ${loggedInUserName ?: ""}\n\nThank you."
                                    )
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Contact Lakshya190207@gmail.com", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email Officer", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://good-dream-75b67.web.app/delete-account"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Visit https://good-dream-75b67.web.app/delete-account", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web Portal", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onClearAllData()
                        Toast.makeText(context, "All account data, cart, and local sessions purged successfully.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete & Erase Data", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun AccountStatItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = value,
            color = SatinGoldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
    }
}
