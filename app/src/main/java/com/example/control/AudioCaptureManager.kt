package com.example.control

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.DeckLinkStateStore
import com.example.model.LogStatus
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures internal Android playback audio (media, videos, games) via Android 10+
 * AudioPlaybackCaptureConfiguration and broadcasts real-time 16-bit PCM frames to paired laptops.
 */
class AudioCaptureManager(private val context: Context) {

    companion object {
        const val SAMPLE_RATE_HZ = 24_000
        const val CHANNELS = 1
        private const val CHUNK_BYTES = 2_400 // ~50 ms of 24kHz 16-bit mono PCM
    }

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(mediaProjection: MediaProjection): Boolean {
        stop()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            DeckLinkStateStore.updateReadiness {
                it.copy(
                    audioForwardingActive = false,
                    audioForwardingError = "Internal phone audio capture requires Android 10 or newer."
                )
            }
            return false
        }

        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordAudio) {
            DeckLinkStateStore.updateReadiness {
                it.copy(
                    audioForwardingActive = false,
                    audioForwardingError = "Grant Microphone/Audio Capture permission in the Phone Audio section."
                )
            }
            return false
        }

        return runCatching {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(CHUNK_BYTES * 2)

            val recorder = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuf * 2)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IllegalStateException("AudioRecord failed to initialize")
            }

            audioRecord = recorder
            isRunning.set(true)
            recorder.startRecording()

            val thread = Thread({
                val buf = ByteArray(CHUNK_BYTES)
                while (isRunning.get()) {
                    val read = runCatching { recorder.read(buf, 0, buf.size) }.getOrDefault(-1)
                    if (read > 0) {
                        if (DeckLinkStateStore.settings.value.capabilities.audioForwarding) {
                            DeckLinkStateStore.broadcastAudioPcm(buf.copyOf(read))
                        }
                    } else if (read < 0) {
                        break
                    }
                }
            }, "DeckLinkAudioCapture").apply {
                priority = Thread.MAX_PRIORITY - 1
                start()
            }
            captureThread = thread

            DeckLinkStateStore.updateReadiness {
                it.copy(audioForwardingActive = true, audioForwardingError = null)
            }
            DeckLinkStateStore.logCommand(
                "local",
                "Phone Audio Stream Started",
                "${SAMPLE_RATE_HZ} Hz PCM internal playback audio active",
                LogStatus.SYSTEM
            )
            true
        }.getOrElse { err ->
            stop()
            DeckLinkStateStore.updateReadiness {
                it.copy(
                    audioForwardingActive = false,
                    audioForwardingError = err.message ?: "Unable to capture internal phone audio"
                )
            }
            DeckLinkStateStore.logCommand(
                "local",
                "Phone Audio Error",
                err.message ?: "Audio capture unavailable",
                LogStatus.BLOCKED
            )
            false
        }
    }

    fun stop() {
        val wasRunning = isRunning.getAndSet(false)
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { captureThread?.interrupt() }
        captureThread = null
        DeckLinkStateStore.updateReadiness {
            it.copy(audioForwardingActive = false)
        }
        if (wasRunning) {
            DeckLinkStateStore.logCommand("local", "Phone Audio Stopped", "Internal audio stream ended", LogStatus.SYSTEM)
        }
    }
}
