package com.onyx.decoled.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorSwatchRow(
    selectedColor: Int,
    onColorSelect: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val swatches = remember {
        listOf(
            0xFF00E5FF.toInt(), // Cyan
            0xFFD500F9.toInt(), // Purple
            0xFFFF1744.toInt(), // Red
            0xFF00E676.toInt(), // Green
            0xFFFF6D00.toInt(), // Orange
            0xFFFFD600.toInt(), // Yellow
            0xFF2979FF.toInt(), // Blue
            0xFFFFFFFF.toInt()  // White
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.38f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { hex ->
            val isSelected = (selectedColor and 0xFFFFFF) == (hex and 0xFFFFFF)
            val composeColor = Color(hex)
            val lum = 0.299f * composeColor.red + 0.587f * composeColor.green + 0.114f * composeColor.blue

            val sizeAnimation by animateDpAsState(
                targetValue = if (isSelected) 40.dp else 36.dp,
                animationSpec = MotionScheme.expressive().fastSpatialSpec<Dp>(),
                label = "swatchSize"
            )

            val shape = if (isSelected) {
                MaterialShapes.Cookie9Sided.toShape()
            } else {
                CircleShape
            }

            val isWhiteOrBright = lum > 0.85f

            var swatchModifier = Modifier
                .size(sizeAnimation)
                .clip(shape)

            if (isWhiteOrBright) {
                swatchModifier = swatchModifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
            }

            Box(
                modifier = swatchModifier
                    .background(composeColor)
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        S1ngerHaptics.segmentTick(haptic)
                        onColorSelect(hex)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val checkTint = if (lum > 0.5f) Color.Black else Color.White
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
