package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.control.DeviceTelemetryProvider
import com.example.control.ScreenCaptureManager
import com.example.control.SystemControlManager
import com.example.data.DeckLinkStateStore
import com.example.data.VaultFileManager
import com.example.model.LogStatus
import com.example.server.DeckLinkHttpServer

class DeckLinkHostService : Service() {

    companion object {
        const val ACTION_START_SERVER = "com.example.decklink.START_SERVER"
        const val ACTION_STOP_SERVER = "com.example.decklink.STOP_SERVER"
        const val ACTION_START_MIRROR = "com.example.decklink.START_MIRROR"
        const val ACTION_STOP_MIRROR = "com.example.decklink.STOP_MIRROR"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_ID = "decklink_host_channel"
        private const val NOTIFICATION_ID = 4102

        fun startServerIntent(context: Context, port: Int): Intent {
            return Intent(context, DeckLinkHostService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, port)
            }
        }

        fun stopServerIntent(context: Context): Intent {
            return Intent(context, DeckLinkHostService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
        }

        fun startMirrorIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, DeckLinkHostService::class.java).apply {
                action = ACTION_START_MIRROR
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
        }

        fun stopMirrorIntent(context: Context): Intent {
            return Intent(context, DeckLinkHostService::class.java).apply {
                action = ACTION_STOP_MIRROR
            }
        }
    }

    private lateinit var telemetryProvider: DeviceTelemetryProvider
    private lateinit var systemControl: SystemControlManager
    private lateinit var vaultFileManager: VaultFileManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var httpServer: DeckLinkHttpServer
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        telemetryProvider = DeviceTelemetryProvider(applicationContext)
        systemControl = SystemControlManager(applicationContext)
        vaultFileManager = VaultFileManager(applicationContext)
        screenCaptureManager = ScreenCaptureManager(applicationContext)
        httpServer = DeckLinkHttpServer(
            context = applicationContext,
            telemetryProvider = telemetryProvider,
            systemControl = systemControl,
            vaultFileManager = vaultFileManager
        )
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVER -> {
                stopAllAndFinish()
                return START_NOT_STICKY
            }

            ACTION_STOP_MIRROR -> {
                screenCaptureManager.stop()
                promoteForeground(includeMediaProjection = false)
                return START_STICKY
            }

            ACTION_START_MIRROR -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                // Ensure server is running and foreground service declares mediaProjection BEFORE getMediaProjection
                ensureServerStarted(DeckLinkStateStore.settings.value.port)
                promoteForeground(includeMediaProjection = true)
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val started = screenCaptureManager.start(resultCode, data)
                    if (!started) {
                        promoteForeground(includeMediaProjection = false)
                    } else {
                        updateNotification(mirrorActive = true)
                    }
                } else {
                    promoteForeground(includeMediaProjection = false)
                }
                return START_STICKY
            }

            else -> {
                val requestedPort = intent?.getIntExtra(EXTRA_PORT, DeckLinkStateStore.settings.value.port)
                    ?: DeckLinkStateStore.settings.value.port
                promoteForeground(includeMediaProjection = DeckLinkStateStore.readiness.value.screenMirrorActive)
                ensureServerStarted(requestedPort)
                return START_STICKY
            }
        }
    }

    private fun ensureServerStarted(port: Int) {
        if (DeckLinkStateStore.readiness.value.serverRunning && httpServer.boundPort == port) {
            updateNotification(mirrorActive = DeckLinkStateStore.readiness.value.screenMirrorActive)
            return
        }
        val result = httpServer.start(port)
        if (result.isSuccess) {
            val activePort = result.getOrThrow()
            acquireWakeLockIfConfigured()
            DeckLinkStateStore.updateReadiness {
                it.copy(serverRunning = true, serverError = null)
            }
            val url = "http://${telemetryProvider.resolveLocalIpv4Address()}:$activePort"
            DeckLinkStateStore.logCommand("local", "Remote Host Started", "Listening at $url", LogStatus.SYSTEM)
            updateNotification(mirrorActive = DeckLinkStateStore.readiness.value.screenMirrorActive)
        } else {
            val err = result.exceptionOrNull()?.message ?: "Unable to bind port $port"
            DeckLinkStateStore.updateReadiness {
                it.copy(serverRunning = false, serverError = err)
            }
            DeckLinkStateStore.logCommand("local", "Host Start Failed", err, LogStatus.BLOCKED)
            stopSelf()
        }
    }

    private fun promoteForeground(includeMediaProjection: Boolean) {
        val notification = buildNotification(mirrorActive = includeMediaProjection)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (includeMediaProjection) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(mirrorActive: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(mirrorActive))
    }

    private fun buildNotification(mirrorActive: Boolean): Notification {
        val ip = telemetryProvider.resolveLocalIpv4Address()
        val port = httpServer.boundPort
        val endpoint = "http://$ip:$port"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopServerIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (mirrorActive) {
            getString(R.string.service_notification_mirror_active, endpoint)
        } else {
            getString(R.string.service_notification_text, endpoint)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_stop_server), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_channel_desc)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLockIfConfigured() {
        releaseWakeLock()
        if (!DeckLinkStateStore.settings.value.keepAwakeWhileHosting) return
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeckLink:HostWakeLock").apply {
                setReferenceCounted(false)
                acquire(60 * 60 * 1000L) // 1 hour bounded lock
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        }
    }

    private fun stopAllAndFinish() {
        screenCaptureManager.stop()
        httpServer.stop()
        systemControl.stopFinderRing()
        releaseWakeLock()
        DeckLinkStateStore.revokeAllSessions()
        DeckLinkStateStore.updateReadiness {
            it.copy(serverRunning = false, screenMirrorActive = false, finderRingActive = false)
        }
        DeckLinkStateStore.logCommand("local", "Remote Host Stopped", "Server closed", LogStatus.SYSTEM)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        screenCaptureManager.stop()
        httpServer.stop()
        systemControl.release()
        releaseWakeLock()
        DeckLinkStateStore.updateReadiness {
            it.copy(serverRunning = false, screenMirrorActive = false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
