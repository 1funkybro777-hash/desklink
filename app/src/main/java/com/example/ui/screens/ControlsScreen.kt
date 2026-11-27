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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Send
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.RemoteCapabilities
import com.example.model.VaultFileItem
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.DeepNavyInk
import com.example.ui.theme.PeriwinkleHero
import com.example.ui.theme.WarmStoneTile

@Composable
fun ControlsScreen(
    state: DeckLinkUiState,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onRunMacro: (String) -> Unit,
    onStopFinderRing: () -> Unit,
    onPushClipboard: (String) -> Unit,
    onCopyClipboard: (String) -> Unit,
    onPickMediaForVault: () -> Unit,
    onPickDocumentForVault: () -> Unit,
    onShareVaultFile: (String) -> Unit,
    onDeleteVaultFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = Files, 1 = Clipboard, 2 = Permissions
    var clipDraft by remember { mutableStateOf("") }
    val caps = state.settings.capabilities

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Share Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Transfer files, sync clipboard snippets, and manage laptop permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented Pill Switcher (inspired by Screenshot 3 & 4's Share Hub tabs)
            item {
                Surface(
                    color = WarmStoneTile,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Files", "Clipboard", "Permissions").forEachIndexed { idx, label ->
                            val selected = selectedSegment == idx
                            Surface(
                                onClick = { selectedSegment = idx },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (selected) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("hub_tab_${label.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.readiness.finderRingActive) {
                item {
                    Button(
                        onClick = onStopFinderRing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_finder_ring_button")
                    ) {
                        Icon(Icons.Rounded.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Silence Active Finder Ring")
                    }
                }
            }

            when (selectedSegment) {
                // SEGMENT 0: SHARE HUB FILES
                0 -> {
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
                                Text(
                                    text = "Send Files Across Devices",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pick photos, videos, or documents from your phone to download instantly on your laptop — or open files uploaded from your laptop.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onPickMediaForVault,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PeriwinkleHero,
                                            contentColor = DeepNavyInk
                                        ),
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("add_vault_media_button")
                                    ) {
                                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Photo / Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    OutlinedButton(
                                        onClick = onPickDocumentForVault,
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            }
                        }
                    }

                    if (state.vaultFiles.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "No shared files yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Add a photo or document above, or drop files from your laptop console.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.vaultFiles, key = { it.name }) { file ->
                            VaultFileCard(
                                item = file,
                                onShare = { onShareVaultFile(file.name) },
                                onDelete = { onDeleteVaultFile(file.name) }
                            )
                        }
                    }
                }

                // SEGMENT 1: CLIPBOARD SHARE HUB
                1 -> {
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
                                Text(
                                    text = "Shared Clipboard",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = clipDraft,
                                    onValueChange = { clipDraft = it },
                                    placeholder = { Text("Type or paste a link or note for your laptop...") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("clipboard_draft_input")
                                )
                                Button(
                                    onClick = {
                                        if (clipDraft.isNotBlank()) {
                                            onPushClipboard(clipDraft)
                                            clipDraft = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PeriwinkleHero,
                                        contentColor = DeepNavyInk
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_clipboard_button")
                                ) {
                                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Push to Shared Clipboard")
                                }
                            }
                        }
                    }

                    if (state.clipboardHistory.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "Recent snippets synced between your phone and laptop will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(state.clipboardHistory, key = { it.id }) { clip ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = clip.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = clip.source,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCopyClipboard(clip.text) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = stringResource(R.string.cd_copy),
                                            tint = CobaltPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SEGMENT 2: GRANULAR PERMISSIONS & MACROS
                else -> {
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
                                    color = CobaltPrimary,
                                    modifier = Modifier.padding(bottom = 6.dp)
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
                                    title = "Cross Control (Touch & Typing)",
                                    subtitle = "Allow taps, swipes, Back/Home/Lock, and keyboard input",
                                    checked = caps.remoteTouchAndInput,
                                    testTag = "cap_remote_touch",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "Audio, Volume & Media",
                                    subtitle = "Control Media/Ring/Alarm levels and playback",
                                    checked = caps.audioAndMedia,
                                    testTag = "cap_audio_media",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(audioAndMedia = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "Clipboard Sync",
                                    subtitle = "Pull phone clipboard or push text from laptop",
                                    checked = caps.clipboardSync,
                                    testTag = "cap_clipboard",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(clipboardSync = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "App Streaming / Launcher",
                                    subtitle = "Browse installed apps and launch them remotely",
                                    checked = caps.appLauncher,
                                    testTag = "cap_app_launcher",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(appLauncher = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "Share Hub File Vault",
                                    subtitle = "Upload files from laptop or download vault files",
                                    checked = caps.fileVault,
                                    testTag = "cap_file_vault",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(fileVault = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "Hardware Actions & Links",
                                    subtitle = "Flashlight, Find-My-Phone chime, vibrate, and URLs",
                                    checked = caps.hardwareActions,
                                    testTag = "cap_hardware",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(hardwareActions = v) } }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CapabilitySwitchRow(
                                    title = "Notifications Bridge",
                                    subtitle = "Mirror active phone notifications and allow dismiss",
                                    checked = caps.notificationBridge,
                                    testTag = "cap_notifications",
                                    onCheckedChange = { v -> onUpdateCapability { it.copy(notificationBridge = v) } }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "QUICK AUTOMATION MACROS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(state.macros, key = { it.id }) { macro ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                    tint = CobaltPrimary
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
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
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
private fun VaultFileCard(
    item: VaultFileItem,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = fileBadgeColorForName(item.name)
    val extLabel = item.name.substringAfterLast('.', "FILE").uppercase().take(4)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = extLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
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
                    tint = CobaltPrimary
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
