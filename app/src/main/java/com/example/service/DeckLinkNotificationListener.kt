package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.DeckLinkStateStore
import com.example.model.BridgedNotification
import com.example.model.LogStatus

class DeckLinkNotificationListener : NotificationListenerService() {

    companion object {
        @Volatile
        var instance: DeckLinkNotificationListener? = null
            private set

        fun isConnected(): Boolean = instance != null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        DeckLinkStateStore.updateReadiness { it.copy(notificationListenerConnected = true) }
        DeckLinkStateStore.logCommand(
            "local",
            "Notification Bridge Ready",
            "Active notifications can sync to paired laptop",
            LogStatus.SYSTEM
        )
        refreshSnapshot()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        DeckLinkStateStore.updateReadiness { it.copy(notificationListenerConnected = false) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    fun refreshSnapshot() {
        runCatching {
            val pm = packageManager
            val list = activeNotifications.orEmpty()
                .filterNot { it.packageName == packageName }
                .sortedByDescending { it.postTime }
                .take(35)
                .map { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                    val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                    val appName = runCatching {
                        val ai = pm.getApplicationInfo(sbn.packageName, 0)
                        pm.getApplicationLabel(ai).toString()
                    }.getOrDefault(sbn.packageName)

                    BridgedNotification(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        appName = appName,
                        title = title.ifBlank { appName },
                        text = text.take(280),
                        postedAtMs = sbn.postTime,
                        isClearable = sbn.isClearable
                    )
                }
            DeckLinkStateStore.updateNotifications(list)
        }
    }

    fun dismissByKey(key: String): Boolean {
        return runCatching {
            cancelNotification(key)
            refreshSnapshot()
            true
        }.getOrDefault(false)
    }
}
