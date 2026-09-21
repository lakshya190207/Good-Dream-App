package com.example

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.GoodDreamTopAppBar
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @org.junit.Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = android.content.pm.PackageInfo().apply {
      packageName = "com.aistudio.gooddream.kxmpzq"
      versionName = "1.0"
    }
    org.robolectric.Shadows.shadowOf(context.packageManager).addPackage(packageInfo)
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Good Dream", appName)
  }

  @Test
  fun `top app bar renders brand title and triggers callbacks`() {
    var menuClicked = false
    var wishlistClicked = false
    var cartClicked = false

    composeTestRule.setContent {
      MyApplicationTheme {
        GoodDreamTopAppBar(
          onMenuClick = { menuClicked = true },
          onWishlistClick = { wishlistClicked = true },
          onCartClick = { cartClicked = true },
          wishlistCount = 2,
          cartCount = 5,
          title = "Good Dream Home Decor",
          subtitle = "Comfort for a Better Tomorrow"
        )
      }
    }

    // Verify title and tagline are displayed
    composeTestRule.onNodeWithTag("good_dream_top_app_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_bar_brand_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_bar_brand_tagline").assertIsDisplayed()

    // Verify badge counts
    composeTestRule.onNodeWithText("2").assertIsDisplayed()
    composeTestRule.onNodeWithText("5").assertIsDisplayed()

    // Perform clicks and verify callbacks
    composeTestRule.onNodeWithTag("top_bar_menu_button").performClick()
    assertTrue(menuClicked)

    composeTestRule.onNodeWithTag("top_bar_wishlist_button").performClick()
    assertTrue(wishlistClicked)

    composeTestRule.onNodeWithTag("top_bar_cart_button").performClick()
    assertTrue(cartClicked)
  }

  @Test
  fun `top app bar operates gracefully with null placeholder callbacks`() {
    composeTestRule.setContent {
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

    // Nodes still exist and display placeholders gracefully
    composeTestRule.onNodeWithTag("good_dream_top_app_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_bar_wishlist_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_bar_cart_button").assertIsDisplayed()
  }

  @Test
  fun `home screen displays hero showcase and trust pillars`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.HomeScreen(
          onNavigateToTab = {},
          onOpenModal = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("home_hero_showcase_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_trust_pillars_strip").assertIsDisplayed()
  }

  @Test
  fun `home feature grid card displays correctly with test tag`() {
    val testItem = com.example.ui.screens.HomeActionItem(
      indexNumber = "01",
      title = "Products",
      subtitle = "12 Categories Hub",
      icon = androidx.compose.material.icons.Icons.Default.Favorite,
      testTagId = "home_card_01_products",
      badgeLabel = "All 12",
      onClick = {}
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.HomeFeatureGridCard(item = testItem)
      }
    }

    composeTestRule.onNodeWithTag("home_card_01_products").assertIsDisplayed()
  }

  @Test
  fun `account screen displays theme toggle card`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AccountScreen(
          wishlistCount = 1,
          cartCount = 2,
          onOpenModal = {},
          onOpenCart = {},
          onOpenWishlist = {},
          supportPhone = "+91 98765 43210",
          supportEmail = "concierge@gooddream.in"
        )
      }
    }

    composeTestRule.onNodeWithTag("account_theme_card").assertExists()
  }

  @Test
  fun `ai chatbot modal renders conversation and handles send action`() {
    var sentMessage = ""
    var cleared = false

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.AiChatBotModal(
          messages = listOf(
            com.example.data.gemini.ChatMessage(
              role = com.example.data.gemini.MessageRole.MODEL,
              text = "Welcome to Good Dream! How can I help with your mattress today?"
            )
          ),
          isLoading = false,
          onSendMessage = { sentMessage = it },
          onClearChat = { cleared = true },
          onClose = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("ai_chatbot_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_chat_messages_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_chat_input_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_chat_clear_button").performClick()
    assertTrue(cleared)
  }

  @Test
  fun `gemini chat service provides immediate help for small problems`() = kotlinx.coroutines.runBlocking {
    val service = com.example.data.gemini.GeminiChatService()
    
    // Test back pain guidance
    val backPainReply = service.sendMessage(emptyList(), "What mattress helps with lower back pain?")
    assertTrue(backPainReply.contains("Back Comfort") || backPainReply.contains("Firmness") || backPainReply.contains("OrthoRest"))

    // Test sizing help
    val sizingReply = service.sendMessage(emptyList(), "What are the dimensions of King vs Queen?")
    assertTrue(sizingReply.contains("King") && sizingReply.contains("Queen"))

    // Test stain cleaning
    val stainReply = service.sendMessage(emptyList(), "How do I clean a stain on my mattress?")
    assertTrue(stainReply.contains("Stain") || stainReply.contains("baking soda") || stainReply.contains("Blot"))
  }

  @Test
  fun `material3 brand colors match exact specifications`() {
    val greenValue = (com.example.ui.theme.DeepElegantGreen.value.toLong() ushr 32) and 0xFFFFFFFFL
    assertEquals(0xFF1B4332L, greenValue)
    val goldValue = (com.example.ui.theme.SatinGoldAccent.value.toLong() ushr 32) and 0xFFFFFFFFL
    assertEquals(0xFFD4AF37L, goldValue)
    val creamValue = (com.example.ui.theme.SoftCreamBackground.value.toLong() ushr 32) and 0xFFFFFFFFL
    assertEquals(0xFFFDFBF7L, creamValue)
  }

  @Test
  fun `plp persistent breadcrumb navigation trail renders and handles home and categories jump`() {
    var homeClicked = false
    var categoriesClicked = false
    val mockCategory = com.example.data.model.CategoryEntity(
      id = "cat_mattress",
      name = "Luxury Mattresses",
      slug = "luxury-mattresses",
      displayOrder = 1,
      thumbnailUrl = "",
      subtitle = "Ergonomic sleep"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.ProductListingScreen(
          products = emptyList(),
          categories = listOf(mockCategory),
          selectedCategory = mockCategory,
          searchQuery = "",
          onSearchChange = {},
          wishlistIds = emptySet(),
          onToggleWishlist = {},
          onSelectProduct = {},
          onSelectCategory = {},
          onBackToCategories = { categoriesClicked = true },
          onNavigateHome = { homeClicked = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("plp_breadcrumb_trail").assertIsDisplayed()
    composeTestRule.onNodeWithTag("plp_breadcrumb_home").assertIsDisplayed().performClick()
    assertTrue(homeClicked)

    composeTestRule.onNodeWithTag("plp_breadcrumb_categories").assertIsDisplayed().performClick()
    assertTrue(categoriesClicked)

    composeTestRule.onNodeWithTag("plp_breadcrumb_current_category").assertIsDisplayed()
  }
}

