package com.example.control

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.example.data.DeckLinkStateStore
import com.example.model.LaunchableApp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SystemControlManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var activeRingtone: Ringtone? = null
    private var torchCameraId: String? = findBackTorchCameraId()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                DeckLinkStateStore.updateReadiness { it.copy(torchEnabled = enabled) }
            }
        }
    }

    init {
        runCatching {
            cameraManager.registerTorchCallback(torchCallback, mainHandler)
        }
    }

    fun release() {
        stopFinderRing()
        runCatching {
            cameraManager.unregisterTorchCallback(torchCallback)
        }
    }

    fun setStreamVolume(streamName: String, level: Int): Boolean {
        val streamType = when (streamName.lowercase()) {
            "media", "music" -> AudioManager.STREAM_MUSIC
            "ring" -> AudioManager.STREAM_RING
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "alarm" -> AudioManager.STREAM_ALARM
            else -> return false
        }
        val max = audioManager.getStreamMaxVolume(streamType)
        val clamped = level.coerceIn(0, max)
        return runCatching {
            audioManager.setStreamVolume(streamType, clamped, AudioManager.FLAG_SHOW_UI)
            true
        }.getOrDefault(false)
    }

    fun setRingerMode(mode: String): Boolean {
        val target = when (mode.lowercase()) {
            "normal" -> AudioManager.RINGER_MODE_NORMAL
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "silent" -> AudioManager.RINGER_MODE_SILENT
            else -> return false
        }
        return runCatching {
            audioManager.ringerMode = target
            true
        }.getOrDefault(false)
    }

    fun dispatchMediaCommand(command: String): Boolean {
        val keyCode = when (command.lowercase()) {
            "play_pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return false
        }
        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
        return true
    }

    fun setTorchEnabled(enabled: Boolean): Boolean {
        val cameraId = torchCameraId ?: findBackTorchCameraId().also { torchCameraId = it }
            ?: return false
        return runCatching {
            cameraManager.setTorchMode(cameraId, enabled)
            DeckLinkStateStore.updateReadiness { it.copy(torchEnabled = enabled) }
            true
        }.getOrDefault(false)
    }

    fun triggerVibration(durationMs: Long = 220L) {
        val safeDuration = durationMs.coerceIn(40L, 1200L)
        runCatching {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(safeDuration)
            }
        }
    }

    fun startFinderRing(): Boolean {
        return runCatching {
            stopFinderRing()
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alertUri) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
            activeRingtone = ringtone
            triggerVibration(500L)
            DeckLinkStateStore.updateReadiness { it.copy(finderRingActive = true) }
            true
        }.getOrDefault(false)
    }

    fun stopFinderRing() {
        runCatching {
            activeRingtone?.stop()
            activeRingtone = null
            DeckLinkStateStore.updateReadiness { it.copy(finderRingActive = false) }
        }
    }

    fun readClipboardText(): String {
        var result = ""
        val latch = CountDownLatch(1)
        mainHandler.post {
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    result = clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
                }
            }
            latch.countDown()
        }
        latch.await(1500, TimeUnit.MILLISECONDS)
        return result
    }

    fun writeClipboardText(text: String, source: String = "Laptop"): Boolean {
        val safeText = text.take(16_000)
        var ok = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            ok = runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("DeckLink Sync", safeText))
                DeckLinkStateStore.recordClipboard(safeText, source)
                true
            }.getOrDefault(false)
            latch.countDown()
        }
        latch.await(1500, TimeUnit.MILLISECONDS)
        return ok
    }

    fun getLaunchableApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                LaunchableApp(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String): Boolean {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty() || !cleanPkg.matches(Regex("^[a-zA-Z0-9._]+$"))) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(cleanPkg) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun openValidatedWebUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        if (parsed.host.isNullOrBlank()) return false

        val intent = Intent(Intent.ACTION_VIEW, parsed).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun runMacro(macroId: String): Pair<Boolean, String> {
        return when (macroId) {
            "desk_mode" -> {
                setRingerMode("vibrate")
                val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                setStreamVolume("media", (maxMedia * 0.35f).toInt())
                triggerVibration(120L)
                true to "Desk Mode applied"
            }
            "focus_silent" -> {
                setRingerMode("silent")
                setStreamVolume("media", 0)
                stopFinderRing()
                true to "Deep Focus applied"
            }
            "media_stage" -> {
                setRingerMode("normal")
                val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                setStreamVolume("media", (maxMedia * 0.80f).toInt())
                dispatchMediaCommand("play_pause")
                true to "Media Stage applied"
            }
            "locate_beacon" -> {
                setTorchEnabled(true)
                startFinderRing()
                true to "Locate Beacon active"
            }
            else -> false to "Unknown macro"
        }
    }

    private fun findBackTorchCameraId(): String? {
        return runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
