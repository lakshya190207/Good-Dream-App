package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Reusable Material 3 CenterAlignedTopAppBar component for Good Dream Home Decor.
 *
 * Features:
 * - Leading Menu drawer icon with minimum 48dp touch target and accessibility semantics.
 * - Centered Brand identity title ("Good Dream Home Decor") with golden crest logo and tagline.
 * - Trailing Wishlist placeholder/action with active badged counts.
 * - Trailing Cart placeholder/action with active badged counts.
 * - Fully configurable callbacks, counts, visibility flags, and M3 scroll behaviors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodDreamTopAppBar(
    onMenuClick: () -> Unit = {},
    onWishlistClick: (() -> Unit)? = null,
    onCartClick: (() -> Unit)? = null,
    wishlistCount: Int = 0,
    cartCount: Int = 0,
    isDarkMode: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    title: String = stringResource(id = R.string.brand_name),
    subtitle: String? = stringResource(id = R.string.brand_tagline),
    showMenuIcon: Boolean = true,
    showWishlist: Boolean = true,
    showCart: Boolean = true,
    elevation: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val barContainerColor = if (isDarkMode) Color(0xFF10221A) else ForestGreenPrimary
    val barScrolledColor = if (isDarkMode) Color(0xFF091610) else ForestGreenDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = elevation)
            .testTag("top_app_bar_surface"),
        color = barContainerColor
    ) {
        CenterAlignedTopAppBar(
            modifier = Modifier
                .testTag("good_dream_top_app_bar"),
            windowInsets = TopAppBarDefaults.windowInsets,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = barContainerColor,
                scrolledContainerColor = barScrolledColor,
                navigationIconContentColor = SatinGoldAccent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            navigationIcon = {
                if (showMenuIcon) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .testTag("top_bar_menu_button")
                            .minimumInteractiveComponentSize()
                            .semantics {
                                contentDescription = "Open navigation drawer"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.cd_open_drawer),
                            tint = SatinGoldAccent
                        )
                    }
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("top_bar_title_container")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Golden Brand Crest Medallion
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(SatinGoldAccent)
                                .testTag("top_bar_brand_logo"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = ForestGreenDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("top_bar_brand_title")
                        )
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            ),
                            color = SatinGoldAccent,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("top_bar_brand_tagline")
                        )
                    }
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Theme Switcher Action
                    if (onToggleTheme != null) {
                        IconButton(
                            onClick = { onToggleTheme() },
                            modifier = Modifier
                                .testTag("top_bar_theme_toggle")
                                .minimumInteractiveComponentSize()
                                .cushionPressEffect(pressedScale = 0.88f)
                                .semantics {
                                    contentDescription = if (isDarkMode) "Switch to Light Haven Mode" else "Switch to Dark Sanctuary Mode"
                                }
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Switch to Light Haven Mode" else "Switch to Dark Sanctuary Mode",
                                tint = SatinGoldAccent
                            )
                        }
                    }

                    // Wishlist Placeholder / Action
                    if (showWishlist) {
                        IconButton(
                            onClick = { onWishlistClick?.invoke() },
                            modifier = Modifier
                                .testTag("top_bar_wishlist_button")
                                .minimumInteractiveComponentSize()
                                .cushionPressEffect(pressedScale = 0.88f)
                                .semantics {
                                    contentDescription = if (wishlistCount > 0) {
                                        "Wishlist with $wishlistCount items"
                                    } else {
                                        "Wishlist placeholder"
                                    }
                                }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (wishlistCount > 0) {
                                        Badge(
                                            containerColor = SatinGoldAccent,
                                            contentColor = ForestGreenDark,
                                            modifier = Modifier.testTag("top_bar_wishlist_badge")
                                        ) {
                                            AnimatedBadgeNumber(count = wishlistCount)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (wishlistCount > 0) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(id = R.string.cd_wishlist),
                                    tint = if (wishlistCount > 0) SatinGoldAccent else Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Cart Placeholder / Action
                    if (showCart) {
                        IconButton(
                            onClick = { onCartClick?.invoke() },
                            modifier = Modifier
                                .testTag("top_bar_cart_button")
                                .minimumInteractiveComponentSize()
                                .cushionPressEffect(pressedScale = 0.88f)
                                .semantics {
                                    contentDescription = if (cartCount > 0) {
                                        "Shopping Cart with $cartCount items"
                                    } else {
                                        "Shopping Cart placeholder"
                                    }
                                }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(
                                            containerColor = SatinGoldAccent,
                                            contentColor = ForestGreenDark,
                                            modifier = Modifier.testTag("top_bar_cart_badge")
                                        ) {
                                            AnimatedBadgeNumber(count = cartCount)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingCart,
                                    contentDescription = stringResource(id = R.string.cd_cart),
                                    tint = if (cartCount > 0) SatinGoldAccent else Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun GoodDreamTopAppBarPreview() {
    MyApplicationTheme {
        GoodDreamTopAppBar(
            onMenuClick = {},
            onWishlistClick = {},
            onCartClick = {},
            wishlistCount = 3,
            cartCount = 2
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun GoodDreamTopAppBarEmptyPlaceholderPreview() {
    MyApplicationTheme {
        GoodDreamTopAppBar(
            onMenuClick = {},
            onWishlistClick = null,
            onCartClick = null,
            wishlistCount = 0,
            cartCount = 0
        )
    }
}
