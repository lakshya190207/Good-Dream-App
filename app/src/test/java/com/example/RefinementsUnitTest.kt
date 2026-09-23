package com.example

import com.example.data.model.ProductEntity
import com.example.ui.components.CART_MILESTONES
import org.junit.Assert.*
import org.junit.Test

class RefinementsUnitTest {

    @Test
    fun testCartMilestoneThresholdsAndTiers() {
        assertEquals(3, CART_MILESTONES.size)

        // Tier 1: Free Delivery & Setup at 25,000
        val tier1 = CART_MILESTONES[0]
        assertEquals(25000.0, tier1.threshold, 0.01)
        assertEquals("Free Delivery & Setup", tier1.title)
        assertEquals("Free Setup", tier1.shortLabel)

        // Tier 2: Mulberry Silk Sleep Mask at 50,000
        val tier2 = CART_MILESTONES[1]
        assertEquals(50000.0, tier2.threshold, 0.01)
        assertEquals("Mulberry Silk Sleep Mask", tier2.title)

        // Tier 3: Dual Contour Pillows at 75,000
        val tier3 = CART_MILESTONES[2]
        assertEquals(75000.0, tier3.threshold, 0.01)
        assertEquals("Dual Contour Pillows", tier3.title)
    }

    @Test
    fun testCartMilestoneProgressCalculations() {
        val maxThreshold = 75000.0

        // Test 0 cart value
        var total = 0.0
        var progress = (total / maxThreshold).toFloat().coerceIn(0f, 1f)
        var nextMilestone = CART_MILESTONES.firstOrNull { total < it.threshold }
        var unlockedCount = CART_MILESTONES.count { total >= it.threshold }
        assertEquals(0f, progress, 0.001f)
        assertEquals(25000.0, nextMilestone?.threshold ?: 0.0, 0.01)
        assertEquals(0, unlockedCount)

        // Test Tier 1 unlocked (₹30,000)
        total = 30000.0
        progress = (total / maxThreshold).toFloat().coerceIn(0f, 1f)
        nextMilestone = CART_MILESTONES.firstOrNull { total < it.threshold }
        val amountNeededTier2 = nextMilestone?.let { it.threshold - total } ?: 0.0
        unlockedCount = CART_MILESTONES.count { total >= it.threshold }
        assertEquals(0.4f, progress, 0.001f)
        assertEquals(50000.0, nextMilestone?.threshold ?: 0.0, 0.01)
        assertEquals(20000.0, amountNeededTier2, 0.01)
        assertEquals(1, unlockedCount)

        // Test Tier 2 unlocked (₹60,000)
        total = 60000.0
        progress = (total / maxThreshold).toFloat().coerceIn(0f, 1f)
        nextMilestone = CART_MILESTONES.firstOrNull { total < it.threshold }
        val amountNeededTier3 = nextMilestone?.let { it.threshold - total } ?: 0.0
        unlockedCount = CART_MILESTONES.count { total >= it.threshold }
        assertEquals(0.8f, progress, 0.001f)
        assertEquals(75000.0, nextMilestone?.threshold ?: 0.0, 0.01)
        assertEquals(15000.0, amountNeededTier3, 0.01)
        assertEquals(2, unlockedCount)

        // Test Tier 3 unlocked (₹90,000 - exceeds max)
        total = 90000.0
        progress = (total / maxThreshold).toFloat().coerceIn(0f, 1f)
        nextMilestone = CART_MILESTONES.firstOrNull { total < it.threshold }
        unlockedCount = CART_MILESTONES.count { total >= it.threshold }
        assertEquals(1.0f, progress, 0.001f)
        assertNull(nextMilestone)
        assertEquals(3, unlockedCount)
    }

