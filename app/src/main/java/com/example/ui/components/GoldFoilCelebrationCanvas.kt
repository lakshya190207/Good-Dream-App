package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

/**
 * Geometric foil particle shapes for the Good Dream luxury celebration engine.
 */
enum class FoilShape {
    RIBBON,     // Tumbling metallic ribbon streamer
    SEQUIN,     // Polished gold coin / sequin disc
    DIAMOND,    // Faceted 4-point diamond shard
    STAR        // Brilliant 4-pointed sparkle star
}

/**
 * Celebration emission archetype.
 */
enum class CelebrationBurstMode {
    CENTER_EXPLOSION,   // Erupts radially outward from the authenticity seal
    DUAL_CANNONS,       // Twin gold cannons firing upward from bottom screen corners
    CELESTIAL_RAIN      // Gentle cascading gold leaf shower from upper sanctuary
}

/**
 * Curated metallic luxury color palette blending 24K gold, champagne, pearl white,
 * and Good Dream signature emerald.
 */
val LUXURY_GOLD_FOIL_COLORS = listOf(
    Color(0xFFFFD700), // 24K Pure Gold
    Color(0xFFE5A93C), // Satin Gold Accent
    Color(0xFFD4AF37), // Metallic Champagne Gold
    Color(0xFFF5D061), // Radiant Gold
    Color(0xFFFFF2B2), // Shimmering Gold Dust
    Color(0xFFFFF8DC), // Pearl Diamond White
    Color(0xFF2D6A4F), // Good Dream Forest Emerald
    Color(0xFF52B788), // Mint Glow
    Color(0xFFC5A059), // Antique Brass
    Color(0xFFB8860B)  // Burnished Bronze Gold
)

/**
 * Individual physics particle simulating air resistance, gravity, 3D tumbling,
 * flutter sway, and specular reflection highlights.
 */
data class GoldFoilParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var rotX: Float = 0f,
    var rotY: Float = 0f,
    var rotZ: Float = 0f,
    var vRotX: Float = 0f,
    var vRotY: Float = 0f,
    var vRotZ: Float = 0f,
    var width: Float = 14f,
    var height: Float = 22f,
    var shape: FoilShape = FoilShape.RIBBON,
    var color: Color = Color(0xFFD4AF37),
    var life: Float = 1f,
    var maxLifeSeconds: Float = 3.8f,
    var ageSeconds: Float = 0f,
    var isAlive: Boolean = true,
    var swayPhase: Float = 0f,
    var swayFrequency: Float = 3.5f,
    var swayAmplitude: Float = 25f,
    var drag: Float = 0.022f,
    var gravity: Float = 750f
) {
    fun update(dt: Float) {
        if (!isAlive) return

        ageSeconds += dt
        if (ageSeconds >= maxLifeSeconds) {
            isAlive = false
            life = 0f
            return
        }

        // Remaining life factor (smooth fade-out in final 25% of lifespan)
        val progress = ageSeconds / maxLifeSeconds
        life = if (progress > 0.75f) {
            ((1f - progress) / 0.25f).coerceIn(0f, 1f)
        } else {
            1f
        }

        // Air resistance drag
        vx *= (1f - (drag * 60f * dt).coerceIn(0f, 0.5f))
        vy = (vy * (1f - (drag * 60f * dt).coerceIn(0f, 0.5f))) + (gravity * dt)

        // Atmospheric fluttering sway
        val sway = sin(ageSeconds * swayFrequency + swayPhase) * swayAmplitude

        x += (vx + sway) * dt
        y += vy * dt

        // 3D Tumbling rotations around 3 axes
        rotX += vRotX * dt
        rotY += vRotY * dt
        rotZ += vRotZ * dt
    }
}

/**
 * Factory helper generating physics-modeled particle bursts.
 */
object GoldFoilPhysicsEngine {

