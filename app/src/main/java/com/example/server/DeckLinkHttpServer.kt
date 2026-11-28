package com.example.server

import android.content.Context
import android.os.Build
import com.example.control.AudioCaptureManager
import com.example.control.DeviceTelemetryProvider
import com.example.control.SystemControlManager
import com.example.data.DeckLinkStateStore
import com.example.data.VaultFileManager
import com.example.model.LogStatus
import com.example.service.DeckLinkAccessibilityService
import com.example.service.DeckLinkNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class DeckLinkHttpServer(
    private val context: Context,
    private val telemetryProvider: DeviceTelemetryProvider,
    private val systemControl: SystemControlManager,
    private val vaultFileManager: VaultFileManager
) {
    companion object {
        private const val MAX_HEADER_BYTES = 16 * 1024
        private const val MAX_JSON_BODY_BYTES = 256 * 1024
        private const val STREAM_BOUNDARY = "decklinkframe"
        private const val IO_BUFFER_BYTES = 128 * 1024 // 128 KB fast transfer buffer
    }

    private var serverScope: CoroutineScope? = null
    private var acceptJob: Job? = null
    private var serverSocket: ServerSocket? = null

    @Volatile
    var boundPort: Int = 8765
        private set

    private val webConsoleBytes: ByteArray by lazy {
        context.assets.open("web_console.html").use { it.readBytes() }
    }

    fun start(port: Int): Result<Int> {
        stop()
        return runCatching {
            val safePort = port.coerceIn(1024, 65535)
            val socket = ServerSocket().apply {
                reuseAddress = true
                receiveBufferSize = 256 * 1024
                bind(InetSocketAddress("0.0.0.0", safePort))
            }
            serverSocket = socket
            boundPort = safePort
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            serverScope = scope

            acceptJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    scope.launch {
                        handleClient(client)
                    }
                }
            }
            safePort
        }
    }

    fun stop() {
        runCatching { acceptJob?.cancel() }
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        runCatching { serverScope?.cancel() }
        serverScope = null
    }

    private suspend fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        socket.use { sock ->
            runCatching {
                sock.tcpNoDelay = true
                sock.sendBufferSize = 256 * 1024
                sock.receiveBufferSize = 256 * 1024
                sock.soTimeout = 30_000
                val input = BufferedInputStream(sock.getInputStream(), IO_BUFFER_BYTES)
                val output = sock.getOutputStream()

                val header = parseHttpHeader(input) ?: return
                routeRequest(header, input, clientIp, sock, output)
            }
        }
    }

    private data class HttpHeader(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
        val contentLength: Long
    )

    private fun parseHttpHeader(input: BufferedInputStream): HttpHeader? {
        val headerBuf = ByteArrayOutputStream()
        var matched = 0
        while (headerBuf.size() < MAX_HEADER_BYTES) {
            val b = input.read()
            if (b == -1) return null
            headerBuf.write(b)
            matched = when {
                matched == 0 && b == '\r'.code -> 1
                matched == 1 && b == '\n'.code -> 2
                matched == 2 && b == '\r'.code -> 3
                matched == 3 && b == '\n'.code -> 4
                b == '\r'.code -> 1
                else -> 0
            }
            if (matched == 4) break
        }
        if (matched != 4) return null

        val headerText = headerBuf.toString(Charsets.ISO_8859_1.name())
        val lines = headerText.split("\r\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val requestLineParts = lines.first().split(" ")
        if (requestLineParts.size < 2) return null

        val method = requestLineParts[0].uppercase()
        val rawTarget = requestLineParts[1]
        val pathPart = rawTarget.substringBefore('?')
        val queryPart = rawTarget.substringAfter('?', "")
        val queryMap = parseQuery(queryPart)

        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val idx = lines[i].indexOf(':')
            if (idx > 0) {
                val key = lines[i].substring(0, idx).trim().lowercase()
                val value = lines[i].substring(idx + 1).trim()
                headers[key] = value
            }
        }

        val contentLength = headers["content-length"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        return HttpHeader(method, pathPart, queryMap, headers, contentLength)
    }

    private fun readSmallBody(input: BufferedInputStream, contentLength: Long): ByteArray? {
        if (contentLength > MAX_JSON_BODY_BYTES.toLong()) return null
        val len = contentLength.toInt()
        if (len == 0) return ByteArray(0)
        val body = ByteArray(len)
        var offset = 0
        while (offset < len) {
            val read = input.read(body, offset, len - offset)
            if (read <= 0) break
            offset += read
        }
        return body
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split('&')) {
            if (pair.isBlank()) continue
            val key = URLDecoder.decode(pair.substringBefore('='), "UTF-8")
            val value = URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
            map[key] = value
        }
        return map
    }

    private suspend fun routeRequest(
        req: HttpHeader,
        input: BufferedInputStream,
        clientIp: String,
        socket: Socket,
        out: OutputStream
    ) {
        when {
            req.method == "GET" && (req.path == "/" || req.path == "/index.html") -> {
                sendBytes(
                    out = out,
                    statusCode = 200,
                    contentType = "text/html; charset=utf-8",
                    bytes = webConsoleBytes,
                    extraHeaders = mapOf(
                        "Content-Security-Policy" to
                            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; frame-ancestors 'none';"
                    )
                )
                return
            }

            req.method == "GET" && req.path == "/api/ping" -> {
                val telemetry = telemetryProvider.collect()
                sendJson(
                    out,
                    200,
                    JSONObject()
                        .put("app", "DeckLink")
                        .put("deviceModel", telemetry.deviceModel)
                        .put("requiresPin", true)
                )
                return
            }

            req.method == "POST" && req.path == "/api/auth/pair" -> {
                val body = readSmallBody(input, req.contentLength)
                if (body == null) {
                    sendJson(out, 413, JSONObject().put("error", "Payload too large"))
                    return
                }
                val payload = parseJson(body)
                val pin = payload.optString("pin", "")
                val clientLabel = payload.optString("clientLabel", req.headers["user-agent"]?.take(42) ?: "Laptop Browser")
                when (val result = DeckLinkStateStore.attemptPair(pin, clientIp, clientLabel)) {
                    is DeckLinkStateStore.PairResult.Success -> {
                        sendJson(
                            out,
                            200,
                            JSONObject()
                                .put("ok", true)
                                .put("sessionId", result.session.sessionId)
                                .put("token", result.session.token)
                                .put("streamTicket", result.session.streamTicket)
                        )
                    }
                    is DeckLinkStateStore.PairResult.LockedOut -> {
                        sendJson(
                            out,
                            429,
                            JSONObject()
                                .put("error", "Too many failed PIN attempts. Retry in ${result.retryAfterSeconds}s.")
                                .put("retryAfterSeconds", result.retryAfterSeconds)
                        )
                    }
                    DeckLinkStateStore.PairResult.InvalidPin -> {
                        sendJson(out, 401, JSONObject().put("error", "Invalid 6-digit pairing PIN"))
                    }
                }
                return
            }
        }

        // Live Video Mirror Stream & Single Snapshot
        if (req.method == "GET" && (req.path == "/api/stream/mjpeg" || req.path == "/api/screen/frame")) {
            val session = validateBearerOrTicket(req)
            if (session == null) {
                sendJson(out, 401, JSONObject().put("error", "Unauthorized stream request"))
                return
            }
            if (!DeckLinkStateStore.settings.value.capabilities.screenMirror) {
                DeckLinkStateStore.logCommand(clientIp, "Screen Mirror", "Blocked by phone capability toggle", LogStatus.BLOCKED)
                sendJson(out, 403, JSONObject().put("error", "Screen mirroring is disabled on phone"))
                return
            }

            if (req.path == "/api/screen/frame") {
                val frame = DeckLinkStateStore.latestScreenJpeg
                if (frame == null) {
                    sendJson(out, 503, JSONObject().put("error", "Screen mirror is not active on phone yet"))
                } else {
                    sendBytes(out, 200, "image/jpeg", frame)
                }
                return
            }

            streamMjpeg(socket, out, session.streamTicket)
            return
        }

        // Live Internal Phone Audio Stream (16-bit PCM @ 24kHz mono over HTTP chunked stream)
        if (req.method == "GET" && req.path == "/api/stream/audio") {
            val session = validateBearerOrTicket(req)
            if (session == null) {
                sendJson(out, 401, JSONObject().put("error", "Unauthorized audio stream request"))
                return
            }
            if (!DeckLinkStateStore.settings.value.capabilities.audioForwarding) {
                DeckLinkStateStore.logCommand(clientIp, "Phone Audio Stream", "Blocked by phone toggle", LogStatus.BLOCKED)
                sendJson(out, 403, JSONObject().put("error", "Phone-to-laptop audio forwarding is disabled on phone"))
                return
            }
            if (!DeckLinkStateStore.readiness.value.audioForwardingActive) {
                sendJson(out, 503, JSONObject().put("error", "Phone audio capture is not active on phone yet. Tap 'Phone audio' on your phone."))
                return
            }
            DeckLinkStateStore.logCommand(clientIp, "Phone Audio Listen", "Laptop listening to phone sound", LogStatus.ALLOWED)
            streamAudioPcm(socket, out, session.streamTicket)
            return
        }

        // Unlimited-Size Fast Streaming File Download & Preview (Supports HTTP Range seeking)
        if (req.method == "GET" && req.path == "/api/files/download") {
            val session = validateBearerOrTicket(req)
            if (session == null) {
                sendJson(out, 401, JSONObject().put("error", "Unauthorized"))
                return
            }
            if (!DeckLinkStateStore.settings.value.capabilities.fileVault) {
                DeckLinkStateStore.logCommand(clientIp, "File Download", "Blocked by phone policy", LogStatus.BLOCKED)
                sendJson(out, 403, JSONObject().put("error", "File Vault is disabled on phone"))
                return
            }
            val relPath = req.query["path"]?.ifBlank { null } ?: req.query["name"].orEmpty()
            val file = vaultFileManager.resolveSafeEntry(relPath)
            if (file == null || !file.exists() || !file.isFile) {
                sendJson(out, 404, JSONObject().put("error", "File not found in vault"))
                return
            }
            val inline = req.query["inline"] == "1" || req.query["inline"] == "true"
            DeckLinkStateStore.logCommand(clientIp, if (inline) "File Preview" else "File Download", file.name, LogStatus.ALLOWED)
            streamFileDownload(socket, out, file, req.headers["range"], inline)
            return
        }

        // Unlimited-Size Fast Streaming File Upload (Zero-RAM 128KB direct socket -> disk)
        if (req.method == "POST" && req.path == "/api/files/upload") {
            val session = validateBearerOrTicket(req)
            if (session == null) {
                sendJson(out, 401, JSONObject().put("error", "Authentication required"))
                return
            }
            if (!DeckLinkStateStore.settings.value.capabilities.fileVault) {
                DeckLinkStateStore.logCommand(clientIp, "Upload Vault File", "Blocked by phone capability switch", LogStatus.BLOCKED)
                sendJson(out, 403, JSONObject().put("error", "File Vault is disabled in DeckLink on your phone"))
                return
            }
            socket.soTimeout = 60_000
            val dirPath = req.query["path"].orEmpty()
            val rawName = req.query["name"].orEmpty().ifBlank { "upload_${System.currentTimeMillis()}.bin" }
            val res = vaultFileManager.streamUpload(dirPath, rawName, req.contentLength, input)
            if (res.isSuccess) {
                val saved = res.getOrThrow()
                DeckLinkStateStore.logCommand(clientIp, "File Uploaded", "${saved.relativePath} (${saved.sizeBytes} B)", LogStatus.ALLOWED)
                sendJson(
                    out,
                    200,
                    JSONObject()
                        .put("ok", true)
                        .put("name", saved.name)
                        .put("relativePath", saved.relativePath)
                        .put("sizeBytes", saved.sizeBytes)
                )
            } else {
                sendJson(out, 400, JSONObject().put("error", res.exceptionOrNull()?.message ?: "Upload failed"))
            }
            return
        }

        // All remaining /api/* endpoints strictly require Authorization: Bearer <token>
        val bearerToken = extractBearer(req.headers)
        val session = DeckLinkStateStore.validateBearerToken(bearerToken)
        if (session == null) {
            sendJson(out, 401, JSONObject().put("error", "Authentication required"))
            return
        }

        val bodyBytes = readSmallBody(input, req.contentLength)
        if (bodyBytes == null) {
            sendJson(out, 413, JSONObject().put("error", "JSON payload exceeds limit"))
            return
        }

        val caps = DeckLinkStateStore.settings.value.capabilities

        when {
            req.method == "POST" && req.path == "/api/auth/logout" -> {
                DeckLinkStateStore.revokeSession(session.sessionId)
                sendJson(out, 200, JSONObject().put("ok", true))
            }

            req.method == "GET" && req.path == "/api/status" -> {
                val telemetry = telemetryProvider.collect()
                val readiness = DeckLinkStateStore.readiness.value
                val json = JSONObject()
                    .put("deviceModel", telemetry.deviceModel)
                    .put("androidVersion", telemetry.androidVersion)
                    .put("ipAddress", telemetry.ipAddress)
                    .put("networkType", telemetry.networkType)
                    .put("batteryPercent", telemetry.batteryPercent)
                    .put("isCharging", telemetry.isCharging)
                    .put("batteryTempC", telemetry.batteryTempC.toDouble())
                    .put("ramUsedMb", telemetry.ramUsedMb)
                    .put("ramTotalMb", telemetry.ramTotalMb)
                    .put("storageUsedGb", telemetry.storageUsedGb.toDouble())
                    .put("storageTotalGb", telemetry.storageTotalGb.toDouble())
                    .put("vaultFreeSpaceBytes", vaultFileManager.freeSpaceBytes())
                    .put("screenWidth", telemetry.screenWidthPx)
                    .put("screenHeight", telemetry.screenHeightPx)
                    .put("uptimeMinutes", telemetry.uptimeMinutes)
                    .put("audioSampleRate", AudioCaptureManager.SAMPLE_RATE_HZ)
                    .put("audioChannels", AudioCaptureManager.CHANNELS)
                    .put(
                        "volumes",
                        JSONObject()
                            .put("media", telemetry.volumes.media)
                            .put("mediaMax", telemetry.volumes.mediaMax)
                            .put("ring", telemetry.volumes.ring)
                            .put("ringMax", telemetry.volumes.ringMax)
                            .put("notification", telemetry.volumes.notification)
                            .put("notificationMax", telemetry.volumes.notificationMax)
                            .put("alarm", telemetry.volumes.alarm)
                            .put("alarmMax", telemetry.volumes.alarmMax)
                            .put("ringerMode", telemetry.volumes.ringerMode)
                    )
                    .put(
                        "readiness",
                        JSONObject()
                            .put("screenMirrorActive", readiness.screenMirrorActive)
                            .put("audioForwardingActive", readiness.audioForwardingActive)
                            .put("audioForwardingSupported", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            .put("audioForwardingError", readiness.audioForwardingError ?: "")
                            .put("mirrorRotationDegrees", readiness.mirrorRotationDegrees)
                            .put("accessibilityConnected", DeckLinkAccessibilityService.isConnected())
                            .put("notificationBridgeConnected", DeckLinkNotificationListener.isConnected())
                            .put("torchEnabled", readiness.torchEnabled)
                            .put("finderRingActive", readiness.finderRingActive)
                    )
                    .put(
                        "capabilities",
                        JSONObject()
                            .put("screenMirror", caps.screenMirror)
                            .put("audioForwarding", caps.audioForwarding)
                            .put("remoteTouchAndInput", caps.remoteTouchAndInput)
                            .put("audioAndMedia", caps.audioAndMedia)
                            .put("clipboardSync", caps.clipboardSync)
                            .put("appLauncher", caps.appLauncher)
                            .put("fileVault", caps.fileVault)
                            .put("hardwareActions", caps.hardwareActions)
                            .put("notificationBridge", caps.notificationBridge)
                    )
                sendJson(out, 200, json)
            }

            // Mirror Rotation Sync (0 / 90 / 180 / 270)
            req.method == "POST" && req.path == "/api/screen/rotate" -> {
                if (!requireCapability(caps.screenMirror, clientIp, "Rotate Mirror", out)) return
                val deg = parseJson(bodyBytes).optInt("degrees", 0)
                DeckLinkStateStore.setMirrorRotationDegrees(deg)
                val finalDeg = DeckLinkStateStore.readiness.value.mirrorRotationDegrees
                DeckLinkStateStore.logCommand(clientIp, "Mirror Rotation", "${finalDeg}°", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("ok", true).put("degrees", finalDeg))
            }

            // Remote Touch, Gestures, System Keys & Text Input
            req.method == "POST" && req.path == "/api/input/tap" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "Remote Tap", out)) return
                val a11y = requireAccessibility(clientIp, "Remote Tap", out) ?: return
                val body = parseJson(bodyBytes)
                val x = body.optDouble("x", -1.0).toFloat()
                val y = body.optDouble("y", -1.0).toFloat()
                val duration = body.optLong("durationMs", 70L)
                if (x !in 0f..1f || y !in 0f..1f) {
                    sendJson(out, 400, JSONObject().put("error", "Coordinates must be normalized [0..1]"))
                    return
                }
                val ok = a11y.performNormalizedTap(x, y, duration)
                DeckLinkStateStore.logCommand(clientIp, "Remote Tap", "(${"%.2f".format(x)}, ${"%.2f".format(y)})", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            req.method == "POST" && req.path == "/api/input/swipe" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "Remote Swipe", out)) return
                val a11y = requireAccessibility(clientIp, "Remote Swipe", out) ?: return
                val body = parseJson(bodyBytes)
                val x1 = body.optDouble("x1", -1.0).toFloat()
                val y1 = body.optDouble("y1", -1.0).toFloat()
                val x2 = body.optDouble("x2", -1.0).toFloat()
                val y2 = body.optDouble("y2", -1.0).toFloat()
                val duration = body.optLong("durationMs", 240L)
                if (x1 !in 0f..1f || y1 !in 0f..1f || x2 !in 0f..1f || y2 !in 0f..1f) {
                    sendJson(out, 400, JSONObject().put("error", "Swipe coordinates must be normalized [0..1]"))
                    return
                }
                val ok = a11y.performNormalizedSwipe(x1, y1, x2, y2, duration)
                DeckLinkStateStore.logCommand(clientIp, "Remote Swipe", "(${"%.2f".format(x1)},${"%.2f".format(y1)}) -> (${"%.2f".format(x2)},${"%.2f".format(y2)})", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            req.method == "POST" && req.path == "/api/input/global" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "Global Key", out)) return
                val a11y = requireAccessibility(clientIp, "Global Key", out) ?: return
                val action = parseJson(bodyBytes).optString("action", "")
                val ok = a11y.performGlobalKey(action)
                DeckLinkStateStore.logCommand(clientIp, "System Key", action.uppercase(), if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok))
            }

            req.method == "POST" && req.path == "/api/input/text" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "Remote Typing", out)) return
                val a11y = requireAccessibility(clientIp, "Remote Typing", out) ?: return
                val body = parseJson(bodyBytes)
                val clear = body.optBoolean("clear", false)
                val ok = if (clear) {
                    a11y.clearFocusedText()
                } else {
                    val text = body.optString("text", "")
                    val append = body.optBoolean("append", true)
                    a11y.inputText(text, append)
                }
                DeckLinkStateStore.logCommand(clientIp, "Remote Keyboard", if (clear) "Cleared field" else "Typed text", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            req.method == "GET" && req.path == "/api/ui/tree" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "UI Tree", out)) return
                val a11y = requireAccessibility(clientIp, "UI Tree", out) ?: return
                val nodes = a11y.inspectInteractiveNodes()
                val arr = JSONArray()
                nodes.forEach { node ->
                    arr.put(
                        JSONObject()
                            .put("index", node.index)
                            .put("label", node.label)
                            .put("className", node.className)
                            .put("clickable", node.clickable)
                            .put("editable", node.editable)
                            .put("x", node.centerX.toDouble())
                            .put("y", node.centerY.toDouble())
                    )
                }
                sendJson(out, 200, JSONObject().put("nodes", arr))
            }

            req.method == "POST" && req.path == "/api/input/node-click" -> {
                if (!requireCapability(caps.remoteTouchAndInput, clientIp, "Node Click", out)) return
                val a11y = requireAccessibility(clientIp, "Node Click", out) ?: return
                val index = parseJson(bodyBytes).optInt("index", -1)
                val ok = a11y.clickInteractiveNode(index)
                DeckLinkStateStore.logCommand(clientIp, "UI Node Click", "Element #$index", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            // Audio & Media Controls
            req.method == "POST" && req.path == "/api/audio/volume" -> {
                if (!requireCapability(caps.audioAndMedia, clientIp, "Volume Control", out)) return
                val body = parseJson(bodyBytes)
                val stream = body.optString("stream", "")
                val level = body.optInt("level", -1)
                val ok = systemControl.setStreamVolume(stream, level)
                DeckLinkStateStore.logCommand(clientIp, "Volume Set", "$stream -> $level", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok))
            }

            req.method == "POST" && req.path == "/api/audio/ringer" -> {
                if (!requireCapability(caps.audioAndMedia, clientIp, "Ringer Mode", out)) return
                val mode = parseJson(bodyBytes).optString("mode", "")
                val ok = systemControl.setRingerMode(mode)
                DeckLinkStateStore.logCommand(clientIp, "Ringer Mode", mode.uppercase(), if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok))
            }

            req.method == "POST" && req.path == "/api/media/key" -> {
                if (!requireCapability(caps.audioAndMedia, clientIp, "Media Transport", out)) return
                val command = parseJson(bodyBytes).optString("command", "")
                val ok = systemControl.dispatchMediaCommand(command)
                DeckLinkStateStore.logCommand(clientIp, "Media Control", command.uppercase(), if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok))
            }

            // Bidirectional Clipboard Sync
            req.method == "GET" && req.path == "/api/clipboard" -> {
                if (!requireCapability(caps.clipboardSync, clientIp, "Clipboard Read", out)) return
                val text = systemControl.readClipboardText()
                DeckLinkStateStore.logCommand(clientIp, "Clipboard Pull", "${text.length} chars", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("text", text))
            }

            req.method == "POST" && req.path == "/api/clipboard" -> {
                if (!requireCapability(caps.clipboardSync, clientIp, "Clipboard Push", out)) return
                val text = parseJson(bodyBytes).optString("text", "")
                val ok = systemControl.writeClipboardText(text, source = "Laptop ($clientIp)")
                DeckLinkStateStore.logCommand(clientIp, "Clipboard Push", "${text.length} chars", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            // App Launcher
            req.method == "GET" && req.path == "/api/apps" -> {
                if (!requireCapability(caps.appLauncher, clientIp, "List Apps", out)) return
                val apps = systemControl.getLaunchableApps()
                val arr = JSONArray()
                apps.forEach { app ->
                    arr.put(JSONObject().put("label", app.label).put("packageName", app.packageName))
                }
                sendJson(out, 200, JSONObject().put("apps", arr))
            }

            req.method == "POST" && req.path == "/api/apps/launch" -> {
                if (!requireCapability(caps.appLauncher, clientIp, "Launch App", out)) return
                val pkg = parseJson(bodyBytes).optString("packageName", "")
                val ok = systemControl.launchApp(pkg)
                DeckLinkStateStore.logCommand(clientIp, "Launch App", pkg, if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 404, JSONObject().put("ok", ok))
            }

            // Notification Bridge
            req.method == "GET" && req.path == "/api/notifications" -> {
                if (!requireCapability(caps.notificationBridge, clientIp, "Read Notifications", out)) return
                DeckLinkNotificationListener.instance?.refreshSnapshot()
                val arr = JSONArray()
                DeckLinkStateStore.notifications.value.forEach { item ->
                    arr.put(
                        JSONObject()
                            .put("key", item.key)
                            .put("appName", item.appName)
                            .put("title", item.title)
                            .put("text", item.text)
                            .put("postedAtMs", item.postedAtMs)
                            .put("isClearable", item.isClearable)
                    )
                }
                sendJson(
                    out,
                    200,
                    JSONObject()
                        .put("connected", DeckLinkNotificationListener.isConnected())
                        .put("notifications", arr)
                )
            }

            req.method == "POST" && req.path == "/api/notifications/dismiss" -> {
                if (!requireCapability(caps.notificationBridge, clientIp, "Dismiss Notification", out)) return
                val key = parseJson(bodyBytes).optString("key", "")
                val ok = DeckLinkNotificationListener.instance?.dismissByKey(key) ?: false
                DeckLinkStateStore.logCommand(clientIp, "Dismiss Notification", if (ok) "Cleared" else "Unavailable", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, 200, JSONObject().put("ok", ok))
            }

            // Hardware Actions & Quick Tools
            req.method == "POST" && req.path == "/api/device/torch" -> {
                if (!requireCapability(caps.hardwareActions, clientIp, "Flashlight", out)) return
                val enabled = parseJson(bodyBytes).optBoolean("enabled", false)
                val ok = systemControl.setTorchEnabled(enabled)
                DeckLinkStateStore.logCommand(clientIp, "Flashlight", if (enabled) "ON" else "OFF", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, 200, JSONObject().put("ok", ok).put("enabled", DeckLinkStateStore.readiness.value.torchEnabled))
            }

            req.method == "POST" && req.path == "/api/device/ring" -> {
                if (!requireCapability(caps.hardwareActions, clientIp, "Find Phone Ring", out)) return
                val active = parseJson(bodyBytes).optBoolean("active", true)
                val ok = if (active) systemControl.startFinderRing() else {
                    systemControl.stopFinderRing()
                    true
                }
                DeckLinkStateStore.logCommand(clientIp, "Find Phone Ring", if (active) "Started chime" else "Stopped chime", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("ok", ok).put("active", DeckLinkStateStore.readiness.value.finderRingActive))
            }

            req.method == "POST" && req.path == "/api/device/vibrate" -> {
                if (!requireCapability(caps.hardwareActions, clientIp, "Vibrate", out)) return
                val durationMs = parseJson(bodyBytes).optLong("durationMs", 240L)
                systemControl.triggerVibration(durationMs)
                DeckLinkStateStore.logCommand(clientIp, "Haptic Ping", "${durationMs.coerceIn(40L, 1200L)}ms", LogStatus.ALLOWED)
                sendJson(out, 200, JSONObject().put("ok", true))
            }

            req.method == "POST" && req.path == "/api/device/open-url" -> {
                if (!requireCapability(caps.hardwareActions, clientIp, "Open URL", out)) return
                val url = parseJson(bodyBytes).optString("url", "")
                val ok = systemControl.openValidatedWebUrl(url)
                DeckLinkStateStore.logCommand(clientIp, "Open URL", url.take(80), if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(
                    out,
                    if (ok) 200 else 400,
                    JSONObject().put("ok", ok).apply {
                        if (!ok) put("error", "Only valid http:// or https:// URLs are allowed")
                    }
                )
            }

            // Automation Macros
            req.method == "GET" && req.path == "/api/macros" -> {
                val arr = JSONArray()
                DeckLinkStateStore.builtInMacros.forEach { macro ->
                    arr.put(
                        JSONObject()
                            .put("id", macro.id)
                            .put("title", macro.title)
                            .put("subtitle", macro.subtitle)
                            .put("steps", macro.stepsSummary)
                    )
                }
                sendJson(out, 200, JSONObject().put("macros", arr))
            }

            req.method == "POST" && req.path == "/api/macros/run" -> {
                if (!requireCapability(caps.hardwareActions && caps.audioAndMedia, clientIp, "Run Macro", out)) return
                val id = parseJson(bodyBytes).optString("id", "")
                val (ok, msg) = systemControl.runMacro(id)
                DeckLinkStateStore.logCommand(clientIp, "Macro Executed", "$id: $msg", if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok).put("message", msg))
            }

            // Secure Multi-Folder File Browser (Unlimited Size)
            req.method == "GET" && req.path == "/api/files" -> {
                if (!requireCapability(caps.fileVault, clientIp, "Browse Vault Files", out)) return
                val reqPath = vaultFileManager.normalizeRelativePath(req.query["path"].orEmpty())
                val entries = vaultFileManager.listEntries(reqPath)
                val entriesArr = JSONArray()
                val filesOnlyArr = JSONArray()
                entries.forEach { item ->
                    val obj = JSONObject()
                        .put("name", item.name)
                        .put("relativePath", item.relativePath)
                        .put("isDirectory", item.isDirectory)
                        .put("sizeBytes", item.sizeBytes)
                        .put("lastModifiedMs", item.lastModifiedMs)
                        .put("mimeType", item.mimeType)
                    entriesArr.put(obj)
                    if (!item.isDirectory) filesOnlyArr.put(obj)
                }
                sendJson(
                    out,
                    200,
                    JSONObject()
                        .put("currentPath", reqPath)
                        .put("freeSpaceBytes", vaultFileManager.freeSpaceBytes())
                        .put("entries", entriesArr)
                        .put("files", filesOnlyArr)
                )
            }

            req.method == "POST" && req.path == "/api/files/mkdir" -> {
                if (!requireCapability(caps.fileVault, clientIp, "Create Vault Folder", out)) return
                val body = parseJson(bodyBytes)
                val dirPath = body.optString("path", "")
                val folderName = body.optString("name", "")
                if (folderName.isBlank()) {
                    sendJson(out, 400, JSONObject().put("error", "Folder name cannot be empty"))
                    return
                }
                val res = vaultFileManager.createFolder(dirPath, folderName)
                if (res.isSuccess) {
                    val created = res.getOrThrow()
                    DeckLinkStateStore.logCommand(clientIp, "Folder Created", created.relativePath, LogStatus.ALLOWED)
                    sendJson(out, 200, JSONObject().put("ok", true).put("name", created.name).put("relativePath", created.relativePath))
                } else {
                    sendJson(out, 400, JSONObject().put("error", res.exceptionOrNull()?.message ?: "Could not create folder"))
                }
            }

            req.method == "DELETE" && req.path == "/api/files" -> {
                if (!requireCapability(caps.fileVault, clientIp, "Delete Vault Entry", out)) return
                val target = req.query["path"]?.ifBlank { null } ?: req.query["name"].orEmpty()
                val ok = vaultFileManager.deleteEntry(target)
                DeckLinkStateStore.logCommand(clientIp, "File Deleted", target, if (ok) LogStatus.ALLOWED else LogStatus.BLOCKED)
                sendJson(out, if (ok) 200 else 404, JSONObject().put("ok", ok))
            }

            else -> {
                sendJson(out, 404, JSONObject().put("error", "Endpoint not found"))
            }
        }
    }

    private fun validateBearerOrTicket(req: HttpHeader) =
        DeckLinkStateStore.validateBearerToken(extractBearer(req.headers))
            ?: DeckLinkStateStore.validateStreamTicket(req.query["ticket"])

    /**
     * Ultra-fast zero-RAM-buffering streaming file download with HTTP Range (206) support.
     */
    private fun streamFileDownload(
        socket: Socket,
        out: OutputStream,
        file: File,
        rangeHeader: String?,
        inline: Boolean
    ) {
        socket.soTimeout = 0
        val totalSize = file.length()
        val mime = vaultFileManager.guessMimeType(file.name)
        val safeFilename = file.name.replace("\"", "")
        val disposition = if (inline) "inline" else "attachment"

        var start = 0L
        var end = (totalSize - 1L).coerceAtLeast(0L)
        var isPartial = false

        if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=", ignoreCase = true) && totalSize > 0L) {
            val spec = rangeHeader.substring(6).substringBefore(',').trim()
            val dash = spec.indexOf('-')
            if (dash >= 0) {
                val sPart = spec.substring(0, dash).trim()
                val ePart = spec.substring(dash + 1).trim()
                val parsedStart = sPart.toLongOrNull()
                val parsedEnd = ePart.toLongOrNull()
                if (parsedStart != null) {
                    start = parsedStart.coerceIn(0L, totalSize - 1L)
                    end = (parsedEnd ?: (totalSize - 1L)).coerceIn(start, totalSize - 1L)
                    isPartial = true
                } else if (parsedEnd != null && parsedEnd > 0L) {
                    start = (totalSize - parsedEnd).coerceAtLeast(0L)
                    end = totalSize - 1L
                    isPartial = true
                }
            }
        }

        val contentLen = if (totalSize == 0L) 0L else (end - start + 1L)
        val header = buildString {
            append(if (isPartial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mime\r\n")
            append("Content-Length: $contentLen\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (isPartial) {
                append("Content-Range: bytes $start-$end/$totalSize\r\n")
            }
            append("Content-Disposition: $disposition; filename=\"$safeFilename\"\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.flush()

        if (contentLen <= 0L) return

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val buffer = ByteArray(IO_BUFFER_BYTES)
            var remaining = contentLen
            while (remaining > 0L && !socket.isClosed) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = raf.read(buffer, 0, toRead)
                if (read <= 0) break
                out.write(buffer, 0, read)
                remaining -= read.toLong()
            }
            out.flush()
        }
    }

    private suspend fun streamMjpeg(socket: Socket, out: OutputStream, ticket: String) {
        socket.soTimeout = 0
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: multipart/x-mixed-replace; boundary=$STREAM_BOUNDARY\r\n")
            append("Cache-Control: no-store, no-cache, must-revalidate\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.flush()

        var lastSentTs = 0L
        while (!socket.isClosed) {
            if (DeckLinkStateStore.validateStreamTicket(ticket) == null) break
            if (!DeckLinkStateStore.settings.value.capabilities.screenMirror) break
            if (!DeckLinkStateStore.readiness.value.screenMirrorActive) {
                delay(250L)
                continue
            }
            val frame = DeckLinkStateStore.latestScreenJpeg
            val ts = DeckLinkStateStore.latestFrameTimestampMs
            if (frame != null && ts != lastSentTs) {
                lastSentTs = ts
                val partHeader = buildString {
                    append("--$STREAM_BOUNDARY\r\n")
                    append("Content-Type: image/jpeg\r\n")
                    append("Content-Length: ${frame.size}\r\n\r\n")
                }
                val wrote = runCatching {
                    out.write(partHeader.toByteArray(Charsets.US_ASCII))
                    out.write(frame)
                    out.write("\r\n".toByteArray(Charsets.US_ASCII))
                    out.flush()
                    true
                }.getOrDefault(false)
                if (!wrote) break
            }
            val fps = DeckLinkStateStore.settings.value.mirrorFps.coerceIn(3, 20)
            delay((1000L / fps).coerceAtLeast(45L))
        }
    }

    private fun streamAudioPcm(socket: Socket, out: OutputStream, ticket: String) {
        socket.soTimeout = 0
        val header = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/octet-stream\r\n")
            append("Transfer-Encoding: chunked\r\n")
            append("Cache-Control: no-store, no-cache\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("X-Audio-Sample-Rate: ${AudioCaptureManager.SAMPLE_RATE_HZ}\r\n")
            append("X-Audio-Channels: ${AudioCaptureManager.CHANNELS}\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.flush()

        val queue = DeckLinkStateStore.registerAudioSubscriber()
        try {
            while (!socket.isClosed) {
                if (DeckLinkStateStore.validateStreamTicket(ticket) == null) break
                if (!DeckLinkStateStore.settings.value.capabilities.audioForwarding) break
                if (!DeckLinkStateStore.readiness.value.audioForwardingActive) break

                val chunk = queue.poll(300, TimeUnit.MILLISECONDS) ?: continue
                if (chunk.isEmpty()) continue
                val ok = runCatching {
                    val sizeHex = Integer.toHexString(chunk.size)
                    out.write("$sizeHex\r\n".toByteArray(Charsets.US_ASCII))
                    out.write(chunk)
                    out.write("\r\n".toByteArray(Charsets.US_ASCII))
                    out.flush()
                    true
                }.getOrDefault(false)
                if (!ok) break
            }
            runCatching {
                out.write("0\r\n\r\n".toByteArray(Charsets.US_ASCII))
                out.flush()
            }
        } finally {
            DeckLinkStateStore.unregisterAudioSubscriber(queue)
        }
    }

    private fun requireCapability(enabled: Boolean, clientIp: String, action: String, out: OutputStream): Boolean {
        if (!enabled) {
            DeckLinkStateStore.logCommand(clientIp, action, "Blocked by phone capability switch", LogStatus.BLOCKED)
            sendJson(out, 403, JSONObject().put("error", "$action is disabled in DeckLink on your phone"))
            return false
        }
        return true
    }

    private fun requireAccessibility(clientIp: String, action: String, out: OutputStream): DeckLinkAccessibilityService? {
        val svc = DeckLinkAccessibilityService.instance
        if (svc == null) {
            DeckLinkStateStore.logCommand(clientIp, action, "Accessibility service not enabled", LogStatus.BLOCKED)
            sendJson(out, 503, JSONObject().put("error", "Enable DeckLink Accessibility Service on your phone for remote touch & keys"))
        }
        return svc
    }

    private fun extractBearer(headers: Map<String, String>): String? {
        val auth = headers["authorization"] ?: return null
        if (!auth.startsWith("Bearer ", ignoreCase = true)) return null
        return auth.substring(7).trim().ifBlank { null }
    }

    private fun parseJson(bytes: ByteArray): JSONObject {
        if (bytes.isEmpty()) return JSONObject()
        return runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrElse { JSONObject() }
    }

    private fun sendJson(out: OutputStream, statusCode: Int, json: JSONObject) {
        sendBytes(out, statusCode, "application/json; charset=utf-8", json.toString().toByteArray(Charsets.UTF_8))
    }

    private fun sendBytes(
        out: OutputStream,
        statusCode: Int,
        contentType: String,
        bytes: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val reason = when (statusCode) {
            200 -> "OK"
            206 -> "Partial Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            413 -> "Payload Too Large"
            429 -> "Too Many Requests"
            503 -> "Service Unavailable"
            else -> "Error"
        }
        val headers = buildString {
            append("HTTP/1.1 $statusCode $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Referrer-Policy: no-referrer\r\n")
            extraHeaders.forEach { (k, v) -> append("$k: $v\r\n") }
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }
}
