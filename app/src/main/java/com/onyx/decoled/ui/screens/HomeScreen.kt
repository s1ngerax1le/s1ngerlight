package com.onyx.decoled.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onyx.decoled.LedController
import com.onyx.decoled.MainViewModel
import com.onyx.decoled.RootState
import com.onyx.decoled.ui.components.Choice
import com.onyx.decoled.ui.components.ChoiceItem
import com.onyx.decoled.ui.components.ColorBadge
import com.onyx.decoled.ui.components.ColorPad
import com.onyx.decoled.ui.components.ColorSwatchRow
import com.onyx.decoled.ui.components.HaloPreview
import com.onyx.decoled.ui.components.HeroState
import com.onyx.decoled.ui.components.HeroStatusCard
import com.onyx.decoled.ui.components.ListRow
import com.onyx.decoled.ui.components.S1ngerHaptics
import com.onyx.decoled.ui.components.SectionHeader
import com.onyx.decoled.ui.components.SegmentedGroup
import com.onyx.decoled.ui.components.SegmentedItem
import com.onyx.decoled.ui.components.SliderItem
import com.onyx.decoled.ui.components.SwitchItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    outerPadding: PaddingValues,
    listState: LazyListState,
    topBarState: TopAppBarState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val heroState by viewModel.heroState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val isWhiteActive = viewModel.isWhiteBoostActive(uiState)
    val hasRoot = uiState.root == RootState.Granted
    val isControlsEnabled = uiState.masterEnabled && hasRoot

    val effectOptions = listOf(
        Choice(LedController.EFFECT_STATIC, "常亮", "固定亮度，可启用 Shield 锁定"),
        Choice(LedController.EFFECT_BREATH, "呼吸", "亮度按周期平滑起伏"),
        Choice(LedController.EFFECT_BLINK, "闪烁", "按周期亮灭交替"),
        Choice(LedController.EFFECT_STROBE, "律动", "60 ms 高频亮灭")
    )

    val showSpeedSlider = uiState.effect == LedController.EFFECT_BREATH || uiState.effect == LedController.EFFECT_BLINK
    val effectItemCount = if (showSpeedSlider) 2 else 1

    var showEffectDialog by rememberSaveable { mutableStateOf(false) }

    PageScaffold(
        title = "s1ngerlight",
        outerPadding = outerPadding,
        listState = listState,
        topBarState = topBarState
    ) {
        // Hero Card
        item(key = "hero_card") {
            HeroStatusCard(
                state = heroState,
                onClick = {
                    if (heroState is HeroState.NoRoot) {
                        viewModel.checkRoot()
                    } else {
                        viewModel.setMasterEnabled(!uiState.masterEnabled)
                    }
                }
            )
        }

        item(key = "hero_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: 光环
        item(key = "section_halo") {
            SectionHeader(text = "光环")
        }

        item(key = "group_halo") {
            SegmentedGroup {
                // Item 0: HaloPreview (non-clickable)
                SegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = null,
                    enabled = hasRoot
                ) {
                    HaloPreview(
                        enabled = isControlsEnabled,
                        color = Color(uiState.color),
                        brightness = uiState.brightness,
                        effect = uiState.effect,
                        speedMs = uiState.speed
                    )
                }

                // Item 1: Master Switch
                SegmentedItem(
                    index = 1,
                    count = 2,
                    onClick = {
                        if (hasRoot) {
                            val target = !uiState.masterEnabled
                            if (target) S1ngerHaptics.toggleOn(haptic) else S1ngerHaptics.toggleOff(haptic)
                            viewModel.setMasterEnabled(target)
                        }
                    },
                    enabled = hasRoot
                ) {
                    SwitchItem(
                        icon = Icons.Outlined.Lightbulb,
                        title = "Deco 氛围灯",
                        supporting = "控制摄像头模组旁光环",
                        checked = uiState.masterEnabled,
                        onCheckedChange = { viewModel.setMasterEnabled(it) },
                        enabled = hasRoot
                    )
                }
            }
        }

        // Section: 快捷场景
        item(key = "section_scene") {
            SectionHeader(text = "快捷场景")
        }

        item(key = "group_scene") {
            SegmentedGroup {
                SegmentedItem(
                    index = 0,
                    count = 1,
                    onClick = {
                        if (hasRoot) {
                            val target = !isWhiteActive
                            if (target) S1ngerHaptics.toggleOn(haptic) else S1ngerHaptics.toggleOff(haptic)
                            viewModel.toggleWhiteBoost(target)
                        }
                    },
                    enabled = hasRoot
                ) {
                    SwitchItem(
                        icon = Icons.Outlined.WbSunny,
                        title = "纯白补光",
                        supporting = "白色 · 100% 亮度 · 常亮",
                        checked = isWhiteActive,
                        onCheckedChange = { viewModel.toggleWhiteBoost(it) },
                        enabled = hasRoot
                    )
                }
            }
        }

        // Section: 亮度
        item(key = "section_brightness") {
            SectionHeader(text = "亮度")
        }

        item(key = "group_brightness") {
            SegmentedGroup {
                // Item 0: Brightness Slider
                SegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = null,
                    enabled = isControlsEnabled
                ) {
                    SliderItem(
                        icon = Icons.Outlined.BrightnessMedium,
                        title = "亮度",
                        valueLabel = "${(uiState.brightness * 100) / 255}%",
                        value = uiState.brightness.toFloat(),
                        range = 0f..255f,
                        steps = 0,
                        onValueChange = { viewModel.onBrightnessChange(it.toInt()) },
                        enabled = isControlsEnabled
                    )
                }

                // Item 1: Connected Preset Buttons (25%, 50%, 75%, 100%)
                SegmentedItem(
                    index = 1,
                    count = 2,
                    onClick = null,
                    enabled = isControlsEnabled
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .alpha(if (isControlsEnabled) 1f else 0.38f),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(
                            Pair("25%", 64),
                            Pair("50%", 128),
                            Pair("75%", 192),
                            Pair("100%", 255)
                        )
                        val leadingShapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        val middleShapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                        val trailingShapes = ButtonGroupDefaults.connectedTrailingButtonShapes()

                        presets.forEachIndexed { i, (label, value) ->
                            val shape = when (i) {
                                0 -> leadingShapes
                                presets.size - 1 -> trailingShapes
                                else -> middleShapes
                            }
                            val isChecked = uiState.brightness == value
                            ToggleButton(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isControlsEnabled) {
                                        S1ngerHaptics.segmentTick(haptic)
                                        viewModel.setPresetBrightness(value)
                                    }
                                },
                                shapes = shape,
                                enabled = isControlsEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        // Section: 颜色
        item(key = "section_color") {
            SectionHeader(text = "颜色")
        }

        item(key = "group_color") {
            SegmentedGroup {
                // Item 0: Current color row
                SegmentedItem(
                    index = 0,
                    count = 3,
                    onClick = null,
                    enabled = isControlsEnabled
                ) {
                    ListRow(
                        title = "当前颜色",
                        supporting = String.format("#%06X", 0xFFFFFF and uiState.color),
                        leading = { ColorBadge(Color(uiState.color)) },
                        enabled = isControlsEnabled
                    )
                }

                // Item 1: ColorPad
                SegmentedItem(
                    index = 1,
                    count = 3,
                    onClick = null,
                    enabled = isControlsEnabled
                ) {
                    ColorPad(
                        color = uiState.color,
                        onColorChange = { viewModel.onColorChange(it) },
                        enabled = isControlsEnabled
                    )
                }

                // Item 2: ColorSwatchRow
                SegmentedItem(
                    index = 2,
                    count = 3,
                    onClick = null,
                    enabled = isControlsEnabled
                ) {
                    ColorSwatchRow(
                        selectedColor = uiState.color,
                        onColorSelect = { viewModel.setPresetColor(it) },
                        enabled = isControlsEnabled
                    )
                }
            }
        }

        // Section: 灯效
        item(key = "section_effect") {
            SectionHeader(text = "灯效")
        }

        item(key = "group_effect") {
            SegmentedGroup {
                // Item 0: ChoiceItem
                SegmentedItem(
                    index = 0,
                    count = effectItemCount,
                    onClick = if (isControlsEnabled) {
                        {
                            S1ngerHaptics.segmentTick(haptic)
                            showEffectDialog = true
                        }
                    } else null,
                    enabled = isControlsEnabled
                ) {
                    ChoiceItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "灯效模式",
                        options = effectOptions,
                        selected = uiState.effect,
                        onSelect = { viewModel.setEffect(it) },
                        enabled = isControlsEnabled,
                        showDialog = showEffectDialog,
                        onDismiss = { showEffectDialog = false }
                    )
                }

                // Item 1: Speed Slider (animated visibility)
                AnimatedVisibility(
                    visible = showSpeedSlider,
                    enter = expandVertically(MotionScheme.expressive().fastSpatialSpec()) + fadeIn(),
                    exit = shrinkVertically(MotionScheme.expressive().fastSpatialSpec()) + fadeOut()
                ) {
                    SegmentedItem(
                        index = 1,
                        count = 2,
                        onClick = null,
                        enabled = isControlsEnabled
                    ) {
                        SliderItem(
                            icon = Icons.Outlined.Timer,
                            title = "动效周期",
                            valueLabel = "${uiState.speed} ms",
                            value = uiState.speed.toFloat(),
                            range = 300f..3000f,
                            steps = 26,
                            onValueChange = { viewModel.onSpeedChange(it.toInt()) },
                            enabled = isControlsEnabled
                        )
                    }
                }
            }
        }
    }
}