    fun spawnBurst(
        canvasWidth: Float,
        canvasHeight: Float,
        particleCount: Int = 90,
        mode: CelebrationBurstMode = CelebrationBurstMode.CENTER_EXPLOSION,
        customOrigin: Offset? = null
    ): List<GoldFoilParticle> {
        val safeW = if (canvasWidth <= 0f) 1080f else canvasWidth
        val safeH = if (canvasHeight <= 0f) 1920f else canvasHeight
        val particles = ArrayList<GoldFoilParticle>(particleCount)

        for (i in 0 until particleCount) {
            val shape = when (Random.nextInt(10)) {
                in 0..4 -> FoilShape.RIBBON
                in 5..7 -> FoilShape.SEQUIN
                8 -> FoilShape.DIAMOND
                else -> FoilShape.STAR
            }

            val color = LUXURY_GOLD_FOIL_COLORS[Random.nextInt(LUXURY_GOLD_FOIL_COLORS.size)]
            val sizeFactor = Random.nextFloat() * 0.7f + 0.65f // 0.65x to 1.35x
            val w = when (shape) {
                FoilShape.RIBBON -> 12f * sizeFactor
                FoilShape.SEQUIN -> 16f * sizeFactor
                FoilShape.DIAMOND -> 18f * sizeFactor
                FoilShape.STAR -> 20f * sizeFactor
            }
            val h = when (shape) {
                FoilShape.RIBBON -> (Random.nextFloat() * 16f + 16f) * sizeFactor // 16 to 32
                else -> w
            }

            val maxLife = Random.nextFloat() * 1.5f + 3.0f // 3.0 to 4.5 seconds

            val p = GoldFoilParticle(
                width = w,
                height = h,
                shape = shape,
                color = color,
                maxLifeSeconds = maxLife,
                vRotX = (Random.nextFloat() - 0.5f) * 14f,
                vRotY = (Random.nextFloat() - 0.5f) * 16f,
                vRotZ = (Random.nextFloat() - 0.5f) * 10f,
                swayPhase = Random.nextFloat() * (2 * PI).toFloat(),
                swayFrequency = Random.nextFloat() * 2.5f + 2.0f,
                swayAmplitude = Random.nextFloat() * 30f + 15f,
                gravity = Random.nextFloat() * 250f + 650f
            )

            when (mode) {
                CelebrationBurstMode.CENTER_EXPLOSION -> {
                    val origin = customOrigin ?: Offset(safeW * 0.5f, safeH * 0.30f)
                    p.x = origin.x + (Random.nextFloat() - 0.5f) * 40f
                    p.y = origin.y + (Random.nextFloat() - 0.5f) * 40f

                    val angle = Random.nextFloat() * (2 * PI).toFloat()
                    val speed = Random.nextFloat() * 750f + 250f
                    p.vx = cos(angle) * speed
                    p.vy = (sin(angle) * speed) - 280f // Upward explosive pop
                }
                CelebrationBurstMode.DUAL_CANNONS -> {
                    val isLeft = i % 2 == 0
                    p.x = if (isLeft) safeW * 0.05f else safeW * 0.95f
                    p.y = safeH * 0.95f

                    val baseAngle = if (isLeft) (-PI / 4).toFloat() else (-3 * PI / 4).toFloat()
                    val angleSpread = (Random.nextFloat() - 0.5f) * 0.45f
                    val angle = baseAngle + angleSpread
                    val speed = Random.nextFloat() * 850f + 400f
                    p.vx = cos(angle) * speed
                    p.vy = sin(angle) * speed
                }
                CelebrationBurstMode.CELESTIAL_RAIN -> {
                    p.x = Random.nextFloat() * safeW
                    p.y = -Random.nextFloat() * safeH * 0.3f
                    p.vx = (Random.nextFloat() - 0.5f) * 80f
                    p.vy = Random.nextFloat() * 120f + 80f
                }
            }

            particles.add(p)
        }

        return particles
    }
}

/**
 * Hardware-accelerated Compose Canvas that simulates a celebratory gold foil confetti shower
 * with 3D tumble projection, metallic luster specular glares, and real aerodynamic drag.
 * Automatically halts when animation completes for 0% idle CPU/GPU consumption.
 */
