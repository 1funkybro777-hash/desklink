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
import com.example.data.DeckLinkStateStore
import com.example.model.LogStatus
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private val isCapturing = AtomicBoolean(false)

    @Volatile
    private var lastEncodeTimeMs: Long = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stop()
        }
    }

    fun start(resultCode: Int, data: Intent): Boolean {
        stop()
        return runCatching {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mgr.getMediaProjection(resultCode, data) ?: return false

            val metrics = context.resources.displayMetrics
            val settings = DeckLinkStateStore.settings.value
            val scale = settings.mirrorQuality.scale
            val captureWidth = ((metrics.widthPixels * scale).toInt().coerceIn(320, 960) / 2) * 2
            val captureHeight = ((metrics.heightPixels * scale).toInt().coerceIn(480, 1920) / 2) * 2
            val densityDpi = (metrics.densityDpi * scale).toInt().coerceIn(120, 480)

            val thread = HandlerThread("DeckLinkScreenCapture").apply { start() }
            val handler = Handler(thread.looper)

            projection.registerCallback(projectionCallback, handler)

            val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            reader.setOnImageAvailableListener({ imgReader ->
                processLatestImage(imgReader, captureWidth, captureHeight)
            }, handler)

            val display = projection.createVirtualDisplay(
                "DeckLinkMirror",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            mediaProjection = projection
            imageReader = reader
            virtualDisplay = display
            captureThread = thread
            captureHandler = handler
            isCapturing.set(true)

            DeckLinkStateStore.updateReadiness { it.copy(screenMirrorActive = true) }
            DeckLinkStateStore.logCommand(
                "local",
                "Screen Mirror Started",
                "${captureWidth}x${captureHeight} @ ${settings.mirrorFps} FPS",
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

            val jpegOut = ByteArrayOutputStream(32 * 1024)
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
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { captureThread?.quitSafely() }
        captureThread = null
        captureHandler = null
        DeckLinkStateStore.latestScreenJpeg = null
        DeckLinkStateStore.updateReadiness { it.copy(screenMirrorActive = false) }
        if (wasActive) {
            DeckLinkStateStore.logCommand("local", "Screen Mirror Stopped", "Capture ended", LogStatus.SYSTEM)
        }
    }
}
