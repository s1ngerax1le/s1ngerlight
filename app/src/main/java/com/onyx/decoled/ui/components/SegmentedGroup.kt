package com.onyx.decoled.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onyx.decoled.ui.theme.Dimensions
import com.onyx.decoled.ui.theme.itemSurface
import com.onyx.decoled.ui.theme.itemSurfacePressed

@Composable
fun SegmentedGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SegmentGap),
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedItem(
    index: Int,
    count: Int,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val isInteractive = onClick != null && enabled
    val isPressedNow = pressed && isInteractive

    val spec = MotionScheme.expressive().fastSpatialSpec<Dp>()
    val isTopRounded = isPressedNow || index == 0
    val isBottomRounded = isPressedNow || index == count - 1

    val top by animateDpAsState(
        targetValue = if (isTopRounded) Dimensions.SegmentOuter else Dimensions.SegmentInner,
        animationSpec = spec,
        label = "segmentTopCorner"
    )
    val bottom by animateDpAsState(
        targetValue = if (isBottomRounded) Dimensions.SegmentOuter else Dimensions.SegmentInner,
        animationSpec = spec,
        label = "segmentBottomCorner"
    )

    val bg by animateColorAsState(
        targetValue = if (isPressedNow) MaterialTheme.colorScheme.itemSurfacePressed else MaterialTheme.colorScheme.itemSurface,
        animationSpec = MotionScheme.expressive().fastEffectsSpec(),
        label = "segmentBgColor"
    )

    val shape = RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomStart = bottom,
        bottomEnd = bottom
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = bg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null,
            interactionSource = interaction,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = bg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}
