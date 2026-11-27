package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.RemoteCapabilities
import com.example.model.VaultFileItem
import com.example.ui.DeckLinkUiState

@Composable
fun ControlsScreen(
    state: DeckLinkUiState,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onRunMacro: (String) -> Unit,
    onToggleTorch: () -> Unit,
    onStopFinderRing: () -> Unit,
    onPickMediaForVault: () -> Unit,
    onPickDocumentForVault: () -> Unit,
    onShareVaultFile: (String) -> Unit,
    onDeleteVaultFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val caps = state.settings.capabilities

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Remote Controls & Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Decide exactly what your paired laptop is allowed to control in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Local Overrides (Torch / Active Finder Alert)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleTorch,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("local_torch_button")
                    ) {
                        Icon(
                            imageVector = if (state.readiness.torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.readiness.torchEnabled) "Flashlight ON" else "Flashlight OFF")
                    }

                    if (state.readiness.finderRingActive) {
                        Button(
                            onClick = onStopFinderRing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stop_finder_ring_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Silence Ring")
                        }
                    }
                }
            }

            // Granular Capability Switches
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LAPTOP PERMISSION GATES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        CapabilitySwitchRow(
                            title = "Live Screen Mirror",
                            subtitle = "Allow laptop to view live video stream",
                            checked = caps.screenMirror,
                            testTag = "cap_screen_mirror",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(screenMirror = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Remote Touch, Keys & Typing",
                            subtitle = "Allow taps, swipes, Back/Home/Lock, and keyboard input",
                            checked = caps.remoteTouchAndInput,
                            testTag = "cap_remote_touch",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Audio, Volume & Media",
                            subtitle = "Control Media/Ring/Alarm levels, ringer mode, and playback",
                            checked = caps.audioAndMedia,
                            testTag = "cap_audio_media",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(audioAndMedia = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Bidirectional Clipboard Sync",
                            subtitle = "Pull phone clipboard or push text from laptop",
                            checked = caps.clipboardSync,
                            testTag = "cap_clipboard",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(clipboardSync = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "App Launcher",
                            subtitle = "Browse installed apps and launch them remotely",
                            checked = caps.appLauncher,
                            testTag = "cap_app_launcher",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(appLauncher = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Two-Way File Vault",
                            subtitle = "Upload files from laptop or download vault files",
                            checked = caps.fileVault,
                            testTag = "cap_file_vault",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(fileVault = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Hardware Actions & Links",
                            subtitle = "Flashlight, Find-My-Phone chime, vibrate, and open URLs",
                            checked = caps.hardwareActions,
                            testTag = "cap_hardware",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(hardwareActions = v) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CapabilitySwitchRow(
                            title = "Notification Bridge",
                            subtitle = "Mirror active notifications and allow remote dismiss",
                            checked = caps.notificationBridge,
                            testTag = "cap_notifications",
                            onCheckedChange = { v -> onUpdateCapability { it.copy(notificationBridge = v) } }
                        )
                    }
                }
            }

            // Automation Macros
            item {
                Text(
                    text = "AUTOMATION MACROS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(state.macros, key = { it.id }) { macro ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = macro.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = macro.stepsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { onRunMacro(macro.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("run_macro_${macro.id}")
                        ) {
                            Text("Run")
                        }
                    }
                }
            }

            // Shared File Vault (Play-compliant Photo Picker + SAF OpenDocument)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SHARED FILE VAULT",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.vaultFiles.size} FILES",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Drop photos or documents here to download from your laptop, or access files uploaded by your laptop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPickMediaForVault,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("add_vault_media_button")
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Photo/Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onPickDocumentForVault,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("add_vault_document_button")
                            ) {
                                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Document", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (state.vaultFiles.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Vault is empty. Files stay privately sandboxed until shared or uploaded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.vaultFiles.forEach { file ->
                                    VaultFileRow(
                                        item = file,
                                        onShare = { onShareVaultFile(file.name) },
                                        onDelete = { onDeleteVaultFile(file.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CapabilitySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun VaultFileRow(
    item: VaultFileItem,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.cd_share_file),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_file),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${"%.1f".format(bytes / 1024f)} KB"
        else -> "${"%.2f".format(bytes / (1024f * 1024f))} MB"
    }
}
