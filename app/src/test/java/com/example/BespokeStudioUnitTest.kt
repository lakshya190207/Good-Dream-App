package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class BespokeStudioUnitTest {

    @Test
    fun testDefaultKingMattressPricing() {
        val config = BespokeMattressConfiguration(
            coreType = BespokeCoreType.POCKET_SPRINGS,
            comfortLayer = BespokeComfortLayer.BELGIAN_LATEX,
            quiltCover = BespokeQuiltCover.ORGANIC_BAMBOO,
            sizeStandard = BespokeSizeStandard.KING,
            customThicknessInches = 10,
            customMonogramText = "",
            includeFoundationBedBase = false
        )

        // Base Pocket Springs: 34,999 * 1.0 (King area) = 34,999
        // Thickness adjustment: (10 - 8) * 1,500 = 3,000
        // Belgian Latex Comfort: 7,500 * 1.0 = 7,500
        // Organic Bamboo Quilt: 0.0
        // Total = 34,999 + 3,000 + 7,500 = 45,499 -> rounded to nearest 100 = 45,500.0
        val price = config.calculatePrice()
        assertEquals(45500.0, price, 0.01)

        // 20% COD advance should be 20% of 45,500 = 9,100.0
        val advance = config.calculateCodAdvance()
        assertEquals(9100.0, advance, 0.01)

        // Verify dimensions
        val (w, l) = config.getEffectiveDimensions()
        assertEquals(72, w)
        assertEquals(78, l)
        assertEquals("72\" x 78\" (10\" Profile)", config.getFormattedDimensions())
    }

    @Test
    fun testCustomDimensionsAndMonogramPricing() {
        val config = BespokeMattressConfiguration(
            coreType = BespokeCoreType.HYBRID_SOVEREIGN,
            comfortLayer = BespokeComfortLayer.ORGANIC_WOOL_CASHMERE,
            quiltCover = BespokeQuiltCover.MULBERRY_SILK,
            sizeStandard = BespokeSizeStandard.CUSTOM,
            customWidthInches = 72,
            customLengthInches = 78,
            customThicknessInches = 12,
            customMonogramText = "THE STERLING SUITE",
            customMonogramColor = BespokeMonogramColor.SATIN_GOLD,
            includeFoundationBedBase = true
        )

        // Base Hybrid Sovereign: 54,999
        // Thickness: (12 - 8) * 1,500 = 6,000
        // Wool Cashmere: 11,000
        // Mulberry Silk: 8,500
        // Monogram: 1,500
        // Bed base: 18,000
        // Total = 54,999 + 6,000 + 11,000 + 8,500 + 1,500 + 18,000 = 99,999 -> rounded = 100,000.0
        val price = config.calculatePrice()
        assertEquals(100000.0, price, 0.01)

        // 20% COD advance = 20,000.0
        val advance = config.calculateCodAdvance()
        assertEquals(20000.0, advance, 0.01)

        // SKU generation
        val sku = config.generateSku()
        assertTrue(sku.startsWith("BESPOKE-HYBR-CST-72x78-"))
    }

    @Test
    fun testSingleSizePriceReduction() {
        val singleConfig = BespokeMattressConfiguration(
            coreType = BespokeCoreType.ORTHOPEDIC_HIGH_RESILIENCE,
            comfortLayer = BespokeComfortLayer.ZERO_GRAVITY_FOAM,
            quiltCover = BespokeQuiltCover.ORGANIC_BAMBOO,
            sizeStandard = BespokeSizeStandard.SINGLE,
            customThicknessInches = 8
        )

        val price = singleConfig.calculatePrice()
        // Single area (36x72) is much smaller than King (72x78), areaMultiplier is 0.6
        // Total should be substantially lower than king
        assertTrue(price < 25000.0)
    }
}
