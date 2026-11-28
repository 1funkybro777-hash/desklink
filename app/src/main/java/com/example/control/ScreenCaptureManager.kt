package com.example.control

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import com.example.data.DeckLinkStateStore
import com.example.model.LogStatus
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureManager(
    private val context: Context,
    private val onProjectionStopped: () -> Unit = {}
) {

    @Volatile
    var activeMediaProjection: MediaProjection? = null
        private set

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var displayManager: DisplayManager? = null
    private val isCapturing = AtomicBoolean(false)

    @Volatile
    private var currentCaptureWidth: Int = 0

    @Volatile
    private var currentCaptureHeight: Int = 0

    @Volatile
    private var lastEncodeTimeMs: Long = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stop()
            onProjectionStopped()
        }
    }

    // Automatically detects phone portrait <-> landscape switches (e.g. YouTube fullscreen)
    // and resizes the VirtualDisplay so widescreen content streams full-frame with zero black bars.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY || !isCapturing.get()) return
            val handler = captureHandler ?: return
            handler.post {
                checkAndResizeForOrientation()
            }
        }
    }

    fun start(resultCode: Int, data: Intent): Boolean {
        stop()
        return runCatching {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mgr.getMediaProjection(resultCode, data) ?: return false

            val thread = HandlerThread("DeckLinkScreenCapture").apply { start() }
            val handler = Handler(thread.looper)

            projection.registerCallback(projectionCallback, handler)

            activeMediaProjection = projection
            captureThread = thread
            captureHandler = handler
            isCapturing.set(true)

            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager = dm
            dm.registerDisplayListener(displayListener, handler)

            val (w, h, dpi) = computeCaptureDimensions()
            createCaptureSurface(projection, w, h, dpi, handler)

            DeckLinkStateStore.updateReadiness { it.copy(screenMirrorActive = true) }
            DeckLinkStateStore.logCommand(
                "local",
                "Screen Mirror Started",
                "${w}x${h} @ ${DeckLinkStateStore.settings.value.mirrorFps} FPS",
                LogStatus.SYSTEM
            )
            true
        }.getOrElse { err ->
            stop()
            DeckLinkStateStore.logCommand(
                "local",
                "Screen Mirror Error",
                err.message ?: "Unable to start MediaProjection",
                LogStatus.BLOCKED
            )
            false
        }
    }

    private fun computeCaptureDimensions(): Triple<Int, Int, Int> {
        val metrics = context.resources.displayMetrics
        val settings = DeckLinkStateStore.settings.value
        val scale = settings.mirrorQuality.scale
        val rawW = (metrics.widthPixels * scale).toInt()
        val rawH = (metrics.heightPixels * scale).toInt()
        val isLandscape = rawW > rawH
        val captureWidth = if (isLandscape) {
            (rawW.coerceIn(640, 1600) / 2) * 2
        } else {
            (rawW.coerceIn(360, 1080) / 2) * 2
        }
        val captureHeight = if (isLandscape) {
            (rawH.coerceIn(360, 1080) / 2) * 2
        } else {
            (rawH.coerceIn(640, 1920) / 2) * 2
        }
        val densityDpi = (metrics.densityDpi * scale).toInt().coerceIn(120, 480)
        return Triple(captureWidth, captureHeight, densityDpi)
    }

    private fun createCaptureSurface(
        projection: MediaProjection,
        width: Int,
        height: Int,
        dpi: Int,
        handler: Handler
    ) {
        currentCaptureWidth = width
        currentCaptureHeight = height

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader.setOnImageAvailableListener({ imgReader ->
            processLatestImage(imgReader, width, height)
        }, handler)

        val existingDisplay = virtualDisplay
        if (existingDisplay != null) {
            existingDisplay.resize(width, height, dpi)
            existingDisplay.surface = reader.surface
            val oldReader = imageReader
            imageReader = reader
            runCatching { oldReader?.setOnImageAvailableListener(null, null) }
            runCatching { oldReader?.close() }
        } else {
            val display = projection.createVirtualDisplay(
                "DeckLinkMirror",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
            imageReader = reader
            virtualDisplay = display
        }
    }

    private fun checkAndResizeForOrientation() {
        val projection = activeMediaProjection ?: return
        val handler = captureHandler ?: return
        if (!isCapturing.get()) return
        val (newW, newH, newDpi) = computeCaptureDimensions()
        if (newW != currentCaptureWidth || newH != currentCaptureHeight) {
            runCatching {
                createCaptureSurface(projection, newW, newH, newDpi, handler)
            }
        }
    }

    private fun processLatestImage(reader: ImageReader, width: Int, height: Int) {
        if (!isCapturing.get()) return
        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return
        try {
            val settings = DeckLinkStateStore.settings.value
            val minIntervalMs = (1000L / settings.mirrorFps.coerceIn(3, 20))
            val now = System.currentTimeMillis()
            if (now - lastEncodeTimeMs < minIntervalMs) {
                return
            }
            lastEncodeTimeMs = now

            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val paddedWidth = width + rowPadding / pixelStride
            val fullBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            fullBitmap.copyPixelsFromBuffer(buffer)

            val croppedBitmap = if (paddedWidth != width) {
                Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
            } else {
                fullBitmap
            }

            val jpegOut = ByteArrayOutputStream(48 * 1024)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, settings.mirrorQuality.jpegQuality, jpegOut)
            val jpegBytes = jpegOut.toByteArray()

            if (croppedBitmap !== fullBitmap) {
                croppedBitmap.recycle()
            }
            fullBitmap.recycle()

            DeckLinkStateStore.latestScreenJpeg = jpegBytes
            DeckLinkStateStore.latestFrameTimestampMs = now
        } catch (_: Throwable) {
            // Skip frame if display rotates or surface closes mid-frame
        } finally {
            runCatching { image.close() }
        }
    }

    fun stop() {
        val wasActive = isCapturing.getAndSet(false)
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        displayManager = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { activeMediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { activeMediaProjection?.stop() }
        activeMediaProjection = null
        runCatching { captureThread?.quitSafely() }
        captureThread = null
        captureHandler = null
        currentCaptureWidth = 0
        currentCaptureHeight = 0
        DeckLinkStateStore.latestScreenJpeg = null
        DeckLinkStateStore.updateReadiness { it.copy(screenMirrorActive = false) }
        if (wasActive) {
            DeckLinkStateStore.logCommand("local", "Screen Mirror Stopped", "Capture ended", LogStatus.SYSTEM)
        }
    }
}