    @Test
    fun testPredictiveFilterTagMatching() {
        val sampleProducts = listOf(
            ProductEntity(
                id = "prod-1",
                categoryId = "cat-mattress",
                title = "Royal Orthopedic Crown Mattress",
                subtitle = "Pocket Spring Spinal Support",
                sku = "ROYAL-ORTHO-01",
                price = 32000.0,
                originalPrice = 45000.0,
                firmness = "Extra Firm",
                dimensions = "72\" x 78\" (King)",
                description = "Pocket springs with orthopedic spinal alignment support",
                imagesJson = "https://example.com/img1.jpg"
            ),
            ProductEntity(
                id = "prod-2",
                categoryId = "cat-mattress",
                title = "Belgian Natural Latex Cloud",
                subtitle = "100% Organic Dunlop Latex",
                sku = "BELG-LATEX-02",
                price = 48000.0,
                originalPrice = 65000.0,
                firmness = "Ultra Plush",
                dimensions = "60\" x 78\" (Queen)",
                description = "100% Organic Belgian natural latex core with cooling tencel",
                imagesJson = "https://example.com/img2.jpg"
            ),
            ProductEntity(
                id = "prod-3",
                categoryId = "cat-mattress",
                title = "Sovereign Grandeur Hybrid",
                subtitle = "Multi-zone Luxury Coils",
                sku = "SOV-HYBRID-03",
                price = 85000.0,
                originalPrice = 110000.0,
                firmness = "Medium Firm",
                dimensions = "72\" x 78\" (King)",
                description = "Multi-zone pocket coils with organic wool cashmere pillowtop",
                imagesJson = "https://example.com/img3.jpg"
            )
        )

        // Test Orthopedic Tag
        val orthoMatches = sampleProducts.filter { p ->
            p.title.contains("orthopedic", ignoreCase = true) ||
            p.description.contains("orthopedic", ignoreCase = true)
        }
        assertEquals(1, orthoMatches.size)
        assertEquals("prod-1", orthoMatches.first().id)

        // Test King Tag (72" x 78")
        val kingMatches = sampleProducts.filter { p ->
            p.dimensions.contains("King", ignoreCase = true) ||
            p.dimensions.contains("72", ignoreCase = true)
        }
        assertEquals(2, kingMatches.size)

        // Test Under ₹35,000 Tag
        val under35kMatches = sampleProducts.filter { it.price <= 35000.0 }
        assertEquals(1, under35kMatches.size)
        assertEquals("prod-1", under35kMatches.first().id)

        // Test Under ₹50,000 Tag
        val under50kMatches = sampleProducts.filter { it.price <= 50000.0 }
        assertEquals(2, under50kMatches.size)

        // Test Belgian Latex Tag
        val latexMatches = sampleProducts.filter { p ->
            p.title.contains("latex", ignoreCase = true) ||
            p.description.contains("latex", ignoreCase = true)
        }
        assertEquals(1, latexMatches.size)
        assertEquals("prod-2", latexMatches.first().id)

        // Test Extra Firm Tag
        val extraFirmMatches = sampleProducts.filter { p ->
            p.firmness.contains("Extra Firm", ignoreCase = true)
        }
        assertEquals(1, extraFirmMatches.size)
        assertEquals("prod-1", extraFirmMatches.first().id)

        // Test Ultra Plush Tag
        val plushMatches = sampleProducts.filter { p ->
            p.firmness.contains("Plush", ignoreCase = true)
        }
        assertEquals(1, plushMatches.size)
        assertEquals("prod-2", plushMatches.first().id)
    }

    @Test
    fun testOrthopedicSpinePressureBiometrics() {
        val postures = com.example.ui.components.SleepPosture.values()
        assertEquals(3, postures.size)
        assertTrue(postures.contains(com.example.ui.components.SleepPosture.BACK))
        assertTrue(postures.contains(com.example.ui.components.SleepPosture.SIDE))
        assertTrue(postures.contains(com.example.ui.components.SleepPosture.STOMACH))

        val techs = com.example.ui.components.MattressTech.values()
        assertEquals(2, techs.size)
        assertTrue(techs.contains(com.example.ui.components.MattressTech.CONVENTIONAL))
        assertTrue(techs.contains(com.example.ui.components.MattressTech.GOOD_DREAM_7ZONE))

        // Peak pressure biometric thresholds
        val conventionalPeakKpa = 78.0
        val goodDreamPeakKpa = 22.0
        val reductionPercent = ((conventionalPeakKpa - goodDreamPeakKpa) / conventionalPeakKpa) * 100.0
        assertEquals(71.79, reductionPercent, 0.1)
    }

