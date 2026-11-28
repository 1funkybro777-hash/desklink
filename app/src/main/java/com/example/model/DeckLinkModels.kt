package com.example.model

enum class MirrorQuality(val label: String, val scale: Float, val jpegQuality: Int) {
    FAST("Fast (Low Bandwidth)", 0.38f, 52),
    BALANCED("Balanced (HD)", 0.52f, 68),
    CRISP("Crisp (High Detail)", 0.68f, 80)
}

data class RemoteCapabilities(
    val screenMirror: Boolean = true,
    val audioForwarding: Boolean = true,
    val remoteTouchAndInput: Boolean = true,
    val audioAndMedia: Boolean = true,
    val clipboardSync: Boolean = true,
    val appLauncher: Boolean = true,
    val fileVault: Boolean = true,
    val hardwareActions: Boolean = true,
    val notificationBridge: Boolean = true
)

data class ServerSettings(
    val port: Int = 8765,
    val mirrorQuality: MirrorQuality = MirrorQuality.BALANCED,
    val mirrorFps: Int = 10,
    val keepAwakeWhileHosting: Boolean = true,
    val capabilities: RemoteCapabilities = RemoteCapabilities()
)

data class LaptopSession(
    val sessionId: String,
    val token: String,
    val streamTicket: String,
    val clientIp: String,
    val clientLabel: String,
    val connectedAtMs: Long,
    val lastActiveAtMs: Long
)

enum class LogStatus {
    ALLOWED,
    BLOCKED,
    AUTH_FAILED,
    SYSTEM
}

data class CommandLogEntry(
    val id: String,
    val timestampMs: Long,
    val clientIp: String,
    val action: String,
    val detail: String,
    val status: LogStatus
)

data class VolumeLevels(
    val media: Int = 0,
    val mediaMax: Int = 15,
    val ring: Int = 0,
    val ringMax: Int = 7,
    val notification: Int = 0,
    val notificationMax: Int = 7,
    val alarm: Int = 0,
    val alarmMax: Int = 7,
    val ringerMode: String = "normal"
)

data class DeviceTelemetry(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val ipAddress: String = "127.0.0.1",
    val networkType: String = "Offline",
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 0f,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 1L,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 1f,
    val screenWidthPx: Int = 1080,
    val screenHeightPx: Int = 2400,
    val uptimeMinutes: Long = 0L,
    val volumes: VolumeLevels = VolumeLevels()
)

data class VaultFileItem(
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val mimeType: String,
    val relativePath: String = name,
    val isDirectory: Boolean = false
)

data class LaunchableApp(
    val label: String,
    val packageName: String
)

data class BridgedNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAtMs: Long,
    val isClearable: Boolean
)

data class ClipboardEntry(
    val id: String,
    val text: String,
    val source: String,
    val timestampMs: Long
)

data class UiNodeSummary(
    val index: Int,
    val label: String,
    val className: String,
    val clickable: Boolean,
    val editable: Boolean,
    val centerX: Float,
    val centerY: Float
)

data class AutomationMacro(
    val id: String,
    val title: String,
    val subtitle: String,
    val stepsSummary: String
)

data class ServiceReadiness(
    val serverRunning: Boolean = false,
    val serverError: String? = null,
    val screenMirrorActive: Boolean = false,
    val audioForwardingActive: Boolean = false,
    val audioForwardingError: String? = null,
    val mirrorRotationDegrees: Int = 0,
    val accessibilityConnected: Boolean = false,
    val notificationListenerConnected: Boolean = false,
    val torchEnabled: Boolean = false,
    val finderRingActive: Boolean = false
)
