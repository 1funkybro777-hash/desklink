package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.RemoteCapabilities
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun ControlsScreen(
    state: DeckLinkUiState,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggleTorch: () -> Unit,
    onRunMacro: (String) -> Unit,
    onStopFinderRing: () -> Unit,
    onPushClipboard: (String) -> Unit,
    onCopyClipboard: (String) -> Unit,
    onPickMediaForVault: () -> Unit,
    onPickDocumentForVault: () -> Unit,
    onShareVaultFile: (String) -> Unit,
    onDeleteVaultFile: (String) -> Unit,
    onCopyUsbCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clipDraft by remember { mutableStateOf("") }
    val caps = state.settings.capabilities

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "All Features",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureBlack
                    )
                    Text(
                        text = "Dedicated section for every phone-to-laptop control feature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureBlack
                    )
                }
            }

            // FEATURE SECTION 1: SCREEN MIRROR & AUDIO STREAMING
            item {
                MonoSectionCard(title = "SCREEN MIRROR & PHONE AUDIO") {
                    MonoSwitchRow(
                        title = "Allow Live Screen Mirror",
                        subtitle = "Stream live phone video & rotation to laptop",
                        checked = caps.screenMirror,
                        testTag = "cap_screen_mirror",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(screenMirror = v) } }
                    )
                    HorizontalDivider(color = PureBlack, thickness = 1.dp)
                    MonoSwitchRow(
                        title = "Allow Phone Sound to Laptop",
                        subtitle = "Forward music, videos & game sound to laptop speakers",
                        checked = caps.audioForwarding,
                        testTag = "cap_audio_forwarding",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(audioForwarding = v) } }
                    )
                }
            }

            // FEATURE SECTION 2: REMOTE TOUCH, GESTURES & KEYBOARD
            item {
                MonoSectionCard(title = "REMOTE TOUCH & KEYBOARD") {
                    Text(
                        text = if (state.readiness.accessibilityConnected) {
                            "Accessibility Engine: ACTIVE"
                        } else {
                            "Accessibility Engine: NOT ENABLED — Tap below to allow remote taps, swipes & typing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = PureBlack
                    )
                    MonoActionButton(
                        text = if (state.readiness.accessibilityConnected) "Accessibility Settings (Active)" else "Enable Remote Touch Service",
                        testTag = "accessibility_setup_button",
                        onClick = onOpenAccessibilitySettings
                    )
                    MonoSwitchRow(
                        title = "Allow Remote Touch & Typing",
                        subtitle = "Permit clicks, swipes, Back/Home/Lock & keyboard input",
                        checked = caps.remoteTouchAndInput,
                        testTag = "cap_remote_touch",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } }
                    )
                }
            }

            // FEATURE SECTION 3: AUDIO, VOLUME & MEDIA
            item {
                MonoSectionCard(title = "VOLUME, RINGER & MEDIA CONTROLS") {
                    MonoSwitchRow(
                        title = "Allow Volume & Media Control",
                        subtitle = "Media/Ring/Notification/Alarm levels, ringer mode & Play/Pause/Next",
                        checked = caps.audioAndMedia,
                        testTag = "cap_audio_media",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(audioAndMedia = v) } }
                    )
                }
            }

            // FEATURE SECTION 4: APP LAUNCHER / STREAMING
            item {
                MonoSectionCard(title = "APP LAUNCHER (${state.installedApps.size} APPS)") {
                    MonoSwitchRow(
                        title = "Allow Remote App Launching",
                        subtitle = "Browse and open installed phone apps directly from laptop",
                        checked = caps.appLauncher,
                        testTag = "cap_app_launcher",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(appLauncher = v) } }
                    )
                }
            }

            // FEATURE SECTION 5: FILE TRANSFER VAULT
            item {
                MonoSectionCard(title = "FILE TRANSFER VAULT (${state.vaultFiles.size} FILES)") {
                    MonoSwitchRow(
                        title = "Allow Two-Way File Transfers",
                        subtitle = "Upload files from laptop or download vault files on laptop",
                        checked = caps.fileVault,
                        testTag = "cap_file_vault",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(fileVault = v) } }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MonoActionButton(
                            text = "Add Photo / Video",
                            testTag = "add_vault_media_button",
                            onClick = onPickMediaForVault,
                            modifier = Modifier.weight(1f)
                        )
                        MonoActionButton(
                            text = "Add Document",
                            testTag = "add_vault_document_button",
                            onClick = onPickDocumentForVault,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.vaultFiles.isEmpty()) {
                        Text(
                            text = "No files in vault yet. Add a file above or send one from your laptop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureBlack
                        )
                    } else {
                        state.vaultFiles.forEach { file ->
                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PureBlack)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = PureBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatBytes(file.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PureBlack
                                        )
                                    }
                                    IconButton(
                                        onClick = { onShareVaultFile(file.name) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.cd_share_file), tint = PureBlack)
                                    }
                                    IconButton(
                                        onClick = { onDeleteVaultFile(file.name) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.cd_delete_file), tint = PureBlack)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FEATURE SECTION 6: CLIPBOARD SYNC
            item {
                MonoSectionCard(title = "CLIPBOARD SYNC") {
                    MonoSwitchRow(
                        title = "Allow Bidirectional Clipboard",
                        subtitle = "Copy and paste text snippets between laptop and phone",
                        checked = caps.clipboardSync,
                        testTag = "cap_clipboard",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(clipboardSync = v) } }
                    )

                    OutlinedTextField(
                        value = clipDraft,
                        onValueChange = { clipDraft = it },
                        placeholder = { Text("Enter text or link to share with laptop...", color = PureBlack) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureBlack,
                            unfocusedTextColor = PureBlack,
                            focusedBorderColor = PureBlack,
                            unfocusedBorderColor = PureBlack,
                            cursorColor = PureBlack,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clipboard_draft_input")
                    )

                    MonoActionButton(
                        text = "Push to Shared Clipboard",
                        testTag = "save_clipboard_button",
                        onClick = {
                            if (clipDraft.isNotBlank()) {
                                onPushClipboard(clipDraft)
                                clipDraft = ""
                            }
                        }
                    )

                    state.clipboardHistory.take(4).forEach { clip ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clip.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureBlack,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = clip.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureBlack
                                )
                            }
                            IconButton(
                                onClick = { onCopyClipboard(clip.text) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.cd_copy), tint = PureBlack)
                            }
                        }
                    }
                }
            }

            // FEATURE SECTION 7: NOTIFICATIONS BRIDGE
            item {
                MonoSectionCard(title = "NOTIFICATIONS BRIDGE") {
                    Text(
                        text = if (state.readiness.notificationListenerConnected) {
                            "Notification Access: ACTIVE"
                        } else {
                            "Notification Access: NOT ENABLED — Grant access to view & dismiss alerts on laptop."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PureBlack
                    )
                    MonoActionButton(
                        text = if (state.readiness.notificationListenerConnected) "Notification Settings (Active)" else "Enable Notification Bridge",
                        testTag = "notification_setup_button",
                        onClick = onOpenNotificationSettings
                    )
                    MonoSwitchRow(
                        title = "Allow Notification Mirroring",
                        subtitle = "Show phone notifications on laptop and allow remote dismiss",
                        checked = caps.notificationBridge,
                        testTag = "cap_notifications",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(notificationBridge = v) } }
                    )
                }
            }

            // FEATURE SECTION 8: HARDWARE ACTIONS & AUTOMATION MACROS
            item {
                MonoSectionCard(title = "HARDWARE & AUTOMATION MACROS") {
                    MonoSwitchRow(
                        title = "Allow Hardware Actions & Links",
                        subtitle = "Flashlight, Ring Phone, Vibrate Ping, and Open Web Links",
                        checked = caps.hardwareActions,
                        testTag = "cap_hardware",
                        onCheckedChange = { v -> onUpdateCapability { it.copy(hardwareActions = v) } }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MonoActionButton(
                            text = if (state.readiness.torchEnabled) "Flashlight: ON" else "Flashlight: OFF",
                            emphasized = state.readiness.torchEnabled,
                            testTag = "local_torch_button",
                            onClick = onToggleTorch,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.readiness.finderRingActive) {
                            MonoActionButton(
                                text = "Stop Ring Alert",
                                emphasized = true,
                                testTag = "stop_finder_ring_button",
                                onClick = onStopFinderRing,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    state.macros.forEach { macro ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(macro.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PureBlack)
                                Text(macro.stepsSummary, style = MaterialTheme.typography.bodySmall, color = PureBlack)
                            }
                            Surface(
                                onClick = { onRunMacro(macro.id) },
                                color = PureWhite,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.2.dp, PureBlack),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("run_macro_${macro.id}")
                            ) {
                                Text(
                                    text = "Run",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FEATURE SECTION 9: USB CABLE BRIDGE
            item {
                MonoSectionCard(title = "USB CABLE BRIDGE (ZERO-LAG)") {
                    Text(
                        text = state.adbForwardCommand,
                        style = MaterialTheme.typography.labelMedium,
                        color = PureBlack
                    )
                    MonoActionButton(
                        text = "Copy USB Forward Command",
                        testTag = "copy_usb_command_button",
                        onClick = onCopyUsbCommand
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
internal fun MonoSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PureBlack
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureBlack,
                checkedTrackColor = PureWhite,
                checkedBorderColor = PureBlack,
                uncheckedThumbColor = PureBlack,
                uncheckedTrackColor = PureWhite,
                uncheckedBorderColor = PureBlack
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${"%.1f".format(bytes / 1024f)} KB"
        else -> "${"%.2f".format(bytes / (1024f * 1024f))} MB"
    }
}
