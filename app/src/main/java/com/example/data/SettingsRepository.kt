package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.MirrorQuality
import com.example.model.RemoteCapabilities
import com.example.model.ServerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deckLinkDataStore: DataStore<Preferences> by preferencesDataStore(name = "decklink_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PORT = intPreferencesKey("server_port")
        val MIRROR_QUALITY = stringPreferencesKey("mirror_quality")
        val MIRROR_FPS = intPreferencesKey("mirror_fps")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val CAP_MIRROR = booleanPreferencesKey("cap_mirror")
        val CAP_AUDIO_FORWARDING = booleanPreferencesKey("cap_audio_forwarding")
        val CAP_TOUCH = booleanPreferencesKey("cap_touch")
        val CAP_AUDIO = booleanPreferencesKey("cap_audio")
        val CAP_CLIPBOARD = booleanPreferencesKey("cap_clipboard")
        val CAP_APPS = booleanPreferencesKey("cap_apps")
        val CAP_FILES = booleanPreferencesKey("cap_files")
        val CAP_HARDWARE = booleanPreferencesKey("cap_hardware")
        val CAP_NOTIFICATIONS = booleanPreferencesKey("cap_notifications")
    }

    val settingsFlow: Flow<ServerSettings> = context.deckLinkDataStore.data.map { prefs ->
        val qualityName = prefs[Keys.MIRROR_QUALITY] ?: MirrorQuality.BALANCED.name
        val quality = MirrorQuality.entries.firstOrNull { it.name == qualityName } ?: MirrorQuality.BALANCED
        ServerSettings(
            port = (prefs[Keys.PORT] ?: 8765).coerceIn(1024, 65535),
            mirrorQuality = quality,
            mirrorFps = (prefs[Keys.MIRROR_FPS] ?: 10).coerceIn(3, 20),
            keepAwakeWhileHosting = prefs[Keys.KEEP_AWAKE] ?: true,
            capabilities = RemoteCapabilities(
                screenMirror = prefs[Keys.CAP_MIRROR] ?: true,
                audioForwarding = prefs[Keys.CAP_AUDIO_FORWARDING] ?: true,
                remoteTouchAndInput = prefs[Keys.CAP_TOUCH] ?: true,
                audioAndMedia = prefs[Keys.CAP_AUDIO] ?: true,
                clipboardSync = prefs[Keys.CAP_CLIPBOARD] ?: true,
                appLauncher = prefs[Keys.CAP_APPS] ?: true,
                fileVault = prefs[Keys.CAP_FILES] ?: true,
                hardwareActions = prefs[Keys.CAP_HARDWARE] ?: true,
                notificationBridge = prefs[Keys.CAP_NOTIFICATIONS] ?: true
            )
        )
    }

    suspend fun saveSettings(settings: ServerSettings) {
        context.deckLinkDataStore.edit { prefs ->
            prefs[Keys.PORT] = settings.port.coerceIn(1024, 65535)
            prefs[Keys.MIRROR_QUALITY] = settings.mirrorQuality.name
            prefs[Keys.MIRROR_FPS] = settings.mirrorFps.coerceIn(3, 20)
            prefs[Keys.KEEP_AWAKE] = settings.keepAwakeWhileHosting
            prefs[Keys.CAP_MIRROR] = settings.capabilities.screenMirror
            prefs[Keys.CAP_AUDIO_FORWARDING] = settings.capabilities.audioForwarding
            prefs[Keys.CAP_TOUCH] = settings.capabilities.remoteTouchAndInput
            prefs[Keys.CAP_AUDIO] = settings.capabilities.audioAndMedia
            prefs[Keys.CAP_CLIPBOARD] = settings.capabilities.clipboardSync
            prefs[Keys.CAP_APPS] = settings.capabilities.appLauncher
            prefs[Keys.CAP_FILES] = settings.capabilities.fileVault
            prefs[Keys.CAP_HARDWARE] = settings.capabilities.hardwareActions
            prefs[Keys.CAP_NOTIFICATIONS] = settings.capabilities.notificationBridge
        }
    }
}
