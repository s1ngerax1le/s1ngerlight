package com.onyx.decoled.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.onyx.decoled.LedController

sealed interface HeroState {
    data object Checking : HeroState
    data object NoRoot : HeroState
    data class Off(val shield: Boolean, val bootSync: Boolean) : HeroState
    data class On(val effect: Int, val colorHex: String, val brightnessPct: Int, val shield: Boolean) : HeroState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroStatusCard(
    state: HeroState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val containerColorTarget = when (state) {
        HeroState.Checking -> MaterialTheme.colorScheme.secondaryContainer
        HeroState.NoRoot -> MaterialTheme.colorScheme.errorContainer
        is HeroState.Off -> MaterialTheme.colorScheme.secondaryContainer
        is HeroState.On -> MaterialTheme.colorScheme.primaryContainer
    }

    val onContainerColor = when (state) {
        HeroState.Checking -> MaterialTheme.colorScheme.onSecondaryContainer
        HeroState.NoRoot -> MaterialTheme.colorScheme.onErrorContainer
        is HeroState.Off -> MaterialTheme.colorScheme.onSecondaryContainer
        is HeroState.On -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val containerColor by animateColorAsState(
        targetValue = containerColorTarget,
        animationSpec = MotionScheme.expressive().slowEffectsSpec(),
        label = "heroContainerColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "heroRotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cookieRotation"
    )

    Surface(
        onClick = {
            when (state) {
                HeroState.Checking -> {}
                HeroState.NoRoot -> {
                    S1ngerHaptics.confirm(haptic)
                    onClick()
                }
                is HeroState.Off -> {
                    S1ngerHaptics.toggleOn(haptic)
                    onClick()
                }
                is HeroState.On -> {
                    S1ngerHaptics.toggleOff(haptic)
                    onClick()
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 60dp Cookie9Sided Badge
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cookieShape = MaterialShapes.Cookie9Sided.toShape()
                    // Rotating background layer when On
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                if (state is HeroState.On) {
                                    rotationZ = rotationAngle
                                }
                            }
                            .clip(cookieShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    // Non-rotating icon layer
                    when (state) {
                        HeroState.Checking -> {
                            LoadingIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        HeroState.NoRoot -> {
                            Icon(
                                imageVector = Icons.Filled.GppBad,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        is HeroState.Off -> {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        is HeroState.On -> {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val title = when (state) {
                        HeroState.Checking -> "正在检查权限"
                        HeroState.NoRoot -> "未获取 Root 权限"
                        is HeroState.Off -> "氛围灯已关闭"
                        is HeroState.On -> "氛围灯运行中"
                    }

                    val effectName = when (state) {
                        is HeroState.On -> when (state.effect) {
                            LedController.EFFECT_BREATH -> "呼吸"
                            LedController.EFFECT_BLINK -> "闪烁"
                            LedController.EFFECT_STROBE -> "律动"
                            else -> "常亮"
                        }
                        else -> ""
                    }

                    val subtitle = when (state) {
                        HeroState.Checking -> "申请 Root…"
                        HeroState.NoRoot -> "点击重新申请"
                        is HeroState.Off -> "Deco 光环 · aw21024"
                        is HeroState.On -> "Deco 光环 · $effectName · ${state.colorHex}"
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = onContainerColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainerColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capsule row
            val capsules = when (state) {
                HeroState.Checking -> listOf("v2.0.0")
                HeroState.NoRoot -> listOf("v2.0.0", "无 Root")
                is HeroState.Off -> listOf(
                    "v2.0.0",
                    if (state.shield) "Shield 已锁定" else "Shield 未锁定",
                    if (state.bootSync) "开机保持" else "开机不保持"
                )
                is HeroState.On -> listOf(
                    "v2.0.0",
                    "亮度 ${state.brightnessPct}%",
                    if (state.shield) "Shield 已锁定" else "Shield 未锁定"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                capsules.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.55f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = null
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainerColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
