package com.example.data.repository

import com.example.data.model.AppConfigEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity

object DefaultCatalogData {

    val categories = listOf(
        CategoryEntity(
            id = "cat_springhaven",
            name = "SpringHaven Series",
            slug = "springhaven-series",
            displayOrder = 1,
            thumbnailUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=800&auto=format&fit=crop",
            subtitle = "Flagship ~15-inch Integrated Bed & Mattress Setups"
        ),
        CategoryEntity(
            id = "cat_mattress",
            name = "Mattress",
            slug = "mattress",
            displayOrder = 2,
            thumbnailUrl = "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=800&auto=format&fit=crop",
            subtitle = "Orthopedic, Memory Foam & Pocket Spring"
        ),
        CategoryEntity(
            id = "cat_pillow_bolster",
            name = "Pillow, Bolster and Cushion",
            slug = "pillow-bolster-cushion",
            displayOrder = 3,
            thumbnailUrl = "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?q=80&w=800&auto=format&fit=crop",
            subtitle = "Ergonomic Sleeping & Support Accessories"
        ),
        CategoryEntity(
            id = "cat_covers",
            name = "Cushion, Pillow, Bolster Covers",
            slug = "cushion-pillow-bolster-covers",
            displayOrder = 4,
            thumbnailUrl = "https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?q=80&w=800&auto=format&fit=crop",
            subtitle = "Artisanal Velvet, Cotton & Linen Covers"
        ),
        CategoryEntity(
            id = "cat_bedsheet",
            name = "Bedsheet & Protector",
            slug = "bedsheet-protector",
            displayOrder = 5,
            thumbnailUrl = "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=800&auto=format&fit=crop",
            subtitle = "100% Egyptian Cotton & Waterproof Protectors"
        ),
        CategoryEntity(
            id = "cat_curtains",
            name = "Curtains",
            slug = "curtains",
            displayOrder = 6,
            thumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=800&auto=format&fit=crop",
            subtitle = "Thermal Blackout, Linen & Sheer Draperies"
        ),
        CategoryEntity(
            id = "cat_sofa",
            name = "Sofa",
            slug = "sofa",
            displayOrder = 7,
            thumbnailUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=800&auto=format&fit=crop",
            subtitle = "Living Room Sectionals, Loungers & Sets"
        ),
        CategoryEntity(
            id = "cat_furnishers",
            name = "Home Furnishers",
            slug = "home-furnishers",
            displayOrder = 8,
            thumbnailUrl = "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?q=80&w=800&auto=format&fit=crop",
            subtitle = "Accent Consoles, Wall Decor & Lamps"
        ),
        CategoryEntity(
            id = "cat_office",
            name = "Office Furniture",
            slug = "office-furniture",
            displayOrder = 9,
            thumbnailUrl = "https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=800&auto=format&fit=crop",
            subtitle = "Ergonomic Desks & Modular Workstations"
        ),
        CategoryEntity(
            id = "cat_chairs",
            name = "Chairs & Revolving Chairs",
            slug = "chairs-revolving-chairs",
            displayOrder = 10,
            thumbnailUrl = "https://images.unsplash.com/photo-1580481077195-731da03fed7e?q=80&w=800&auto=format&fit=crop",
            subtitle = "Executive Rolling & Accent Armchairs"
        ),
        CategoryEntity(
            id = "cat_doormats",
            name = "Doormats",
            slug = "doormats",
            displayOrder = 11,
            thumbnailUrl = "https://images.unsplash.com/photo-1600585154526-990dced4db0d?q=80&w=800&auto=format&fit=crop",
            subtitle = "Coir, Anti-Skid & Luxury Welcome Mats"
        ),
        CategoryEntity(
            id = "cat_others",
            name = "Others",
            slug = "others",
            displayOrder = 12,
            thumbnailUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?q=80&w=800&auto=format&fit=crop",
            subtitle = "Bespoke Hardware & Special Accessories"
        )
    )

