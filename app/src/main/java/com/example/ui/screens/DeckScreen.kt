package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun DeckScreen(
    state: DeckLinkUiState,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onRequestScreenMirror: () -> Unit,
    onStopScreenMirror: () -> Unit,
    onStartAudioForwarding: () -> Unit,
    onStopAudioForwarding: () -> Unit,
    onSetMirrorRotation: (Int) -> Unit,
    onOpenShareHub: () -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onRotatePin: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onRevokeSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val trimmedQuery = searchQuery.trim().lowercase()
    val matchingApps = remember(state.installedApps, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else state.installedApps.filter {
            it.label.lowercase().contains(trimmedQuery) || it.packageName.lowercase().contains(trimmedQuery)
        }.take(6)
    }
    val matchingFiles = remember(state.vaultFiles, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else state.vaultFiles.filter { it.name.lowercase().contains(trimmedQuery) }.take(6)
    }

    val isOnline = state.readiness.serverRunning

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
            // App Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DeckLink",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureBlack
                        )
                        Text(
                            text = "${state.telemetry.deviceModel} • ${state.telemetry.networkType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureBlack
                        )
                    }
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(if (isOnline) 2.dp else 1.dp, PureBlack)
                    ) {
                        Text(
                            text = if (isOnline) "● ONLINE" else "○ STANDBY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // SECTION 1: SEARCH EVERYWHERE
            item {
                MonoSectionCard(title = "1. SEARCH EVERYWHERE") {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PureBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search apps and files...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureBlack),
                                    cursorBrush = SolidColor(PureBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_everywhere_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search),
                                        tint = PureBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = trimmedQuery.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (matchingApps.isEmpty() && matchingFiles.isEmpty()) {
                                Text(
                                    text = "No matching apps or files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureBlack
                                )
                            }
                            matchingApps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchApp(app.packageName, app.label) }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[APP] ${app.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = PureBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("OPEN →", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                                }
                            }
                            matchingFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenShareHub() }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[FILE] ${file.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("VIEW →", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: LAPTOP CONNECTION & PAIRING PIN
            item {
                MonoSectionCard(title = "2. LAPTOP CONNECTION & PIN") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LAPTOP BROWSER URL", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                            Text(
                                text = state.serverUrl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PureBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onCopyUrl,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_url_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.cd_copy), tint = PureBlack)
                        }
                    }

                    HorizontalDivider(color = PureBlack, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("6-DIGIT PAIRING PIN", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                            Text(
                                text = state.pairingPin.chunked(3).joinToString(" "),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 2.sp,
                                color = PureBlack
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onCopyPin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_pin_button")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.cd_copy), tint = PureBlack)
                            }
                            IconButton(
                                onClick = onRotatePin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rotate_pin_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_refresh_pin), tint = PureBlack)
                            }
                        }
                    }

                    if (state.readiness.serverError != null) {
                        Text(
                            text = "Error: ${state.readiness.serverError}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                    }

                    MonoActionButton(
                        text = if (isOnline) "Stop Remote Host" else "Start Remote Host",
                        emphasized = true,
                        testTag = if (isOnline) "stop_server_button" else "start_server_button",
                        onClick = { if (isOnline) onStopHost() else onStartHost() }
                    )
                }
            }

            // SECTION 3: SCREEN MIRROR & ROTATION
            item {
                MonoSectionCard(title = "3. SCREEN MIRROR & ROTATION") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Cast, contentDescription = null, tint = PureBlack, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (state.readiness.screenMirrorActive) {
                                "Status: LIVE MIRRORING ACTIVE (${state.readiness.mirrorRotationDegrees}°)"
                            } else {
                                "Status: STANDBY — Start to mirror screen on laptop"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PureBlack
                        )
                    }

                    MonoActionButton(
                        text = if (state.readiness.screenMirrorActive) "Stop Screen Mirror" else "Start Screen Mirror",
                        emphasized = state.readiness.screenMirrorActive,
                        testTag = "mirror_toggle_button",
                        onClick = {
                            if (state.readiness.screenMirrorActive) onStopScreenMirror()
                            else onRequestScreenMirror()
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                        Text(
                            text = "LAPTOP MIRROR ROTATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "0°", 90 to "90°", 180 to "180°", 270 to "270°").forEach { (deg, label) ->
                            val selected = state.readiness.mirrorRotationDegrees == deg
                            MonoChoiceChip(
                                label = label,
                                selected = selected,
                                testTag = "rotate_$deg",
                                onClick = { onSetMirrorRotation(deg) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // SECTION 4: PHONE-TO-LAPTOP LIVE AUDIO
            item {
                MonoSectionCard(title = "4. PHONE SOUND TO LAPTOP") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = PureBlack, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (state.readiness.audioForwardingActive) {
                                "Status: STREAMING PHONE AUDIO TO LAPTOP"
                            } else {
                                "Stream music, videos & game sound from this phone straight to laptop speakers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (state.readiness.audioForwardingActive) FontWeight.Bold else FontWeight.Normal,
                            color = PureBlack
                        )
                    }

                    if (!state.readiness.audioForwardingError.isNullOrBlank()) {
                        Text(
                            text = state.readiness.audioForwardingError,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PureBlack
                        )
                    }

                    MonoActionButton(
                        text = if (state.readiness.audioForwardingActive) "Stop Phone Audio Stream" else "Start Phone Audio Stream",
                        emphasized = state.readiness.audioForwardingActive,
                        testTag = "audio_forwarding_toggle_button",
                        onClick = {
                            if (state.readiness.audioForwardingActive) onStopAudioForwarding()
                            else onStartAudioForwarding()
                        }
                    )
                }
            }

            // SECTION 5: PAIRED LAPTOPS
            item {
                MonoSectionCard(title = "5. PAIRED LAPTOPS (${state.sessions.size})") {
                    if (state.sessions.isEmpty()) {
                        Text(
                            text = "No laptops connected yet. Open the URL above in your laptop browser and enter the 6-digit PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureBlack
                        )
                    } else {
                        state.sessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.clientLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PureBlack
                                    )
                                    Text(
                                        text = session.clientIp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureBlack
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onRevokeSession(session.sessionId) },
                                    border = BorderStroke(1.dp, PureBlack),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("revoke_session_${session.sessionId}")
                                ) {
                                    Text("Disconnect", color = PureBlack, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: LIVE DEVICE TELEMETRY
            item {
                val t = state.telemetry
                MonoSectionCard(title = "6. LIVE DEVICE TELEMETRY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonoStatCell("BATTERY", "${t.batteryPercent}%")
                        MonoStatCell("RAM", "${"%.1f".format(t.ramUsedMb / 1024f)}/${"%.0f".format(t.ramTotalMb / 1024f)}G")
                        MonoStatCell("STORAGE", "${"%.0f".format(t.storageUsedGb)}/${"%.0f".format(t.storageTotalGb)}G")
                        MonoStatCell("TEMP", "${"%.1f".format(t.batteryTempC)}°C")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
internal fun MonoSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, PureBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
            HorizontalDivider(color = PureBlack, thickness = 1.dp)
            content()
        }
    }
}

@Composable
internal fun MonoActionButton(
    text: String,
    emphasized: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = PureWhite,
        contentColor = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (emphasized) 2.2.dp else 1.2.dp, PureBlack),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PureBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun MonoChoiceChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = PureWhite,
        contentColor = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (selected) 2.2.dp else 1.dp, PureBlack),
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (selected) "● $label" else label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = PureBlack
            )
        }
    }
}

@Composable
private fun MonoStatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = PureBlack
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureBlack
        )
    }
}