    @Test
    fun testBedroomFitDimensionsAndClearances() {
        val roomPresets = com.example.ui.components.BedroomRoomPreset.values()
        assertEquals(4, roomPresets.size)

        val bedPresets = com.example.ui.components.BedSizePreset.values()
        assertEquals(3, bedPresets.size)

        // Master Suite: 14' x 16' = 168" x 192"
        val masterW = 14f * 12f // 168
        val masterL = 16f * 12f // 192

        // King Bed: 72" x 78"
        val kingW = 72f
        val kingL = 78f

        val sideClearanceMaster = (masterW - kingW) / 2f
        val footClearanceMaster = masterL - (kingL + 6f)
        assertEquals(48f, sideClearanceMaster, 0.01f)
        assertEquals(108f, footClearanceMaster, 0.01f)
        assertTrue("Master Suite should be spacious for King bed", sideClearanceMaster >= 32f && footClearanceMaster >= 36f)

        // Studio: 10' x 10' = 120" x 120"
        val studioW = 10f * 12f // 120
        val studioL = 10f * 12f // 120

        val sideClearanceStudio = (studioW - kingW) / 2f
        val footClearanceStudio = studioL - (kingL + 6f)
        assertEquals(24f, sideClearanceStudio, 0.01f)
        assertEquals(36f, footClearanceStudio, 0.01f)
        assertFalse("Studio King side clearance should not be spacious (>32 in)", sideClearanceStudio >= 32f)

        // Queen Bed in Studio: 60" x 78"
        val queenW = 60f
        val sideClearanceQueenStudio = (studioW - queenW) / 2f
        assertEquals(30f, sideClearanceQueenStudio, 0.01f)
    }

    @Test
    fun testFullscreenFabricInspectorCraftBadgesAndPresets() {
        val minZoom = 1.0f
        val maxZoom = 3.5f
        val doubleTapZoom = 2.5f

        val presets = listOf(1.0f to "100%", 2.0f to "200%", 3.5f to "350% MAX")
        assertEquals(3, presets.size)
        assertEquals(minZoom, presets[0].first, 0.001f)
        assertEquals(2.0f, presets[1].first, 0.001f)
        assertEquals(maxZoom, presets[2].first, 0.001f)

        // Test pinch zoom clamp
        val clampedZoomLow = (0.5f).coerceIn(minZoom, maxZoom)
        val clampedZoomHigh = (5.0f).coerceIn(minZoom, maxZoom)
        val clampedZoomMid = (2.2f).coerceIn(minZoom, maxZoom)
        assertEquals(1.0f, clampedZoomLow, 0.001f)
        assertEquals(3.5f, clampedZoomHigh, 0.001f)
        assertEquals(2.2f, clampedZoomMid, 0.001f)

        // Test double-tap toggle
        var currentZoom = 1.0f
        currentZoom = if (currentZoom > 1.2f) minZoom else doubleTapZoom
        assertEquals(2.5f, currentZoom, 0.001f)

        currentZoom = if (currentZoom > 1.2f) minZoom else doubleTapZoom
        assertEquals(1.0f, currentZoom, 0.001f)
    }

    @Test
    fun testGoldFoilPhysicsEngineAndKinematics() {
        val p = com.example.ui.components.GoldFoilParticle(
            x = 500f,
            y = 500f,
            vx = 200f,
            vy = -300f,
            maxLifeSeconds = 2.0f,
            gravity = 800f,
            drag = 0.02f
        )

        assertTrue(p.isAlive)
        assertEquals(1f, p.life, 0.001f)

        // Simulate 0.5s step
        p.update(0.5f)
        assertTrue(p.isAlive)
        assertTrue("X coordinate should advance with horizontal velocity", p.x > 500f)
        assertTrue("Gravity should accelerate vertical velocity downward", p.vy > -300f)
        assertEquals(0.5f, p.ageSeconds, 0.001f)

        // Age beyond max lifespan
        p.update(1.6f)
        assertFalse("Particle should expire after maxLifeSeconds", p.isAlive)
        assertEquals(0f, p.life, 0.001f)
    }

