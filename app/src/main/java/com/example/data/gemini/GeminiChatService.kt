package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiChatService {

    companion object {
        private const val TAG = "GeminiChatService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        // Supported Gemini models configured for maximum cost efficiency and speed
        const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite" // Cost-efficient, ultra-fast, lightweight (Default)
        const val MODEL_FLASH = "gemini-3.5-flash" // Standard general tasks
        const val MODEL_PRO = "gemini-3.1-pro-preview" // Complex reasoning

        const val DEFAULT_MODEL = MODEL_FLASH_LITE

        // Limits context history to the last 6 messages (3 conversational turns)
        // to prevent quadratic token growth and save substantially on API billing
        const val MAX_HISTORY_MESSAGES = 6

        // Limits maximum output tokens per turn to prevent token bloat and runaway costs
        const val MAX_OUTPUT_TOKENS = 350

        val SYSTEM_INSTRUCTION = """
            You are the "DreamCare AI Concierge", an expert, warm, and attentive customer service specialist for "Good Dream Home Decor Private Limited" (tagline: "Comfort for a Better Tomorrow").
            
            Your role is to help users solve small and everyday problems quickly, including:
            1. Mattress selection & firmness guidance (e.g. orthopedic firm for back pain and spine alignment; medium-firm for couples; plush for side sleepers).
            2. Bed sizing & custom dimensions (King: 76"x80", Queen: 60"x80", Single/Twin: 38"x75", Indian standard 72"x72", 72"x78", and bespoke custom sizes).
            3. Mattress & upholstery care (stain removal using baking soda & mild detergent, rotating mattresses every 3-6 months, using waterproof breathable protectors).
            4. Good Dream customer policies: 100-Night Risk-Free Sleep Trial, 10-Year Craftsmanship Warranty, and Free White-Glove doorstep delivery & assembly.
            5. Order tracking, service visits, and maintenance inquiries.
            
            Cost & Conciseness Guidelines:
            - Keep responses concise, direct, and under 120 words.
            - Use short bullet points when outlining steps.
            - Never invent external brands; recommend Good Dream collections (SpringHaven Luxe, OrthoRest, Velvet Serenity, EcoSleep).
        """.trimIndent()
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a multi-turn chat message to Gemini and returns the model response.
     * Prunes history to recent turns and enforces strict output token limits for cost efficiency.
     */
    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        model: String = MODEL_FLASH_LITE
    ): String = withContext(Dispatchers.IO) {
        val rawApiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val isKeyConfigured = rawApiKey.isNotBlank() &&
                rawApiKey != "MY_GEMINI_API_KEY" &&
                !rawApiKey.startsWith("your_")

        if (isKeyConfigured) {
            try {
                val url = "$BASE_URL$model:generateContent"

                val jsonBody = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", SYSTEM_INSTRUCTION)
                            })
                        })
                    })

                    val contentsArray = JSONArray()
                    // Cost optimization: Prune conversation history to recent turns to avoid token bloat
                    val prunedHistory = if (history.size > MAX_HISTORY_MESSAGES) {
                        history.takeLast(MAX_HISTORY_MESSAGES)
                    } else {
                        history
                    }

                    for (msg in prunedHistory) {
                        contentsArray.put(JSONObject().apply {
                            put("role", if (msg.role == MessageRole.USER) "user" else "model")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", msg.text)
                                })
                            })
                        })
                    }

                    // Append current user message
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userMessage)
                            })
                        })
                    })

                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("topP", 0.85)
                        put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", rawApiKey)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string().orEmpty()
                    if (response.isSuccessful && responseStr.isNotBlank()) {
                        val root = JSONObject(responseStr)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val replyText = parts.getJSONObject(0).optString("text")
                                if (replyText.isNotBlank()) {
                                    return@withContext replyText.trim()
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Gemini API error code ${response.code}: $responseStr")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed, falling back to local concierge knowledge", e)
            }
        }

        // Intelligent local concierge responder for small problems when API key is unavailable or offline
        return@withContext generateLocalConciergeFallback(userMessage)
    }

    /**
     * Provides structured, highly accurate help for common small problems
     * regarding sleep, furniture, mattress sizing, cleaning, warranty, and trial periods.
     */
    private fun generateLocalConciergeFallback(query: String): String {
        val lower = query.lowercase().trim()

        return when {
            lower.contains("back") || lower.contains("pain") || lower.contains("spine") || lower.contains("ortho") -> {
                """
                **DreamCare Sleep Guidance for Back Comfort:**

                For back discomfort or spinal alignment, our sleep orthopedists recommend:
                - **Firmness**: Medium-Firm to Firm (7–8/10 on the firmness scale). Excessively soft mattresses cause the pelvic region to sink, hyperextending lumbar joints.
                - **Recommended Collection**: **SpringHaven OrthoRest Hybrid** — features zoned pocket coils that provide reinforced lower-back support while cushioning shoulders.
                - **Quick Posture Tip**:
                  • *Back Sleepers*: Place a slim pillow under knees to relieve lower spine pressure.
                  • *Side Sleepers*: Place a pillow between your knees to keep hips aligned.
                """.trimIndent()
            }

            lower.contains("size") || lower.contains("dimension") || lower.contains("king") || lower.contains("queen") || lower.contains("measure") -> {
                """
                **Good Dream Mattress Sizing Guide:**

                Here are the standard dimensions for our bedframes and mattresses:
                - **King Size**: 76" x 80" (193 x 203 cm) — Ultimate luxury for couples and co-sleeping with pets or kids.
                - **Queen Size**: 60" x 80" (152 x 203 cm) — Most popular choice for master bedrooms.
                - **Single / Twin**: 38" x 75" (96 x 190 cm) — Ideal for guest rooms or studio lofts.
                - **Indian Standard**: 72" x 72" or 72" x 78" also supported.

                **Need a custom fit?** Good Dream crafts custom sized mattresses with millimeter precision! Tap **Custom Size Inquiry** or submit a request in the app.
                """.trimIndent()
            }

            lower.contains("clean") || lower.contains("stain") || lower.contains("wash") || lower.contains("odor") || lower.contains("spill") -> {
                """
                **Care & Stain Removal Guide:**

                To protect your mattress warranty and fabric integrity:
                1. **Immediate Spills**: Blot immediately with clean microfiber cloths — *never rub or scrub*, as this drives moisture into the latex/foam layers.
                2. **Gentle Spot Cleaner**: Mix 1 cup warm water with 1 tbsp mild eco-detergent and 1 tsp white vinegar. Lightly mist and dab with a cloth.
                3. **Deodorizing & Moisture**: Sprinkle dry baking soda generously across the surface, let sit for 45 minutes, then vacuum thoroughly with an upholstery attachment.
                4. **Protection**: Always use a **Good Dream Waterproof Breathable Mattress Protector** to preserve your 10-year warranty.
                """.trimIndent()
            }

            lower.contains("trial") || lower.contains("100") || lower.contains("night") || lower.contains("return") -> {
                """
                **Good Dream 100-Night Risk-Free Sleep Trial:**

                - **Break-in Period**: Your body needs approximately 21 to 30 nights to adjust to proper orthopedic spinal support.
                - **Full Refund Guarantee**: If you aren't completely in love within 100 nights, contact us for free pickup and a 100% refund.
                - **Zero Hassle**: Pickups are scheduled at your convenience; returned mattresses in clean condition are sanitized and donated to local charities.
                """.trimIndent()
            }

            lower.contains("warranty") || lower.contains("guarantee") || lower.contains("years") -> {
                """
                **10-Year Craftsmanship Warranty Coverage:**

                Good Dream provides a comprehensive 10-Year Limited Warranty:
                - **Covered**: Visible sagging or indentations greater than 1.5 inches, broken coils, physical flaws causing foam to degrade or split under normal domestic usage.
                - **Not Covered**: Natural softening of materials over time, burns, stains, or using improper non-supportive bed bases.
                - **To file a claim**: Go to **Service Warranties** in the menu drawer or tap **Repairs & Service** in your Account tab.
                """.trimIndent()
            }

            lower.contains("shipping") || lower.contains("delivery") || lower.contains("track") || lower.contains("arrive") -> {
                """
                **Delivery & Installation Support:**

                - **White-Glove Delivery**: Complimentary for all mattress orders. Our certified logistics team handles doorstep delivery, unboxing, room-of-choice placement, and packaging disposal.
                - **Standard Delivery Time**: 2 to 4 business days for standard sizes; 5 to 7 business days for custom bespoke dimensions.
                - **Tracking**: Check your status in **Order Tracking** or call our concierge line at **+91 80 4123 9999**.
                """.trimIndent()
            }

            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                """
                Hello and welcome to **Good Dream Home Decor**! 🌙

                I am your **DreamCare AI Concierge**. How may I assist you with your home or sleep comfort today?
                - Sizing or custom mattress dimensions
                - Selecting the right firmness for back support
                - Cleaning & maintenance tips
                - 100-Night Sleep Trial & 10-Year Warranty questions
                - Order tracking & delivery details
                """.trimIndent()
            }

            else -> {
                """
                **DreamCare Concierge Advice:**

                Thank you for your question regarding *"$query"*. 

                At Good Dream Home Decor, our mission is "Comfort for a Better Tomorrow". Here are a few ways we can help:
                - **Need personalized guidance?** Our sleep advisors can assist you directly via chat or phone at **+91 80 4123 9999**.
                - **Explore our Collections**: Browse the **SpringHaven Luxe** line under the Products tab to see detailed firmness specs, zoned support, and breathable cooling covers.
                - **Custom Tailoring**: We accommodate bespoke bed heights and non-standard dimensions.
                
                Feel free to ask more specific questions about sizing, firmness, cleaning, or warranties!
                """.trimIndent()
            }
        }
    }
}
