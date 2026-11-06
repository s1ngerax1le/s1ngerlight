package com.onyx.decoled.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalIsDark = compositionLocalOf { false }

object Dimensions {
    val PageHPadding = 16.dp
    val SegmentGap = 2.dp
    val SegmentOuter = 20.dp
    val SegmentInner = 4.dp
    val ListBottomPadding = 24.dp
}

val ColorScheme.pageBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainer

val ColorScheme.itemSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) surfaceContainerLow else surfaceContainerLowest

val ColorScheme.itemSurfacePressed: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainerHigh
