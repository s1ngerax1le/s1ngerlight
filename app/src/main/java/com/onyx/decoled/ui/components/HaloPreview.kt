package com.onyx.decoled.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.onyx.decoled.LedController
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

@Composable
fun HaloPreview(
    enabled: Boolean,
    color: Color,
    brightness: Int,
    effect: Int,
    speedMs: Int,
    modifier: Modifier = Modifier
) {
    val base = max(0.3f, brightness / 255f)
    val safeSpeed = max(300, speedMs)

    val transition = rememberInfiniteTransition(label = "haloAnim")

    // Breath animation phase (0f..1f over safeSpeed ms)
    val breathProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = safeSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloBreath"
    )

    // Blink animation phase (0f..1f over safeSpeed ms)
    val blinkProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = safeSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloBlink"
    )

    // Strobe animation phase (0f..1f over 120 ms)
    val strobeProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloStrobe"
    )

    val currentAlpha = when {
        !enabled -> 0.4f
        effect == LedController.EFFECT_BREATH -> {
            val factor = ((1.0 - cos(breathProgress * 2.0 * PI)) / 2.0).toFloat()
            base * (0.15f + 0.85f * factor)
        }
        effect == LedController.EFFECT_BLINK -> {
            if (blinkProgress < 0.5f) base else 0f
        }
        effect == LedController.EFFECT_STROBE -> {
            if (strobeProgress < 0.5f) base else 0f
        }
        else -> base // STATIC
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Canvas Box (144.dp x 144.dp dedicated for camera module & halo ring)
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val outerGlowRadius = 72.dp.toPx()
                val decoBaseRadius = 58.dp.toPx()
                val innerGlowRadius = 56.dp.toPx()
                val ringRadius = 44.dp.toPx()
                val strokeWidth = 10.dp.toPx()
                val lensRadius = 31.dp.toPx()
                val highlightRadius = 8.dp.toPx()

                // 1. External ambient soft glow outside the Deco module (only when active)
                if (enabled && currentAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = (0.28f * currentAlpha).coerceIn(0f, 1f)),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = outerGlowRadius
                        ),
                        radius = outerGlowRadius,
                        center = centerOffset
                    )
                }

                // 2. Dark Camera Deco Baseplate (provides full contrast for white LED on white cards)
                drawCircle(
                    color = Color(0xFF16181D),
                    radius = decoBaseRadius,
                    center = centerOffset
                )
                // Outer subtle metallic bezel ring
                drawCircle(
                    color = outlineVariant.copy(alpha = 0.45f),
                    radius = decoBaseRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 3. Inner ambient glow wash over the dark baseplate (only when active)
                if (enabled && currentAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = (0.45f * currentAlpha).coerceIn(0f, 1f)),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = innerGlowRadius
                        ),
                        radius = innerGlowRadius,
                        center = centerOffset
                    )
                }

                // 4. Unlit diffuser dark track (always present)
                drawCircle(
                    color = Color(0xFF282C34),
                    radius = ringRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // 5. Active LED Halo Ring (drawn on top of the dark track)
                if (enabled && currentAlpha > 0.01f) {
                    drawCircle(
                        color = color.copy(alpha = currentAlpha.coerceIn(0f, 1f)),
                        radius = ringRadius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )
                }

                // 6. Central Camera Glass Lens
                drawCircle(
                    color = Color(0xFF0B0C0E),
                    radius = lensRadius,
                    center = centerOffset
                )
                // Lens optical coating ring
                drawCircle(
                    color = Color(0xFF232730),
                    radius = lensRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Lens reflection highlight (top-left offset 10dp)
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = highlightRadius,
                    center = Offset(
                        x = centerOffset.x - 10.dp.toPx(),
                        y = centerOffset.y - 10.dp.toPx()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status text capsule physically separated from canvas
        val statusText = when {
            !enabled -> "已熄灭"
            effect == LedController.EFFECT_BREATH -> "呼吸 · $speedMs ms"
            effect == LedController.EFFECT_BLINK -> "闪烁 · $speedMs ms"
            effect == LedController.EFFECT_STROBE -> "律动 · 60 ms"
            else -> "常亮 · ${brightness * 100 / 255}%"
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
