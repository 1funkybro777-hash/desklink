package com.example.data

import com.example.model.AutomationMacro
import com.example.model.BridgedNotification
import com.example.model.ClipboardEntry
import com.example.model.CommandLogEntry
import com.example.model.LaptopSession
import com.example.model.LogStatus
import com.example.model.RemoteCapabilities
import com.example.model.ServerSettings
import com.example.model.ServiceReadiness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue

/**
 * Process-wide reactive coordinator shared between DeckLinkHostService, DeckLinkHttpServer,
 * DeckLinkAccessibilityService, DeckLinkNotificationListener, and DeckLinkViewModel.
 */
object DeckLinkStateStore {

    private val secureRandom = SecureRandom()

    private val _pairingPin = MutableStateFlow(generateSixDigitPin())
    val pairingPin: StateFlow<String> = _pairingPin.asStateFlow()

    private val _settings = MutableStateFlow(ServerSettings())
    val settings: StateFlow<ServerSettings> = _settings.asStateFlow()

    private val _readiness = MutableStateFlow(ServiceReadiness())
    val readiness: StateFlow<ServiceReadiness> = _readiness.asStateFlow()

    private val _sessions = MutableStateFlow<List<LaptopSession>>(emptyList())
    val sessions: StateFlow<List<LaptopSession>> = _sessions.asStateFlow()

    private val _commandLogs = MutableStateFlow<List<CommandLogEntry>>(emptyList())
    val commandLogs: StateFlow<List<CommandLogEntry>> = _commandLogs.asStateFlow()

    private val _notifications = MutableStateFlow<List<BridgedNotification>>(emptyList())
    val notifications: StateFlow<List<BridgedNotification>> = _notifications.asStateFlow()

    private val _clipboardHistory = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val clipboardHistory: StateFlow<List<ClipboardEntry>> = _clipboardHistory.asStateFlow()

    @Volatile
    var latestScreenJpeg: ByteArray? = null

    @Volatile
    var latestFrameTimestampMs: Long = 0L

    // Real-time PCM audio fan-out queues for connected laptop audio stream listeners
    private val audioSubscribers = CopyOnWriteArrayList<LinkedBlockingQueue<ByteArray>>()

    fun registerAudioSubscriber(): LinkedBlockingQueue<ByteArray> {
        val queue = LinkedBlockingQueue<ByteArray>(32)
        audioSubscribers.add(queue)
        return queue
    }

    fun unregisterAudioSubscriber(queue: LinkedBlockingQueue<ByteArray>) {
        audioSubscribers.remove(queue)
        queue.clear()
    }

    fun broadcastAudioPcm(chunk: ByteArray) {
        if (audioSubscribers.isEmpty()) return
        for (queue in audioSubscribers) {
            if (!queue.offer(chunk)) {
                queue.poll()
                queue.offer(chunk)
            }
        }
    }

    // Per-IP failed PIN tracking for brute-force lockout protection
    private data class FailureRecord(val attempts: Int, val lockedUntilMs: Long)
    private val failedAttempts = ConcurrentHashMap<String, FailureRecord>()

    val builtInMacros: List<AutomationMacro> = listOf(
        AutomationMacro(
            id = "desk_mode",
            title = "Desk Mode",
            subtitle = "Quiet phone notifications while working on laptop",
            stepsSummary = "Vibrate ringer + Media 35% + Haptic confirmation"
        ),
        AutomationMacro(
            id = "focus_silent",
            title = "Deep Focus",
            subtitle = "Silence alerts and minimize audio distractions",
            stepsSummary = "Silent ringer + Media 0% + Stop finder alert"
        ),
        AutomationMacro(
            id = "media_stage",
            title = "Media Stage",
            subtitle = "Boost media playback for casting or speakers",
            stepsSummary = "Normal ringer + Media 80% + Play/Pause toggle"
        ),
        AutomationMacro(
            id = "locate_beacon",
            title = "Locate Beacon",
            subtitle = "Find your phone nearby with light and chime",
            stepsSummary = "Flashlight ON + Ring Alert + Vibrate"
        )
    )

    fun generateSixDigitPin(): String {
        val code = secureRandom.nextInt(900_000) + 100_000
        return code.toString()
    }

    fun rotatePairingPin(): String {
        val nextPin = generateSixDigitPin()
        _pairingPin.value = nextPin
        logCommand("local", "PIN Rotated", "New 6-digit pairing PIN issued", LogStatus.SYSTEM)
        return nextPin
    }

    fun updateSettings(newSettings: ServerSettings) {
        _settings.value = newSettings
    }

    fun updateCapabilities(transform: (RemoteCapabilities) -> RemoteCapabilities) {
        _settings.update { current -> current.copy(capabilities = transform(current.capabilities)) }
    }

    fun updateReadiness(transform: (ServiceReadiness) -> ServiceReadiness) {
        _readiness.update(transform)
    }

