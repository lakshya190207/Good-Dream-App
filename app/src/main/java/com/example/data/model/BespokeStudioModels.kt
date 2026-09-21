package com.example.data.model

import java.io.Serializable

enum class BespokeCoreType(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val basePrice: Double,
    val code: String,
    val firmnessScore: Float
) : Serializable {
    POCKET_SPRINGS(
        id = "core_pocket_springs",
        title = "Swedish Pocket Springs",
        subtitle = "Zero-Disturbance Motion Isolation",
        description = "Over 1,200 individual heat-tempered pocketed coils providing contouring spinal support with zero partner disturbance.",
        basePrice = 34999.0,
        code = "SPRG",
        firmnessScore = 7.0f
    ),
    ORGANIC_NATURAL_LATEX(
        id = "core_organic_latex",
        title = "100% Organic Belgian Latex",
        subtitle = "Eco-Pure GOLS Certified Pincore",
        description = "Naturally hypoallergenic, breathable Belgian latex derived from organic Hevea milk with multi-zone pressure distribution.",
        basePrice = 46999.0,
        code = "LATX",
        firmnessScore = 6.5f
    ),
    ORTHOPEDIC_HIGH_RESILIENCE(
        id = "core_ortho_hr",
        title = "Dual-Density Ortho HR Core",
        subtitle = "Chiropractic Spinal Alignment",
        description = "Engineered high-resilience base foam with targeted lumbar transition zone to relieve lower-back tension.",
        basePrice = 29999.0,
        code = "ORTH",
        firmnessScore = 8.5f
    ),
    HYBRID_SOVEREIGN(
        id = "core_hybrid_sovereign",
        title = "Imperial Sovereign Hybrid",
        subtitle = "Pocket Coils + Organic Latex",
        description = "The pinnacle of sleep engineering: independent pocket springs crowned with a 100% natural organic latex comfort layer.",
        basePrice = 54999.0,
        code = "HYBR",
        firmnessScore = 7.5f
    )
}

enum class BespokeComfortLayer(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val additionalPrice: Double,
    val airflowRating: String
) : Serializable {
    BELGIAN_LATEX(
        id = "comfort_belgian_latex",
        title = "2\" Belgian Pincore Latex",
        subtitle = "Buoyant Organic Bounce",
        description = "Open-cell pincore structure ensures continuous air circulation with lively responsive bounce.",
        additionalPrice = 7500.0,
        airflowRating = "98% High"
    ),
    COOLING_GRAPHITE_GEL(
        id = "comfort_cryo_gel",
        title = "2\" Cryo-Gel Cooling Foam",
        subtitle = "Active Thermoregulation",
        description = "Infused with graphite particles and phase-change gel beads to whisk away body heat for a cool sleep climate.",
        additionalPrice = 5500.0,
        airflowRating = "92% Very Good"
    ),
    ORGANIC_WOOL_CASHMERE(
        id = "comfort_wool_cashmere",
        title = "Hand-Tufted Merino & Cashmere",
        subtitle = "Artisan Royal Softness",
        description = "Organic New Zealand merino wool and cashmere fleece hand-tufted for an ultra-plush, cocooning feel.",
        additionalPrice = 11000.0,
        airflowRating = "95% Excellent"
    ),
    ZERO_GRAVITY_FOAM(
        id = "comfort_zero_g",
        title = "2\" Zero-G Memory Cloud",
        subtitle = "Weightless Pressure Relief",
        description = "NASA-inspired viscoelastic foam that adapts seamlessly to the body contour, relieving pressure on joints.",
        additionalPrice = 4500.0,
        airflowRating = "88% Good"
    )
}

enum class BespokeQuiltCover(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val additionalPrice: Double
) : Serializable {
    ORGANIC_BAMBOO(
        id = "quilt_organic_bamboo",
        title = "450 GSM Organic Bamboo",
        subtitle = "Hypoallergenic & Silky",
        description = "Naturally antibacterial and silky smooth, woven from sustainably harvested bamboo fibers.",
        additionalPrice = 0.0
    ),
    BELGIAN_DAMASK(
        id = "quilt_belgian_damask",
        title = "Heritage Belgian Damask",
        subtitle = "Royal Woven Jacquard",
        description = "Classic heavy-weave damask fabric featuring intricate botanical patterns and hand-tufted rosettes.",
        additionalPrice = 5000.0
    ),
    MULBERRY_SILK(
        id = "quilt_mulberry_silk",
        title = "Imperial Mulberry Silk Jacquard",
        subtitle = "Lustrous Gold-Piped Finish",
        description = "Ultra-luxurious pure Mulberry silk blended jacquard adorned with hand-stitched Satin Gold cord piping.",
        additionalPrice = 8500.0
    )
}

enum class BespokeSizeStandard(
    val label: String,
    val widthInches: Int,
    val lengthInches: Int,
    val multiplier: Double,
    val code: String
) : Serializable {
    SINGLE("Single (72\" x 36\")", 36, 72, 0.70, "SGL"),
    DOUBLE("Double (75\" x 48\")", 48, 75, 0.85, "DBL"),
    QUEEN("Queen (78\" x 60\")", 60, 78, 0.95, "QEN"),
    KING("King (78\" x 72\")", 72, 78, 1.00, "KNG"),
    SUPER_KING("Super King (84\" x 78\")", 78, 84, 1.15, "SKG"),
    CUSTOM("Custom Sizing", 72, 78, 1.00, "CST")
}

enum class BespokeFirmness(
    val label: String,
    val score: String,
    val description: String
) : Serializable {
    PLUSH_SOFT("Plush Soft", "4.5 / 10", "Cloud-like gentle sink for side sleepers seeking shoulder and hip cushioning."),
    BALANCED_MEDIUM("Balanced Medium", "6.0 / 10", "The universal sweet spot combining gentle contour with uplifting spine support."),
    ORTHO_MEDIUM_FIRM("Ortho Medium-Firm", "7.5 / 10", "Our signature orthopedic density recommended for posture alignment."),
    EXTRA_FIRM("Extra Firm", "9.0 / 10", "Solid orthopedic foundation favored by stomach sleepers and chiropractic needs.")
}

enum class BespokeMonogramColor(
    val label: String,
    val hexColor: String
) : Serializable {
    SATIN_GOLD("Satin Gold", "#C5A059"),
    PLATINUM_SILVER("Platinum Silver", "#D1D5DB"),
    IMPERIAL_IVORY("Imperial Ivory", "#F7F5EE"),
    FOREST_EMERALD("Forest Emerald", "#102E23")
}

data class BespokeMattressConfiguration(
    val coreType: BespokeCoreType = BespokeCoreType.POCKET_SPRINGS,
    val comfortLayer: BespokeComfortLayer = BespokeComfortLayer.BELGIAN_LATEX,
    val quiltCover: BespokeQuiltCover = BespokeQuiltCover.ORGANIC_BAMBOO,
    val sizeStandard: BespokeSizeStandard = BespokeSizeStandard.KING,
    val customLengthInches: Int = 78,
    val customWidthInches: Int = 72,
    val customThicknessInches: Int = 10,
    val customMonogramText: String = "",
    val customMonogramColor: BespokeMonogramColor = BespokeMonogramColor.SATIN_GOLD,
    val firmnessPreference: BespokeFirmness = BespokeFirmness.ORTHO_MEDIUM_FIRM,
    val includeFoundationBedBase: Boolean = false,
    val includeMatchingPillows: Boolean = true
) : Serializable {

    fun getEffectiveDimensions(): Pair<Int, Int> {
        return if (sizeStandard == BespokeSizeStandard.CUSTOM) {
            customWidthInches to customLengthInches
        } else {
            sizeStandard.widthInches to sizeStandard.lengthInches
        }
    }

    fun getFormattedDimensions(): String {
        val (w, l) = getEffectiveDimensions()
        return "${w}\" x ${l}\" (${customThicknessInches}\" Profile)"
    }

    fun calculatePrice(): Double {
        val (width, length) = getEffectiveDimensions()
        val standardKingArea = 72.0 * 78.0 // 5,616 sq. inches
        val actualArea = (width * length).toDouble()
        val areaMultiplier = (actualArea / standardKingArea).coerceIn(0.6, 1.6)

        // Thickness adjustment (standard is 8 inches; +₹1,500 per additional inch)
        val thicknessAdjustment = ((customThicknessInches - 8).coerceAtLeast(0) * 1500.0)

        val baseComponent = (coreType.basePrice * areaMultiplier) + thicknessAdjustment
        val comfortComponent = comfortLayer.additionalPrice * areaMultiplier
        val quiltComponent = quiltCover.additionalPrice * areaMultiplier
        val monogramComponent = if (customMonogramText.isNotBlank()) 1500.0 else 0.0
        val bedBaseComponent = if (includeFoundationBedBase) 18000.0 else 0.0

        val total = baseComponent + comfortComponent + quiltComponent + monogramComponent + bedBaseComponent
        // Round to nearest 100
        return kotlin.math.round(total / 100.0) * 100.0
    }

    fun calculateCodAdvance(): Double {
        return kotlin.math.round((calculatePrice() * 0.20) / 100.0) * 100.0
    }

    fun generateSku(): String {
        val (w, l) = getEffectiveDimensions()
        return "BESPOKE-${coreType.code}-${sizeStandard.code}-${w}x${l}-${(1000..9999).random()}"
    }
}
