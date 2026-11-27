package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.control.DeviceTelemetryProvider
import com.example.control.SystemControlManager
import com.example.data.DeckLinkStateStore
import com.example.data.SettingsRepository
import com.example.data.VaultFileManager
import com.example.model.AutomationMacro
import com.example.model.ClipboardEntry
import com.example.model.CommandLogEntry
import com.example.model.DeviceTelemetry
import com.example.model.LaptopSession
import com.example.model.LogStatus
import com.example.model.MirrorQuality
import com.example.model.RemoteCapabilities
import com.example.model.ServerSettings
import com.example.model.ServiceReadiness
import com.example.model.VaultFileItem
import com.example.service.DeckLinkAccessibilityService
import com.example.service.DeckLinkHostService
import com.example.service.DeckLinkNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DeckTab {
    DECK,
    CONTROLS,
    ACTIVITY,
    SETTINGS
}

data class DeckLinkUiState(
    val pairingPin: String = "",
    val settings: ServerSettings = ServerSettings(),
    val readiness: ServiceReadiness = ServiceReadiness(),
    val sessions: List<LaptopSession> = emptyList(),
    val logs: List<CommandLogEntry> = emptyList(),
    val clipboardHistory: List<ClipboardEntry> = emptyList(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    val vaultFiles: List<VaultFileItem> = emptyList(),
    val macros: List<AutomationMacro> = DeckLinkStateStore.builtInMacros
) {
    val serverUrl: String
        get() = "http://${telemetry.ipAddress}:${settings.port}"

    val adbForwardCommand: String
        get() = "adb forward tcp:${settings.port} tcp:${settings.port}"
}

class DeckLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepo = SettingsRepository(appContext)
    private val telemetryProvider = DeviceTelemetryProvider(appContext)
    private val vaultFileManager = VaultFileManager(appContext)
    private val localControl = SystemControlManager(appContext)

    private val _telemetry = MutableStateFlow(telemetryProvider.collect())
    private val _vaultFiles = MutableStateFlow<List<VaultFileItem>>(emptyList())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _selectedTab = MutableStateFlow(DeckTab.DECK)
    val selectedTab: StateFlow<DeckTab> = _selectedTab.asStateFlow()

    val uiState: StateFlow<DeckLinkUiState> = combine(
        DeckLinkStateStore.pairingPin,
        DeckLinkStateStore.settings,
        DeckLinkStateStore.readiness,
        DeckLinkStateStore.sessions,
        DeckLinkStateStore.commandLogs,
        DeckLinkStateStore.clipboardHistory,
        _telemetry,
        _vaultFiles
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DeckLinkUiState(
            pairingPin = values[0] as String,
            settings = values[1] as ServerSettings,
            readiness = values[2] as ServiceReadiness,
            sessions = values[3] as List<LaptopSession>,
            logs = values[4] as List<CommandLogEntry>,
            clipboardHistory = values[5] as List<ClipboardEntry>,
            telemetry = values[6] as DeviceTelemetry,
            vaultFiles = values[7] as List<VaultFileItem>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckLinkUiState(
            pairingPin = DeckLinkStateStore.pairingPin.value,
            settings = DeckLinkStateStore.settings.value,
            readiness = DeckLinkStateStore.readiness.value,
            telemetry = _telemetry.value
        )
    )

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { loaded ->
                DeckLinkStateStore.updateSettings(loaded)
            }
        }
        refreshVaultFiles()
        startTelemetryTicker()
    }

    override fun onCleared() {
        localControl.release()
        super.onCleared()
    }

    fun selectTab(tab: DeckTab) {
        _selectedTab.value = tab
        if (tab == DeckTab.CONTROLS) {
            refreshVaultFiles()
        }
    }

    fun refreshReadinessAndTelemetry() {
        DeckLinkStateStore.updateReadiness {
            it.copy(
                accessibilityConnected = DeckLinkAccessibilityService.isConnected(),
                notificationListenerConnected = DeckLinkNotificationListener.isConnected()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            _telemetry.value = telemetryProvider.collect()
            _vaultFiles.value = vaultFileManager.listFiles()
        }
    }

    private fun startTelemetryTicker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                _telemetry.value = telemetryProvider.collect()
                DeckLinkStateStore.updateReadiness {
                    it.copy(
                        accessibilityConnected = DeckLinkAccessibilityService.isConnected(),
                        notificationListenerConnected = DeckLinkNotificationListener.isConnected()
                    )
                }
                delay(3_000L)
            }
        }
    }

    fun startServer() {
        val port = DeckLinkStateStore.settings.value.port
        val intent = DeckLinkHostService.startServerIntent(appContext, port)
        ContextCompat.startForegroundService(appContext, intent)
        emitToast("Starting DeckLink Host on port $port")
    }

    fun stopServer() {
        val intent = DeckLinkHostService.stopServerIntent(appContext)
        appContext.startService(intent)
        emitToast("Remote Host stopped")
    }

    fun onMediaProjectionGranted(resultCode: Int, data: Intent) {
        val intent = DeckLinkHostService.startMirrorIntent(appContext, resultCode, data)
        ContextCompat.startForegroundService(appContext, intent)
        emitToast("Live screen mirror active")
    }

    fun stopScreenMirror() {
        val intent = DeckLinkHostService.stopMirrorIntent(appContext)
        appContext.startService(intent)
        emitToast("Screen mirror stopped")
    }

    fun rotatePairingPin() {
        DeckLinkStateStore.rotatePairingPin()
        emitToast("Generated new 6-digit pairing PIN")
    }

    fun revokeSession(sessionId: String) {
        DeckLinkStateStore.revokeSession(sessionId)
        emitToast("Laptop session disconnected")
    }

    fun revokeAllSessions() {
        DeckLinkStateStore.revokeAllSessions()
        emitToast("All connected laptops disconnected")
    }

    fun clearLogs() {
        DeckLinkStateStore.clearLogs()
        emitToast("Activity log cleared")
    }

    fun copyToClipboard(label: String, text: String) {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        emitToast("Copied $label")
    }

    fun updateCapability(transform: (RemoteCapabilities) -> RemoteCapabilities) {
        DeckLinkStateStore.updateCapabilities(transform)
        val updated = DeckLinkStateStore.settings.value
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveSettings(updated)
        }
    }

    fun updatePort(rawPort: String) {
        val parsed = rawPort.trim().toIntOrNull()
        if (parsed == null || parsed !in 1024..65535) {
            emitToast("Enter a valid port between 1024 and 65535")
            return
        }
        val wasRunning = DeckLinkStateStore.readiness.value.serverRunning
        val updated = DeckLinkStateStore.settings.value.copy(port = parsed)
        DeckLinkStateStore.updateSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveSettings(updated)
        }
        if (wasRunning) {
            val intent = DeckLinkHostService.startServerIntent(appContext, parsed)
            ContextCompat.startForegroundService(appContext, intent)
            emitToast("Host restarted on port $parsed")
        } else {
            emitToast("Port updated to $parsed")
        }
    }

    fun updateMirrorQuality(quality: MirrorQuality) {
        val updated = DeckLinkStateStore.settings.value.copy(mirrorQuality = quality)
        DeckLinkStateStore.updateSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveSettings(updated)
        }
    }

    fun updateMirrorFps(fps: Int) {
        val updated = DeckLinkStateStore.settings.value.copy(mirrorFps = fps.coerceIn(3, 20))
        DeckLinkStateStore.updateSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveSettings(updated)
        }
    }

    fun updateKeepAwake(keepAwake: Boolean) {
        val updated = DeckLinkStateStore.settings.value.copy(keepAwakeWhileHosting = keepAwake)
        DeckLinkStateStore.updateSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveSettings(updated)
        }
    }

    fun runMacroLocally(macroId: String) {
        val (ok, msg) = localControl.runMacro(macroId)
        DeckLinkStateStore.logCommand("local", "Macro", msg, if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
        emitToast(msg)
        refreshReadinessAndTelemetry()
    }

    fun toggleTorchLocally() {
        val target = !DeckLinkStateStore.readiness.value.torchEnabled
        val ok = localControl.setTorchEnabled(target)
        if (!ok) {
            emitToast("Flashlight unavailable on this device")
        }
    }

    fun stopFinderRingLocally() {
        localControl.stopFinderRing()
        emitToast("Finder alert silenced")
    }

    fun importVaultFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = vaultFileManager.importFromUri(uri)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val saved = result.getOrThrow()
                    DeckLinkStateStore.logCommand("local", "Vault File Added", saved.name, LogStatus.SYSTEM)
                    emitToast("Added ${saved.name} to Vault")
                    refreshVaultFiles()
                } else {
                    emitToast(result.exceptionOrNull()?.message ?: "Could not import file")
                }
            }
        }
    }

    fun deleteVaultFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val removed = vaultFileManager.deleteFile(name)
            withContext(Dispatchers.Main) {
                if (removed) {
                    emitToast("Removed $name")
                    refreshVaultFiles()
                }
            }
        }
    }

    fun buildShareVaultIntent(name: String): Intent? {
        return vaultFileManager.createShareIntent(name)
    }

    fun refreshVaultFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _vaultFiles.value = vaultFileManager.listFiles()
        }
    }

    fun buildAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildNotificationAccessSettingsIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        } else {
            Settings.ACTION_SETTINGS
        }
        return Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun emitToast(message: String) {
        _snackbarEvents.tryEmit(message)
    }
}