    fun setMirrorRotationDegrees(degrees: Int) {
        val normalized = ((degrees % 360) + 360) % 360
        val snapped = when (normalized) {
            90, 180, 270 -> normalized
            else -> 0
        }
        _readiness.update { it.copy(mirrorRotationDegrees = snapped) }
    }

    fun updateNotifications(list: List<BridgedNotification>) {
        _notifications.value = list
    }

    fun recordClipboard(text: String, source: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val entry = ClipboardEntry(
            id = UUID.randomUUID().toString(),
            text = trimmed.take(400),
            source = source,
            timestampMs = System.currentTimeMillis()
        )
        _clipboardHistory.update { existing ->
            (listOf(entry) + existing.filterNot { it.text == entry.text }).take(25)
        }
    }

    sealed class PairResult {
        data class Success(val session: LaptopSession) : PairResult()
        data class LockedOut(val retryAfterSeconds: Long) : PairResult()
        data object InvalidPin : PairResult()
    }

    fun attemptPair(submittedPin: String, clientIp: String, clientLabel: String): PairResult {
        val now = System.currentTimeMillis()
        val existingFailure = failedAttempts[clientIp]
        if (existingFailure != null && existingFailure.lockedUntilMs > now) {
            val remainingSec = ((existingFailure.lockedUntilMs - now) / 1000L).coerceAtLeast(1L)
            logCommand(clientIp, "Pair Blocked", "IP rate-limited (${remainingSec}s left)", LogStatus.AUTH_FAILED)
            return PairResult.LockedOut(remainingSec)
        }

        val expectedBytes = _pairingPin.value.toByteArray(Charsets.UTF_8)
        val providedBytes = submittedPin.trim().toByteArray(Charsets.UTF_8)
        val matched = MessageDigest.isEqual(expectedBytes, providedBytes)

        if (!matched) {
            val nextAttempts = (existingFailure?.attempts ?: 0) + 1
            val lockUntil = if (nextAttempts >= 5) now + 60_000L else 0L
            failedAttempts[clientIp] = FailureRecord(
                attempts = if (nextAttempts >= 5) 0 else nextAttempts,
                lockedUntilMs = lockUntil
            )
            logCommand(
                clientIp,
                "Invalid PIN",
                if (lockUntil > 0) "5 failed attempts — locked for 60s" else "Attempt $nextAttempts of 5",
                LogStatus.AUTH_FAILED
            )
            return PairResult.InvalidPin
        }

        failedAttempts.remove(clientIp)
        val session = LaptopSession(
            sessionId = UUID.randomUUID().toString().take(8),
            token = generateSecureToken(32),
            streamTicket = generateSecureToken(24),
            clientIp = clientIp,
            clientLabel = clientLabel.take(48).ifBlank { "Laptop Browser" },
            connectedAtMs = now,
            lastActiveAtMs = now
        )
        _sessions.update { list -> (listOf(session) + list).take(8) }
        logCommand(clientIp, "Laptop Paired", "${session.clientLabel} connected", LogStatus.SYSTEM)
        return PairResult.Success(session)
    }

    fun validateBearerToken(token: String?): LaptopSession? {
        if (token.isNullOrBlank()) return null
        val candidateBytes = token.toByteArray(Charsets.UTF_8)
        val found = _sessions.value.firstOrNull { session ->
            MessageDigest.isEqual(session.token.toByteArray(Charsets.UTF_8), candidateBytes)
        } ?: return null

        val now = System.currentTimeMillis()
        if (now - found.lastActiveAtMs > 5_000L) {
            _sessions.update { list ->
                list.map { if (it.sessionId == found.sessionId) it.copy(lastActiveAtMs = now) else it }
            }
        }
        return found
    }

    fun validateStreamTicket(ticket: String?): LaptopSession? {
        if (ticket.isNullOrBlank()) return null
        val candidateBytes = ticket.toByteArray(Charsets.UTF_8)
        return _sessions.value.firstOrNull { session ->
            MessageDigest.isEqual(session.streamTicket.toByteArray(Charsets.UTF_8), candidateBytes)
        }
    }

    fun revokeSession(sessionId: String) {
        val removed = _sessions.value.firstOrNull { it.sessionId == sessionId }
        _sessions.update { list -> list.filterNot { it.sessionId == sessionId } }
        if (removed != null) {
            logCommand(removed.clientIp, "Session Revoked", "${removed.clientLabel} disconnected", LogStatus.SYSTEM)
        }
    }

    fun revokeAllSessions() {
        val count = _sessions.value.size
        _sessions.value = emptyList()
        if (count > 0) {
            logCommand("local", "All Sessions Revoked", "Disconnected $count active laptop(s)", LogStatus.SYSTEM)
        }
    }

    fun logCommand(clientIp: String, action: String, detail: String, status: LogStatus) {
        val entry = CommandLogEntry(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            clientIp = clientIp,
            action = action,
            detail = detail.take(160),
            status = status
        )
        _commandLogs.update { list -> (listOf(entry) + list).take(120) }
    }

    fun clearLogs() {
        _commandLogs.value = emptyList()
    }

    private fun generateSecureToken(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