@Composable
fun GoldFoilCelebrationCanvas(
    trigger: Int,
    modifier: Modifier = Modifier,
    particleCount: Int = 90,
    burstMode: CelebrationBurstMode = CelebrationBurstMode.CENTER_EXPLOSION,
    customOrigin: Offset? = null,
    onCelebrationFinished: () -> Unit = {}
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val particles = remember { mutableStateListOf<GoldFoilParticle>() }
    var isSimulating by remember { mutableStateOf(false) }

    // Spawn burst whenever trigger increment changes
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            val safeW = if (canvasSize.width > 0f) canvasSize.width else 1080f
            val safeH = if (canvasSize.height > 0f) canvasSize.height else 1920f
            val freshBurst = GoldFoilPhysicsEngine.spawnBurst(
                canvasWidth = safeW,
                canvasHeight = safeH,
                particleCount = particleCount,
                mode = burstMode,
                customOrigin = customOrigin
            )
            particles.clear()
            particles.addAll(freshBurst)
            isSimulating = true
        }
    }

    // 60-120Hz VSync simulation loop using withFrameNanos
    LaunchedEffect(isSimulating) {
        if (!isSimulating) return@LaunchedEffect

        var lastFrameTimeNanos = 0L

        while (isActive && isSimulating) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)

                    var anyAlive = false
                    for (i in 0 until particles.size) {
                        val p = particles[i]
                        if (p.isAlive) {
                            p.update(dt)
                            anyAlive = true
                        }
                    }

                    if (!anyAlive) {
                        isSimulating = false
                        onCelebrationFinished()
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    // Shared path to avoid GC allocations during high-frequency draw loops
    val sharedPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("gold_foil_celebration_canvas")
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }

        if (!isSimulating || particles.isEmpty()) return@Canvas

        for (i in 0 until particles.size) {
            val p = particles[i]
            if (!p.isAlive) continue

            drawFoilParticle(p, sharedPath)
        }
    }
}

/**
 * Draws an individual foil particle with 3D projection, specular glare, and opacity fading.
 */
private fun DrawScope.drawFoilParticle(p: GoldFoilParticle, path: Path) {
    // 3D rotation projection factors
    val scaleX = abs(cos(p.rotY)).coerceAtLeast(0.06f)
    val scaleY = abs(cos(p.rotX)).coerceAtLeast(0.06f)

    // Edge-on specular reflection flash: when face turns perpendicular to light, flash pure bright gold
    val isSpecularFlash = scaleX < 0.22f || scaleY < 0.22f
    val renderColor = if (isSpecularFlash) {
        Color.White.copy(alpha = p.life * 0.95f)
    } else {
        p.color.copy(alpha = (p.life * 0.92f).coerceIn(0f, 1f))
    }

    val rotationDegrees = (p.rotZ * 180f / PI).toFloat()

    rotate(degrees = rotationDegrees, pivot = Offset(p.x, p.y)) {
        scale(scaleX = scaleX, scaleY = scaleY, pivot = Offset(p.x, p.y)) {
            when (p.shape) {
                FoilShape.RIBBON -> {
                    drawRect(
                        color = renderColor,
                        topLeft = Offset(p.x - p.width / 2f, p.y - p.height / 2f),
                        size = Size(p.width, p.height)
                    )
                }
                FoilShape.SEQUIN -> {
                    drawCircle(
                        color = renderColor,
                        radius = p.width / 2f,
                        center = Offset(p.x, p.y)
                    )
                }
                FoilShape.DIAMOND -> {
                    path.reset()
                    val halfW = p.width / 2f
                    val halfH = p.height / 2f
                    path.moveTo(p.x, p.y - halfH)
                    path.lineTo(p.x + halfW, p.y)
                    path.lineTo(p.x, p.y + halfH)
                    path.lineTo(p.x - halfW, p.y)
                    path.close()
                    drawPath(path = path, color = renderColor)
                }
                FoilShape.STAR -> {
                    path.reset()
                    val r = p.width / 2f
                    val inner = r * 0.28f
                    path.moveTo(p.x, p.y - r)
                    path.lineTo(p.x + inner, p.y - inner)
                    path.lineTo(p.x + r, p.y)
                    path.lineTo(p.x + inner, p.y + inner)
                    path.lineTo(p.x, p.y + r)
                    path.lineTo(p.x - inner, p.y + inner)
                    path.lineTo(p.x - r, p.y)
                    path.lineTo(p.x - inner, p.y - inner)
                    path.close()
                    drawPath(path = path, color = renderColor)
                }
            }
        }
    }
}
