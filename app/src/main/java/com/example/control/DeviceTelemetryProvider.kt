package com.example.control

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.model.DeviceTelemetry
import com.example.model.VolumeLevels
import java.net.Inet4Address
import java.net.NetworkInterface

class DeviceTelemetryProvider(private val context: Context) {

    fun collect(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = tempTenths / 10f

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorageBytes = statFs.blockCountLong * statFs.blockSizeLong
        val freeStorageBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val totalStorageGb = (totalStorageBytes / 1_000_000_000f).coerceAtLeast(0.1f)
        val usedStorageGb = ((totalStorageBytes - freeStorageBytes) / 1_000_000_000f).coerceAtLeast(0f)

        val displayMetrics = context.resources.displayMetrics
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerModeStr = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            else -> "normal"
        }

        val volumes = VolumeLevels(
            media = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
            mediaMax = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1),
            ring = audio.getStreamVolume(AudioManager.STREAM_RING),
            ringMax = audio.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1),
            notification = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
            notificationMax = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1),
            alarm = audio.getStreamVolume(AudioManager.STREAM_ALARM),
            alarmMax = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1),
            ringerMode = ringerModeStr
        )

        return DeviceTelemetry(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            ipAddress = resolveLocalIpv4Address(),
            networkType = resolveNetworkType(),
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            storageUsedGb = usedStorageGb,
            storageTotalGb = totalStorageGb,
            screenWidthPx = displayMetrics.widthPixels,
            screenHeightPx = displayMetrics.heightPixels,
            uptimeMinutes = SystemClock.elapsedRealtime() / 60_000L,
            volumes = volumes
        )
    }

    fun resolveLocalIpv4Address(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            val candidates = mutableListOf<Pair<Int, String>>()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val priority = when {
                    name.startsWith("wlan") -> 1
                    name.startsWith("ap") || name.startsWith("swlan") -> 2
                    name.startsWith("eth") || name.startsWith("en") -> 3
                    name.startsWith("rndis") || name.startsWith("usb") -> 4
                    else -> 5
                }
                for (addr in iface.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        candidates.add(priority to host)
                    }
                }
            }
            candidates.minByOrNull { it.first }?.second ?: "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    private fun resolveNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Local Only"
        val caps = cm.getNetworkCapabilities(network) ?: return "Local Only"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi LAN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> "USB Tethering"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Connected"
        }
    }
}
