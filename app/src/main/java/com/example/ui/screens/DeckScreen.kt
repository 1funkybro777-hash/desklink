package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ScreenRotationAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MirrorQuality
import com.example.model.RemoteCapabilities
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBlueTint
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartLiveGreen
import com.example.ui.theme.SmartLiveGreenTint
import com.example.ui.theme.SmartSearchPill
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated

private enum class HomeSheet {
    NONE,
    APP_STREAMING,
    MOBILE_DESKTOP,
    SECURITY_PIN,
    DISPLAY_CONTROL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    state: DeckLinkUiState,
    onOpenDrawer: () -> Unit,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onRequestScreenMirror: () -> Unit,
    onStopScreenMirror: () -> Unit,
    onStartAudioForwarding: () -> Unit,
    onStopAudioForwarding: () -> Unit,
    onSetMirrorRotation: (Int) -> Unit,
    onUpdateMirrorQuality: (MirrorQuality) -> Unit,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onOpenActions: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onRotatePin: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onCopyUsbCommand: () -> Unit,
    onRevokeSession: (String) -> Unit,
    onRevokeAllSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSheet by remember { mutableStateOf(HomeSheet.NONE) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val hostRunning = state.readiness.serverRunning
    val mirrorLive = state.readiness.screenMirrorActive
    val audioLive = state.readiness.audioForwardingActive
    val activeLaptopName = state.sessions.firstOrNull()?.clientLabel
    val heroTitle = when {
        activeLaptopName != null -> activeLaptopName
        hostRunning -> "PC dashboard ready"
        else -> "PC dashboard preview"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartCanvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar (Matches Photo 1: Left action, Right Speaker + Overflow Menu)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("home_open_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Open navigation menu",
                            tint = SmartInk
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (audioLive) onStopAudioForwarding() else onStartAudioForwarding()
                            },
                            modifier = Modifier.testTag("home_top_audio_button")
                        ) {
                            Icon(
                                imageVector = if (audioLive) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = "Toggle phone audio to laptop",
                                tint = if (audioLive) SmartBlue else SmartInkMuted
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("home_overflow_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options",
                                    tint = SmartInk
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = SmartSurfaceElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (hostRunning) "Stop PC Host Server" else "Start PC Host Server") },
                                    onClick = {
                                        showOverflowMenu = false
                                        if (hostRunning) onStopHost() else onStartHost()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rotate Pairing PIN") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onRotatePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Activity Log") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenActivity()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Centered Laptop Hero + Title + Wi-Fi & Battery Status Row (Exact Match to Photo 1)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_device_laptop_1795856433396),
                        contentDescription = "Connected laptop illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(190.dp)
                            .height(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = heroTitle,
                        color = SmartSlateTitle,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status strip: Blue Wi-Fi icon + Battery + Interactive Host Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { if (hostRunning) onStopHost() else onStartHost() },
                            shape = CircleShape,
                            color = if (hostRunning) SmartBlue else SmartSearchPill,
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("home_wifi_host_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = "Toggle PC host server",
                                    tint = if (hostRunning) PureWhite else SmartInkMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.BatteryStd,
                                contentDescription = "Phone battery",
                                tint = SmartInkMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "${state.telemetry.batteryPercent}",
                                color = SmartInkMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            onClick = {
                                if (!hostRunning) onStartHost() else activeSheet = HomeSheet.MOBILE_DESKTOP
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (hostRunning) SmartLiveGreenTint else SmartSearchPill,
                            modifier = Modifier.testTag("home_host_status_pill")
                        ) {
                            Text(
                                text = if (hostRunning) "Live · PIN ${state.pairingPin}" else "Tap to start host",
                                color = if (hostRunning) SmartLiveGreen else SmartInkMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Full-width Top Pill Tile: "App streaming" (Exact Match to Photo 1)
            item {
                Surface(
                    onClick = {
                        if (!hostRunning) onStartHost()
                        activeSheet = HomeSheet.APP_STREAMING
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = if (hostRunning) SmartSurfaceElevated else SmartCanvas,
                    border = BorderStroke(1.dp, if (hostRunning) SmartBlue else SmartBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .testTag("tile_app_streaming")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = "App streaming",
                            tint = if (hostRunning) SmartBlue else SmartInk,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "App streaming",
                            color = SmartInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Row 1: Mobile desktop | Screen share (Exact 2-column match to Photo 1)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SmartDashboardTile(
                        title = "Mobile desktop",
                        subtitle = if (hostRunning) ":${state.settings.port}" else null,
                        icon = Icons.Outlined.DesktopWindows,
                        isActive = hostRunning,
                        onClick = {
                            if (!hostRunning) onStartHost()
                            activeSheet = HomeSheet.MOBILE_DESKTOP
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_mobile_desktop")
                    )

                    SmartDashboardTile(
                        title = "Screen share",
                        subtitle = if (mirrorLive) "Live · ${state.readiness.mirrorRotationDegrees}°" else null,
                        icon = Icons.Outlined.CastConnected,
                        isActive = mirrorLive,
                        onClick = {
                            if (mirrorLive) onStopScreenMirror() else onRequestScreenMirror()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_screen_share")
                    )
                }
            }

            // Row 2: Phone audio | Remote touch (Replaces unavailable Webcam/Hotspot with real working features)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SmartDashboardTile(
                        title = "Phone audio",
                        subtitle = if (audioLive) "Streaming" else null,
                        icon = Icons.Outlined.GraphicEq,
                        isActive = audioLive,
                        onClick = {
                            if (audioLive) onStopAudioForwarding() else onStartAudioForwarding()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_phone_audio")
                    )

                    SmartDashboardTile(
                        title = "Remote touch",
                        subtitle = if (state.readiness.accessibilityConnected) "Ready" else null,
                        icon = Icons.Outlined.TouchApp,
                        isActive = state.readiness.accessibilityConnected,
                        onClick = onOpenActions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_remote_touch")
                    )
                }
            }

            // Row 3: Security & PIN | Display and Control (Exact 2-column match to Photo 1)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SmartDashboardTile(
                        title = "Security & PIN",
                        subtitle = if (state.sessions.isNotEmpty()) "${state.sessions.size} paired" else null,
                        icon = Icons.Outlined.Lock,
                        isActive = state.sessions.isNotEmpty(),
                        onClick = { activeSheet = HomeSheet.SECURITY_PIN },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_security_pin")
                    )

                    SmartDashboardTile(
                        title = "Display and Control",
                        subtitle = "${state.readiness.mirrorRotationDegrees}° · ${state.settings.mirrorFps}fps",
                        icon = Icons.Outlined.ScreenRotationAlt,
                        isActive = state.readiness.mirrorRotationDegrees != 0,
                        onClick = { activeSheet = HomeSheet.DISPLAY_CONTROL },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_display_control")
                    )
                }
            }
        }
    }

    // Bottom Sheets for Tiles
    if (activeSheet != HomeSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = HomeSheet.NONE },
            sheetState = sheetState,
            containerColor = SmartCanvas,
            contentColor = SmartInk
        ) {
            when (activeSheet) {
                HomeSheet.APP_STREAMING -> AppStreamingSheetContent(
                    state = state,
                    onCopyUrl = onCopyUrl,
                    onCopyPin = onCopyPin,
                    onLaunchApp = onLaunchApp
                )

                HomeSheet.MOBILE_DESKTOP -> MobileDesktopSheetContent(
                    state = state,
                    onStartHost = onStartHost,
                    onStopHost = onStopHost,
                    onCopyUrl = onCopyUrl,
                    onCopyPin = onCopyPin,
                    onRotatePin = onRotatePin,
                    onCopyUsbCommand = onCopyUsbCommand
                )

                HomeSheet.SECURITY_PIN -> SecurityPinSheetContent(
                    state = state,
                    onCopyPin = onCopyPin,
                    onRotatePin = onRotatePin,
                    onRevokeSession = onRevokeSession,
                    onRevokeAllSessions = onRevokeAllSessions,
                    onUpdateCapability = onUpdateCapability
                )

                HomeSheet.DISPLAY_CONTROL -> DisplayControlSheetContent(
                    state = state,
                    onRequestScreenMirror = onRequestScreenMirror,
                    onStopScreenMirror = onStopScreenMirror,
                    onSetMirrorRotation = onSetMirrorRotation,
                    onUpdateMirrorQuality = onUpdateMirrorQuality
                )

                HomeSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun SmartDashboardTile(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (isActive) SmartBlueTint else SmartCanvas,
        border = BorderStroke(1.dp, if (isActive) SmartBlue else SmartBorder),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) SmartBlue else SmartInk,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = SmartInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (isActive) SmartBlue else SmartInkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AppStreamingSheetContent(
    state: DeckLinkUiState,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onLaunchApp: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(query, state.installedApps) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) state.installedApps.take(24)
        else state.installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(24)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "App streaming",
            color = SmartSlateTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Open ${state.serverUrl} in your laptop browser with PIN ${state.pairingPin} to stream and launch any app.",
            color = SmartInkMuted,
            fontSize = 13.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCopyUrl,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SmartBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy URL", color = SmartInk)
            }
            OutlinedButton(
                onClick = onCopyPin,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SmartBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("PIN: ${state.pairingPin}", color = SmartInk, fontFamily = JetBrainsMonoFontFamily)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search installed apps...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SmartBlue,
                unfocusedBorderColor = SmartBorder,
                focusedContainerColor = SmartSurfaceElevated,
                unfocusedContainerColor = SmartSurfaceElevated
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                Surface(
                    onClick = { onLaunchApp(app.packageName, app.label) },
                    shape = RoundedCornerShape(16.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, color = SmartInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(app.packageName, color = SmartInkMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Open", color = SmartBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileDesktopSheetContent(
    state: DeckLinkUiState,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onRotatePin: () -> Unit,
    onCopyUsbCommand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mobile desktop", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Open the address below in any browser on your Windows, Mac, or Linux laptop on the same Wi-Fi.",
            color = SmartInkMuted,
            fontSize = 13.sp
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SmartSurfaceElevated,
            border = BorderStroke(1.dp, SmartBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Laptop Browser URL", color = SmartInkMuted, fontSize = 12.sp)
                Text(state.serverUrl, color = SmartInk, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider(color = SmartBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("6-Digit Pairing PIN", color = SmartInkMuted, fontSize = 12.sp)
                        Text(state.pairingPin, color = SmartSlateTitle, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 4.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onCopyPin) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy PIN", tint = SmartBlue)
                        }
                        IconButton(onClick = onRotatePin) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Rotate PIN", tint = SmartBlue)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = if (state.readiness.serverRunning) onStopHost else onStartHost,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.readiness.serverRunning) SmartSlateTitle else SmartBlue,
                    contentColor = PureWhite
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.readiness.serverRunning) "Stop Host" else "Start Host")
            }
            OutlinedButton(
                onClick = onCopyUrl,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SmartBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy URL", color = SmartInk)
            }
        }

        Surface(
            onClick = onCopyUsbCommand,
            shape = RoundedCornerShape(16.dp),
            color = SmartSurfaceElevated,
            border = BorderStroke(1.dp, SmartBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("USB Cable Bridge (Tap to copy)", color = SmartBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(state.adbForwardCommand, color = SmartInk, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp)
                Text("Then open http://127.0.0.1:${state.settings.port}", color = SmartInkMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SecurityPinSheetContent(
    state: DeckLinkUiState,
    onCopyPin: () -> Unit,
    onRotatePin: () -> Unit,
    onRevokeSession: (String) -> Unit,
    onRevokeAllSessions: () -> Unit,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit
) {
    val caps = state.settings.capabilities
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Security & PIN", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SmartSurfaceElevated,
            border = BorderStroke(1.dp, SmartBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Pairing PIN", color = SmartInkMuted, fontSize = 12.sp)
                    Text(state.pairingPin, color = SmartInk, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 4.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCopyPin, shape = RoundedCornerShape(12.dp)) {
                        Text("Copy", color = SmartInk)
                    }
                    OutlinedButton(onClick = onRotatePin, shape = RoundedCornerShape(12.dp)) {
                        Text("New", color = SmartBlue)
                    }
                }
            }
        }

        if (state.sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paired devices (${state.sessions.size})", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = onRevokeAllSessions, shape = RoundedCornerShape(12.dp)) {
                    Text("Disconnect All", color = SmartInk)
                }
            }
            state.sessions.forEach { session ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.clientLabel, color = SmartInk, fontWeight = FontWeight.SemiBold)
                            Text(session.clientIp, color = SmartInkMuted, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onRevokeSession(session.sessionId) }, shape = RoundedCornerShape(12.dp)) {
                            Text("Revoke", color = SmartInk)
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SmartSurfaceElevated,
            border = BorderStroke(1.dp, SmartBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                CapabilityRow("Screen share", caps.screenMirror) { v -> onUpdateCapability { it.copy(screenMirror = v) } }
                HorizontalDivider(color = SmartBorder)
                CapabilityRow("Phone audio to laptop", caps.audioForwarding) { v -> onUpdateCapability { it.copy(audioForwarding = v) } }
                HorizontalDivider(color = SmartBorder)
                CapabilityRow("Remote touch & typing", caps.remoteTouchAndInput) { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } }
                HorizontalDivider(color = SmartBorder)
                CapabilityRow("File vault & clipboard", caps.fileVault) { v -> onUpdateCapability { it.copy(fileVault = v, clipboardSync = v) } }
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SmartInk, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = SmartBlue
            )
        )
    }
}

@Composable
private fun DisplayControlSheetContent(
    state: DeckLinkUiState,
    onRequestScreenMirror: () -> Unit,
    onStopScreenMirror: () -> Unit,
    onSetMirrorRotation: (Int) -> Unit,
    onUpdateMirrorQuality: (MirrorQuality) -> Unit
) {
    val currentDeg = state.readiness.mirrorRotationDegrees
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Display and Control", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Rotate the mirrored phone screen on your laptop and adjust stream clarity.",
            color = SmartInkMuted,
            fontSize = 13.sp
        )

        Button(
            onClick = if (state.readiness.screenMirrorActive) onStopScreenMirror else onRequestScreenMirror,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.readiness.screenMirrorActive) SmartSlateTitle else SmartBlue,
                contentColor = PureWhite
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.readiness.screenMirrorActive) "Stop Screen Share" else "Start Screen Share")
        }

        Text("Mirror Rotation Angle", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0 to "0°", 90 to "90°", 180 to "180°", 270 to "270°").forEach { (deg, label) ->
                val selected = currentDeg == deg
                Surface(
                    onClick = { onSetMirrorRotation(deg) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) SmartBlueTint else SmartSurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) SmartBlue else SmartBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_rotate_$deg")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (selected) SmartBlue else SmartInk,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onSetMirrorRotation(currentDeg - 90) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Rotate Left -90°", color = SmartInk)
            }
            OutlinedButton(
                onClick = { onSetMirrorRotation(currentDeg + 90) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Rotate Right +90°", color = SmartInk)
            }
        }

        Text("Stream Quality", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MirrorQuality.entries.forEach { quality ->
                val selected = state.settings.mirrorQuality == quality
                Surface(
                    onClick = { onUpdateMirrorQuality(quality) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) SmartBlueTint else SmartSurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) SmartBlue else SmartBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = quality.label,
                            color = if (selected) SmartBlue else SmartInk,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