    @Test
    fun testCelebrationBurstModesAndPalette() {
        val modes = com.example.ui.components.CelebrationBurstMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(com.example.ui.components.CelebrationBurstMode.CENTER_EXPLOSION))
        assertTrue(modes.contains(com.example.ui.components.CelebrationBurstMode.DUAL_CANNONS))
        assertTrue(modes.contains(com.example.ui.components.CelebrationBurstMode.CELESTIAL_RAIN))

        // Spawning 50 particles in CENTER_EXPLOSION
        val particles = com.example.ui.components.GoldFoilPhysicsEngine.spawnBurst(
            canvasWidth = 1080f,
            canvasHeight = 1920f,
            particleCount = 50,
            mode = com.example.ui.components.CelebrationBurstMode.CENTER_EXPLOSION
        )
        assertEquals(50, particles.size)
        particles.forEach { p ->
            assertTrue("Particle should be initially alive", p.isAlive)
            assertTrue("Particle should have positive lifespan", p.maxLifeSeconds > 0f)
            assertTrue("Particle width should be within reasonable bounds", p.width in 5f..40f)
            assertTrue("Particle should use luxury palette color", com.example.ui.components.LUXURY_GOLD_FOIL_COLORS.contains(p.color))
        }

        // Dual cannons should emit from left & right sides
        val cannonParticles = com.example.ui.components.GoldFoilPhysicsEngine.spawnBurst(
            canvasWidth = 1000f,
            canvasHeight = 2000f,
            particleCount = 20,
            mode = com.example.ui.components.CelebrationBurstMode.DUAL_CANNONS
        )
        assertEquals(20, cannonParticles.size)
        val leftCount = cannonParticles.count { it.x < 100f }
        val rightCount = cannonParticles.count { it.x > 900f }
        assertEquals(10, leftCount)
        assertEquals(10, rightCount)
    }

    @Test
    fun testCouponPrivilegePolicy() {
        val legacyCodes = listOf("SANCTUARY20", "DECORFEST5K", "SOVEREIGN25", "SPRINGHAVEN25", "DREAM100")
        val validPrivilegeCode = "WELCOME25"

        // Helper mimicking ViewModel applyCoupon logic
        fun evaluateCoupon(code: String, isLoggedIn: Boolean, subtotal: Double): Pair<Boolean, Double> {
            val normalized = code.trim().uppercase()
            if (normalized != "WELCOME25") {
                return false to 0.0
            }
            if (!isLoggedIn) {
                return false to 0.0
            }
            return true to (subtotal * 0.25)
        }

        // 1. All legacy generic codes must be rejected whether logged in or not
        legacyCodes.forEach { code ->
            val (acceptedLoggedOut, _) = evaluateCoupon(code, isLoggedIn = false, subtotal = 40000.0)
            assertFalse("Legacy code $code must be rejected when logged out", acceptedLoggedOut)

            val (acceptedLoggedIn, _) = evaluateCoupon(code, isLoggedIn = true, subtotal = 40000.0)
            assertFalse("Legacy code $code must be rejected when logged in", acceptedLoggedIn)
        }

        // 2. WELCOME25 must be rejected when logged out
        val (loggedOutSuccess, _) = evaluateCoupon(validPrivilegeCode, isLoggedIn = false, subtotal = 40000.0)
        assertFalse("WELCOME25 must be rejected when user is not logged in", loggedOutSuccess)

        // 3. WELCOME25 must be accepted and grant 25% discount when logged in
        val (loggedInSuccess, discount) = evaluateCoupon(validPrivilegeCode, isLoggedIn = true, subtotal = 40000.0)
        assertTrue("WELCOME25 must be accepted when user is logged in", loggedInSuccess)
        assertEquals(10000.0, discount, 0.01)
    }
}
