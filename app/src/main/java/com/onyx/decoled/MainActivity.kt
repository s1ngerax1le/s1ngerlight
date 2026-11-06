package com.onyx.decoled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onyx.decoled.ui.screens.AppScaffold
import com.onyx.decoled.ui.theme.S1ngerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.reloadFromPrefs()
            }
            S1ngerTheme {
                AppScaffold(viewModel = viewModel)
            }
        }
    }
}
