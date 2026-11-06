package com.onyx.decoled

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.decoled.ui.components.HeroState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

sealed interface RootState {
    data object Checking : RootState
    data object Granted : RootState
    data object Denied : RootState
}

sealed interface NodeState {
    data object Unknown : NodeState
    data object Writable : NodeState
    data object ReadOnly : NodeState
    data object Missing : NodeState
}

data class LedUiState(
    val root: RootState = RootState.Checking,
    val node: NodeState = NodeState.Unknown,
    val masterEnabled: Boolean = false,
    val brightness: Int = 180,
    val color: Int = 0xFF00E5FF.toInt(),
    val effect: Int = LedController.EFFECT_STATIC,
    val speed: Int = 1500,
    val shield: Boolean = true,
    val bootSync: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = LedController(application)
    private val ioMutex = Mutex()
    private var debounceApplyJob: Job? = null

    // In-memory backup of settings prior to activating pure white boost mode: (color, brightness, effect)
    private var preWhiteBackup: Triple<Int, Int, Int>? = null

    private val _uiState = MutableStateFlow(
        LedUiState(
            masterEnabled = controller.isMasterEnabled,
            brightness = controller.brightness,
            color = controller.color,
            effect = controller.effect,
            speed = controller.speed,
            shield = controller.isShieldMode,
            bootSync = controller.isBootSync
        )
    )
    val uiState: StateFlow<LedUiState> = _uiState.asStateFlow()

    val heroState: StateFlow<HeroState> = _uiState.map { state ->
        when (state.root) {
            RootState.Checking -> HeroState.Checking
            RootState.Denied -> HeroState.NoRoot
            RootState.Granted -> {
                if (!state.masterEnabled) {
                    HeroState.Off(shield = state.shield, bootSync = state.bootSync)
                } else {
                    val colorHex = String.format("#%06X", 0xFFFFFF and state.color)
                    val brightnessPct = (state.brightness * 100) / 255
                    HeroState.On(
                        effect = state.effect,
                        colorHex = colorHex,
                        brightnessPct = brightnessPct,
                        shield = state.shield
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HeroState.Checking)

    init {
        checkRoot()
    }

    fun checkRoot() {
        _uiState.update { it.copy(root = RootState.Checking) }
        viewModelScope.launch {
            val isRoot = withContext(Dispatchers.IO) {
                RootShell.isRootAvailable()
            }
            val rootState = if (isRoot) RootState.Granted else RootState.Denied
            _uiState.update { it.copy(root = rootState) }
            if (isRoot) {
                probeNode()
            }
        }
    }

    fun probeNode() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val cmd = "test -d ${LedController.SYSFS_LED} && (test -w ${LedController.SYSFS_LED}/brightness && echo rw || echo ro) || echo none"
                    val process = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(process.outputStream)
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    os.writeBytes(cmd + "\n")
                    os.writeBytes("exit\n")
                    os.flush()
                    val line = reader.readLine()
                    process.waitFor()
                    line?.trim() ?: "unknown"
                } catch (_: Exception) {
                    "unknown"
                }
            }
            val nodeState = when (result) {
                "rw" -> NodeState.Writable
                "ro" -> NodeState.ReadOnly
                "none" -> NodeState.Missing
                else -> NodeState.Unknown
            }
            _uiState.update { it.copy(node = nodeState) }
        }
    }

    fun reloadFromPrefs() {
        _uiState.update {
            it.copy(
                masterEnabled = controller.isMasterEnabled,
                brightness = controller.brightness,
                color = controller.color,
                effect = controller.effect,
                speed = controller.speed,
                shield = controller.isShieldMode,
                bootSync = controller.isBootSync
            )
        }
    }

    private fun debounceApply() {
        debounceApplyJob?.cancel()
        debounceApplyJob = viewModelScope.launch {
            delay(90)
            applyToHardware()
        }
    }

    private fun immediateApply() {
        debounceApplyJob?.cancel()
        viewModelScope.launch {
            applyToHardware()
        }
    }

    private suspend fun applyToHardware() {
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                controller.applyState()
            }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _uiState.update { it.copy(masterEnabled = enabled) }
        controller.isMasterEnabled = enabled
        immediateApply()
    }

    fun onBrightnessChange(brightness: Int) {
        val clamped = brightness.coerceIn(0, 255)
        _uiState.update { it.copy(brightness = clamped) }
        controller.brightness = clamped
        debounceApply()
    }

    fun setPresetBrightness(brightness: Int) {
        val clamped = brightness.coerceIn(0, 255)
        _uiState.update { it.copy(brightness = clamped) }
        controller.brightness = clamped
        immediateApply()
    }

    fun onColorChange(color: Int) {
        _uiState.update { it.copy(color = color) }
        controller.color = color
        debounceApply()
    }

    fun setPresetColor(color: Int) {
        _uiState.update { it.copy(color = color) }
        controller.color = color
        immediateApply()
    }

    fun setEffect(effect: Int) {
        _uiState.update { it.copy(effect = effect) }
        controller.effect = effect
        immediateApply()
    }

    fun onSpeedChange(speed: Int) {
        val clamped = speed.coerceIn(300, 3000)
        _uiState.update { it.copy(speed = clamped) }
        controller.speed = clamped
        debounceApply()
    }

    fun setShieldMode(shield: Boolean) {
        _uiState.update { it.copy(shield = shield) }
        controller.isShieldMode = shield
        immediateApply()
        probeNode()
    }

    fun setBootSync(bootSync: Boolean) {
        _uiState.update { it.copy(bootSync = bootSync) }
        controller.isBootSync = bootSync
        immediateApply()
    }

    fun isWhiteBoostActive(state: LedUiState): Boolean {
        return state.masterEnabled &&
                (state.color and 0xFFFFFF) == 0xFFFFFF &&
                state.brightness == 255 &&
                state.effect == LedController.EFFECT_STATIC
    }

    fun toggleWhiteBoost(active: Boolean) {
        if (active) {
            // Backup current state
            val current = _uiState.value
            preWhiteBackup = Triple(current.color, current.brightness, current.effect)
            _uiState.update {
                it.copy(
                    masterEnabled = true,
                    color = 0xFFFFFFFF.toInt(),
                    brightness = 255,
                    effect = LedController.EFFECT_STATIC
                )
            }
            controller.isMasterEnabled = true
            controller.color = 0xFFFFFFFF.toInt()
            controller.brightness = 255
            controller.effect = LedController.EFFECT_STATIC
            immediateApply()
        } else {
            // Restore previous state or defaults
            val (prevColor, prevBrightness, prevEffect) = preWhiteBackup ?: Triple(
                0xFF00E5FF.toInt(),
                180,
                LedController.EFFECT_STATIC
            )
            preWhiteBackup = null
            _uiState.update {
                it.copy(
                    color = prevColor,
                    brightness = prevBrightness,
                    effect = prevEffect
                )
            }
            controller.color = prevColor
            controller.brightness = prevBrightness
            controller.effect = prevEffect
            immediateApply()
        }
    }

    fun fullBlackout() {
        debounceApplyJob?.cancel()
        preWhiteBackup = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ioMutex.withLock {
                    controller.fullBlackout()
                }
            }
            reloadFromPrefs()
            probeNode()
        }
    }
}
