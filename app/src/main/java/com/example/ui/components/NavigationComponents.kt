package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.performLuxuryClick
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainTab

/**
 * GoodDreamHeader delegating to the reusable GoodDreamTopAppBar component.
 */
@Composable
fun GoodDreamHeader(
    onOpenDrawer: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenCart: () -> Unit,
    wishlistCount: Int,
    cartCount: Int,
    modifier: Modifier = Modifier
) {
    GoodDreamTopAppBar(
        onMenuClick = onOpenDrawer,
        onWishlistClick = onOpenWishlist,
        onCartClick = onOpenCart,
        wishlistCount = wishlistCount,
        cartCount = cartCount,
        modifier = modifier
    )
}

@Composable
fun GoodDreamBottomNavBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        GoodDreamNavBarItem(
            selected = currentTab == MainTab.HOME,
            onClick = { onTabSelected(MainTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = "Home",
            testTag = "nav_tab_home"
        )

        GoodDreamNavBarItem(
            selected = currentTab == MainTab.PRODUCTS,
            onClick = { onTabSelected(MainTab.PRODUCTS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Products"
                )
            },
            label = "Products",
            testTag = "nav_tab_products"
        )

        GoodDreamNavBarItem(
            selected = currentTab == MainTab.NEW_LAUNCHES,
            onClick = { onTabSelected(MainTab.NEW_LAUNCHES) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "New Launches"
                )
            },
            label = "New",
            testTag = "nav_tab_new_launches"
        )


        GoodDreamNavBarItem(
            selected = currentTab == MainTab.ACCOUNT,
            onClick = { onTabSelected(MainTab.ACCOUNT) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Account"
                )
            },
            label = "Account",
            testTag = "nav_tab_account"
        )
    }
}

@Composable
private fun RowScope.GoodDreamNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_icon_scale"
    )

    NavigationBarItem(
        selected = selected,
        onClick = {
            haptic.performLuxuryClick()
            onClick()
        },
        icon = {
            Box(modifier = Modifier.scale(iconScale)) {
                icon()
            }
        },
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .testTag(testTag)
            .cushionPressEffect(pressedScale = 0.92f)
    )
}
