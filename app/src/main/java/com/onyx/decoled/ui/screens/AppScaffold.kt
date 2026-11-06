package com.onyx.decoled.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onyx.decoled.MainViewModel
import com.onyx.decoled.ui.components.S1ngerHaptics
import com.onyx.decoled.ui.theme.pageBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val settingsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val homeTopBarState = rememberTopAppBarState()
    val settingsTopBarState = rememberTopAppBarState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.pageBackground,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.pageBackground,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        if (selectedTab != 0) {
                            S1ngerHaptics.segmentTick(haptic)
                            selectedTab = 0
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = "灯光"
                        )
                    },
                    label = { Text("灯光") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        if (selectedTab != 1) {
                            S1ngerHaptics.segmentTick(haptic)
                            selectedTab = 1
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "设置"
                        )
                    },
                    label = { Text("设置") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    ) { outerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(durationMillis = 90)))
            },
            label = "tabTransition"
        ) { targetTab ->
            when (targetTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    outerPadding = outerPadding,
                    listState = homeListState,
                    topBarState = homeTopBarState
                )
                1 -> SettingsScreen(
                    viewModel = viewModel,
                    outerPadding = outerPadding,
                    listState = settingsListState,
                    topBarState = settingsTopBarState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(
    title: String,
    outerPadding: PaddingValues,
    listState: LazyListState,
    topBarState: TopAppBarState,
    content: LazyListScope.() -> Unit
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.pageBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.pageBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.pageBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = outerPadding.calculateBottomPadding() + 24.dp
            ),
            content = content
        )
    }
}