    val products = listOf(
        // SPRINGHAVEN LUXURY FLAGSHIP SERIES (~15-inch ultra-thick bed setups)
        ProductEntity(
            id = "prod_springhaven_emperor",
            categoryId = "cat_springhaven",
            title = "SpringHaven Grand Sovereign 15\" Bed & Mattress Setup",
            subtitle = "Integrated High-Profile Luxury Sleep Sanctuary",
            sku = "GD-SH-1501",
            isSpringhavenSeries = true,
            description = "The pinnacle of Good Dream engineering. Featuring an ultra-thick 15-inch integrated multi-tier micro-pocketed spring core topped with Belgian cashmere quilting and adaptive European natural latex. Designed for royal restorative sleep with zero motion transfer.",
            specificationsJson = "Total Height: 15 Inches Integrated Setup|Comfort Layer: 4-inch Organic Belgian Latex & Gel Memory Foam|Core Foundation: 7-Zone Independent Pocket Springs|Quilt: Hand-tufted Cashmere & Silk Damask|Warranty: 25 Years Comprehensive Limited Warranty|Dimensions: 78\" x 72\" (King Size)|Firmness: Medium Plush Luxury (6/10)",
            price = 89999.0,
            originalPrice = 115000.0,
            imagesJson = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop|https://images.unsplash.com/photo-1540518614846-7ede433c4ef0?q=80&w=1000&auto=format&fit=crop|https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 25,
            thicknessInches = 15,
            material = "Organic Belgian Latex + 7-Zone Titanium Springs",
            dimensions = "78\" x 72\" King",
            firmness = "Medium Plush Luxury",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        ProductEntity(
            id = "prod_springhaven_regalia",
            categoryId = "cat_springhaven",
            title = "SpringHaven Regalia 14.5\" High-Profile Ensemble",
            subtitle = "Deep Foundation Integrated Master Suite",
            sku = "GD-SH-1502",
            isSpringhavenSeries = true,
            description = "Crafted for luxury residential suites. Built with a dual-spring architecture that pairs 2,200 micro-coils with an acoustic-isolated base framework. Breathable horsehair underlay ensures year-round thermal stability.",
            specificationsJson = "Total Height: 14.5 Inches|Coil Count: 2,200 Dual Layer Pocket Springs|Cover: Organic GOTS Certified Cotton|Edge Support: Reinforced Quantum Perimeter Guard|Warranty: 25 Years Comprehensive Limited Warranty|Dimensions: 78\" x 60\" (Queen Size)|Firmness: Balanced Ergonomic Support (7/10)",
            price = 74999.0,
            originalPrice = 95000.0,
            imagesJson = "https://images.unsplash.com/photo-1540518614846-7ede433c4ef0?q=80&w=1000&auto=format&fit=crop|https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 25,
            thicknessInches = 15,
            material = "Dual Layer Quantum Micro-Coils",
            dimensions = "78\" x 60\" Queen",
            firmness = "Balanced Ergonomic"
        ),
        ProductEntity(
            id = "prod_springhaven_monarch",
            categoryId = "cat_springhaven",
            title = "SpringHaven Monarch 15\" Pillow-Top Bedset",
            subtitle = "Box-Spring & Quilted Headboard Master Package",
            sku = "GD-SH-1503",
            isSpringhavenSeries = true,
            description = "A comprehensive luxury ensemble featuring an upholstered hydraulic bedstead and 15-inch deep pillowtop mattress with natural cooling copper infusion.",
            specificationsJson = "Total Height: 15 Inches Mattress + Foundation|Foam: Copper-infused Temperature Neutral Foam|Base: Solid Kiln-Dried Teak Wood Skeleton|Warranty: 25 Years Comprehensive Limited Warranty|Dimensions: 84\" x 72\" (Super King)|Firmness: Cloud Comfort (5.5/10)",
            price = 99999.0,
            originalPrice = 128000.0,
            imagesJson = "https://images.unsplash.com/photo-1616046229478-9901c5536a45?q=80&w=1000&auto=format&fit=crop|https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 25,
            thicknessInches = 15,
            material = "Copper Foam + Teak Foundation",
            dimensions = "84\" x 72\" Super King",
            firmness = "Cloud Comfort Plush"
        ),

        // MATTRESS CATEGORY
        ProductEntity(
            id = "prod_mattress_ortho",
            categoryId = "cat_mattress",
            title = "Good Dream OrthoCare SpineAlign 8\"",
            subtitle = "Certified Doctor Recommended Orthopedic Support",
            sku = "GD-MAT-0201",
            isSpringhavenSeries = false,
            description = "Engineered with 5-zone rebonded high-density foam and high-resilience memory layer for targeted lumbar alignment and sciatic pain relief.",
            specificationsJson = "Thickness: 8 Inches|Core: 100D High-Density Rebonded Foam|Top Layer: Pressure-Relieving Memory Foam|Cover: Anti-microbial Knitted Fabric|Warranty: 10 Years|Firmness: Firm Orthopedic (8/10)",
            price = 24999.0,
            originalPrice = 32000.0,
            imagesJson = "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1000&auto=format&fit=crop|https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 10,
            thicknessInches = 8,
            material = "High Density Rebonded Foam",
            dimensions = "78\" x 72\" King",
            firmness = "Firm Orthopedic"
        ),
        ProductEntity(
            id = "prod_mattress_pocket",
            categoryId = "cat_mattress",
            title = "Good Dream CloudRest Pocket Spring 10\"",
            subtitle = "Zero Partner Disturbance Pocket Coils",
            sku = "GD-MAT-0202",
            isSpringhavenSeries = false,
            description = "Each pocket spring moves independently inside individual fabric sleeves, isolating movement and contouring accurately to your natural spine curvature.",
            specificationsJson = "Thickness: 10 Inches|Spring Type: Barrel-Shaped Pocket Coils|Topper: 2-inch High-Density Foam|Cover: Quilted Belgian Jacquard|Warranty: 10 Years|Firmness: Medium Firm (6.5/10)",
            price = 31999.0,
            originalPrice = 41000.0,
            imagesJson = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 10,
            thicknessInches = 10,
            material = "Individually Encased Coils",
            dimensions = "78\" x 60\" Queen",
            firmness = "Medium Firm"
        ),

        // PILLOW, BOLSTER AND CUSHION
        ProductEntity(
            id = "prod_pillow_memory",
            categoryId = "cat_pillow_bolster",
            title = "Contour Gel Memory Foam Cervical Pillow",
            subtitle = "Ergonomic Neck Curve Contour",
            sku = "GD-PLW-0301",
            isSpringhavenSeries = false,
            description = "Dual-height orthopedic butterfly shape supports cervical vertebrae, reducing morning neck stiffness and snoring.",
            specificationsJson = "Dimensions: 24\" x 15\" x 4.5\"|Core: Pure MDI Cooling Gel Memory Foam|Cover: Washable Bamboo Fiber|Warranty: 3 Years",
            price = 2499.0,
            originalPrice = 3499.0,
            imagesJson = "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 3,
            thicknessInches = 5,
            material = "Cooling Gel Memory Foam",
            dimensions = "24\" x 15\"",
            firmness = "Medium"
        ),
        ProductEntity(
            id = "prod_bolster_down",
            categoryId = "cat_pillow_bolster",
            title = "Good Dream Royal Plush Bolster Roll",
            subtitle = "Hypoallergenic Microfiber Body Support",
            sku = "GD-PLW-0302",
            isSpringhavenSeries = false,
            description = "Generously stuffed with 1200 GSM micro-down alternative fibers for resilient side-sleeper hug and posture support.",
            specificationsJson = "Dimensions: 36\" x 9\" Cylinder|Fill: Down Alternative Microfiber|Shell: 300TC Cotton Sateen|Warranty: 2 Years",
            price = 1899.0,
            originalPrice = 2499.0,
            imagesJson = "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 2,
            thicknessInches = 9,
            material = "Micro-down Alternative",
            dimensions = "36\" x 9\"",
            firmness = "Plush Soft"
        ),

        // CUSHION, PILLOW, BOLSTER COVERS
        ProductEntity(
            id = "prod_covers_velvet",
            categoryId = "cat_covers",
            title = "Heritage Satin Gold & Forest Embroidered Cushion Covers (Set of 5)",
            subtitle = "Artisanal Zari Work Decorative Accent Cases",
            sku = "GD-COV-0401",
            isSpringhavenSeries = false,
            description = "Handcrafted with luxury heavy velvet in signature forest green with satin gold thread embroidery. Invisible zip closures.",
            specificationsJson = "Quantity: Pack of 5|Sizes: 16\" x 16\" (40cm x 40cm)|Fabric: 350 GSM Micro-Velvet|Care: Gentle Dry Clean or Handwash",
            price = 1999.0,
            originalPrice = 2999.0,
            imagesJson = "https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 1,
            thicknessInches = 1,
            material = "Micro-Velvet + Zari Gold",
            dimensions = "16\" x 16\"",
            firmness = "Soft"
        ),

        // BEDSHEET & PROTECTOR
        ProductEntity(
            id = "prod_bedsheet_egyptian",
            categoryId = "cat_bedsheet",
            title = "1000TC Egyptian Long-Staple Cotton King Bedset",
            subtitle = "Includes Fitted Sheet, Flat Sheet & 2 Pillowcases",
            sku = "GD-BED-0501",
            isSpringhavenSeries = false,
            description = "Silky smooth lustrous sateen weave crafted from 100% Giza Egyptian cotton fibers. Deep 16\" pocket fits high-profile SpringHaven beds effortlessly.",
            specificationsJson = "Thread Count: 1000 TC|Drop Depth: Fits up to 16\" mattresses|Material: 100% Extra Long Staple Cotton|Finish: Silk Sateen Luster",
            price = 4599.0,
            originalPrice = 6499.0,
            imagesJson = "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 2,
            thicknessInches = 1,
            material = "1000TC Egyptian Cotton",
            dimensions = "108\" x 108\"",
            firmness = "Silky Smooth"
        ),
        ProductEntity(
            id = "prod_protector_waterproof",
            categoryId = "cat_bedsheet",
            title = "Good Dream AquaShield Bamboo Waterproof Mattress Protector",
            subtitle = "Noiseless TPU Breathable Membrane",
            sku = "GD-BED-0502",
            isSpringhavenSeries = false,
            description = "Protects your investment against spills, stains, and allergens without crunching sounds or heat buildup.",
            specificationsJson = "Surface: 100% Organic Bamboo Terry|Backing: 100% Waterproof TPU Membrane|Skirt: 360 Elastic Deep Pocket (Up to 16\")|Machine Washable: Yes",
            price = 1799.0,
            originalPrice = 2499.0,
            imagesJson = "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 3,
            thicknessInches = 1,
            material = "Bamboo Terry + TPU",
            dimensions = "78\" x 72\"",
            firmness = "Supple"
        ),

        // CURTAINS
        ProductEntity(
            id = "prod_curtains_blackout",
            categoryId = "cat_curtains",
            title = "Thermal Weave 100% Blackout Drapes (Pair)",
            subtitle = "Noise Reducing & UV Heat Insulating",
            sku = "GD-CRT-0601",
            isSpringhavenSeries = false,
            description = "Triple-weave heavy blackout drapery prevents 99.9% sunlight intrusion, lowering room temperatures in summer and retaining cozy warmth in winter.",
            specificationsJson = "Header: Rust-proof Brass Grommets (8 per panel)|Width: 54\" per panel (108\" total)|Length: 9 Feet (Floor to ceiling)|Weight: 380 GSM",
            price = 3299.0,
            originalPrice = 4599.0,
            imagesJson = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 3,
            thicknessInches = 1,
            material = "Triple-Weave Polyester Velvet",
            dimensions = "9 Ft x 4.5 Ft",
            firmness = "Heavy Drape"
        ),

        // SOFA
        ProductEntity(
            id = "prod_sofa_chesterfield",
            categoryId = "cat_sofa",
            title = "The Windsor Emerald Velvet 3-Seater Chesterfield Sofa",
            subtitle = "Deep Diamond Button Tufting & Solid Sheesham Base",
            sku = "GD-SOF-0701",
            isSpringhavenSeries = false,
            description = "Iconic British styling reimagined in rich emerald green velvet with hand-turned antiqued brass nailhead trim. High-density 40D foam ensures no sagging over years of living room gatherings.",
            specificationsJson = "Seating Capacity: 3 Persons|Frame: Seasoned Sheesham Hardwood|Cushioning: 40D Super-Soft Foam + S-Springs|Upholstery: Water-Repellent Velvet|Warranty: 7 Years Structural",
            price = 54999.0,
            originalPrice = 72000.0,
            imagesJson = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 7,
            thicknessInches = 34,
            material = "Sheesham Wood + Velvet",
            dimensions = "88\" W x 36\" D x 32\" H",
            firmness = "Medium Firm"
        ),

        // HOME FURNISHERS
        ProductEntity(
            id = "prod_furnisher_console",
            categoryId = "cat_furnishers",
            title = "Aura Brass & Fluted Oak Entryway Console Table",
            subtitle = "Nordic Architectural Accent Credenza",
            sku = "GD-FRN-0801",
            isSpringhavenSeries = false,
            description = "Clean tambour fluted wooden curves crowned with brushed satin gold metal accents. Features soft-close storage drawers for keys and essentials.",
            specificationsJson = "Dimensions: 48\" L x 14\" D x 32\" H|Wood: White Oak Veneer on engineered board|Hardware: Brushed Gold Brass Alloy|Assembly: Pre-assembled",
            price = 22999.0,
            originalPrice = 29999.0,
            imagesJson = "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 5,
            thicknessInches = 32,
            material = "Fluted Oak & Brass",
            dimensions = "48\" x 14\" x 32\"",
            firmness = "Solid Hardwood"
        ),

        // OFFICE FURNITURE
        ProductEntity(
            id = "prod_office_desk",
            categoryId = "cat_office",
            title = "Apex Dual-Motor Electric Height-Adjustable Standing Desk",
            subtitle = "Solid Walnut Top with Anti-Collision Gyro Sensor",
            sku = "GD-OFF-0901",
            isSpringhavenSeries = false,
            description = "Transition smoothly from sitting to standing with whisper-quiet dual motors (<45dB). 4 programmable height memory presets and integrated cable channel tray.",
            specificationsJson = "Height Range: 24\" to 50\"|Load Capacity: 130 kg|Top: Solid Natural Walnut 60\" x 30\"|Warranty: 5 Years Motor & Electronics",
            price = 36999.0,
            originalPrice = 48000.0,
            imagesJson = "https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 5,
            thicknessInches = 30,
            material = "Steel Frame + Walnut Hardwood",
            dimensions = "60\" x 30\"",
            firmness = "Heavy Duty"
        ),

        // CHAIRS & REVOLVING CHAIRS
        ProductEntity(
            id = "prod_chair_executive",
            categoryId = "cat_chairs",
            title = "Good Dream Ergonomic Pro Executive Revolving Chair",
            subtitle = "3D Dynamic Lumbar & Synchronized Multi-Angle Recline",
            sku = "GD-CHR-1001",
            isSpringhavenSeries = false,
            description = "Crafted for 12+ hour seated comfort. German Class 4 gas lift cylinder, breathable aerospace mesh backrest, and 4D adjustable armrests.",
            specificationsJson = "Recline Angle: 90° - 135° Lockable|Gas Lift: Class 4 BIFMA Certified|Base: Aluminum Alloy 5-Star Wheels|Weight Support: Up to 150 kg|Warranty: 3 Years",
            price = 18999.0,
            originalPrice = 24999.0,
            imagesJson = "https://images.unsplash.com/photo-1580481077195-731da03fed7e?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 3,
            thicknessInches = 48,
            material = "Aerospace Mesh & Aluminum",
            dimensions = "Standard Executive",
            firmness = "Ergonomic Mesh"
        ),

        // DOORMATS
        ProductEntity(
            id = "prod_doormats_welcome",
            categoryId = "cat_doormats",
            title = "Heavy-Duty Natural Coconut Coir Welcome Mat",
            subtitle = "Embossed Monogram Border with Non-Slip Rubber Base",
            sku = "GD-MAT-1101",
            isSpringhavenSeries = false,
            description = "Dense 100% natural coconut fibers scrape off dirt, mud, and moisture instantly. Heavy non-slip vulcanized rubber border keeps mat anchored.",
            specificationsJson = "Dimensions: 30\" x 18\" (75cm x 45cm)|Thickness: 1.5 cm Thick Coir Bristles|Backing: Weatherproof Vulcanized Rubber|Indoor/Outdoor: All-Weather",
            price = 999.0,
            originalPrice = 1499.0,
            imagesJson = "https://images.unsplash.com/photo-1600585154526-990dced4db0d?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = false,
            warrantyYears = 1,
            thicknessInches = 1,
            material = "Natural Coir + Rubber",
            dimensions = "30\" x 18\"",
            firmness = "Stiff Bristle"
        ),

        // OTHERS
        ProductEntity(
            id = "prod_others_diffuser",
            categoryId = "cat_others",
            title = "Good Dream Signature Aromatherapy Ultrasonic Diffuser & Essential Oils",
            subtitle = "Warm Ambient Glow & Pure Cedarwood Sleep Oil",
            sku = "GD-OTH-1201",
            isSpringhavenSeries = false,
            description = "Transform your bedroom into a tranquil sleep sanctuary. Whisper-quiet ultrasonic atomization with waterless auto-shutoff.",
            specificationsJson = "Capacity: 400 ml Tank (10 Hours continuous)|Light: 7 Color Ambient Mood Ring|Included: 15ml Lavender & Cedarwood Blend|Power: Type-C 5V",
            price = 2899.0,
            originalPrice = 3999.0,
            imagesJson = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?q=80&w=1000&auto=format&fit=crop",
            isNewLaunch = true,
            warrantyYears = 1,
            thicknessInches = 6,
            material = "BPA-Free Matte Ceramic",
            dimensions = "6\" x 6\" x 8\"",
            firmness = "Ceramic"
        )
    )

    val appConfigs = listOf(
        AppConfigEntity("company_name", "Good Dream Home Decor Private Limited"),
        AppConfigEntity("tagline", "Comfort for a Better Tomorrow"),
        AppConfigEntity("support_phone", "+91 80 4123 9999"),
        AppConfigEntity("support_whatsapp", "+91 98765 43210"),
        AppConfigEntity("support_email", "care@gooddreamhomedecor.com"),
        AppConfigEntity("store_address", "No. 44, Good Dream Pavilion, Interior Boulevard, Indiranagar, Bengaluru, Karnataka 560038"),
        AppConfigEntity("warranty_policy_url", "https://gooddreamhomedecor.com/warranty-policy"),
        AppConfigEntity("banner_offer", "Festive Grand Launch: Complimentary Silk Pillows with SpringHaven Series!"),
        AppConfigEntity("sponsor_referral_reward", "₹2,500 Store Credit on Every Friend Referral")
    )
}
