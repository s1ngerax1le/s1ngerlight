package com.onyx.decoled.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ColorPad(
    color: Int,
    onColorChange: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentOnColorChange by rememberUpdatedState(onColorChange)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val hueColors = remember {
        listOf(
            Color(0xFFFF0000), // Red
            Color(0xFFFFFF00), // Yellow
            Color(0xFF00FF00), // Green
            Color(0xFF00FFFF), // Cyan
            Color(0xFF0000FF), // Blue
            Color(0xFFFF00FF), // Magenta
            Color(0xFFFF0000)  // Red
        )
    }

    val overlayColors = remember {
        listOf(
            Color.White,
            Color.Transparent,
            Color.Black
        )
    }

    fun coordToColor(x: Float, y: Float, w: Float, h: Float): Int {
        if (w <= 0 || h <= 0) return color
        val clampedX = x.coerceIn(0f, w)
        val clampedY = y.coerceIn(0f, h)

        val hue = (clampedX / w) * 360f
        val halfH = h * 0.5f

        val sat: Float
        val value: Float
        if (clampedY <= halfH) {
            sat = clampedY / halfH
            value = 1f
        } else {
            sat = 1f
            value = max(0.01f, 1f - (clampedY - halfH) / halfH)
        }

        val hsv = floatArrayOf(
            hue.coerceIn(0f, 360f),
            sat.coerceIn(0f, 1f),
            value.coerceIn(0.01f, 1f)
        )
        return android.graphics.Color.HSVToColor(hsv)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.38f)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .onSizeChanged { canvasSize = it }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        if (w > 0 && h > 0) {
                            val newColor = coordToColor(offset.x, offset.y, w, h)
                            currentOnColorChange(newColor)
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        if (w > 0 && h > 0) {
                            val newColor = coordToColor(change.position.x, change.position.y, w, h)
                            currentOnColorChange(newColor)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Horizontal Hue gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = hueColors,
                    startX = 0f,
                    endX = w
                )
            )

            // 2. Vertical Overlay: White -> Transparent (0.5) -> Black
            drawRect(
                brush = Brush.verticalGradient(
                    colors = overlayColors,
                    startY = 0f,
                    endY = h
                )
            )

            // Calculate Thumb coordinate from current color
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            val thumbX = (hsv[0] / 360f) * w
            val halfH = h * 0.5f
            val thumbY = if (hsv[2] >= 0.999f) {
                hsv[1] * halfH
            } else {
                (1f - hsv[2]) * halfH + halfH
            }

            val thumbCenter = Offset(
                x = thumbX.coerceIn(12.dp.toPx(), w - 12.dp.toPx()),
                y = thumbY.coerceIn(12.dp.toPx(), h - 12.dp.toPx())
            )

            // Outer thumb ring: 12dp radius, 3dp white stroke
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner thumb circle: 9dp radius, current color fill
            drawCircle(
                color = Color(color),
                radius = 9.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}
