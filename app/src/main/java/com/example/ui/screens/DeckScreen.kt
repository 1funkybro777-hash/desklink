package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LaptopSession
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.DeepNavyInk
import com.example.ui.theme.FileArchivePurple
import com.example.ui.theme.FileMediaBlue
import com.example.ui.theme.FilePdfRed
import com.example.ui.theme.FileSheetOlive
import com.example.ui.theme.FileSlideOrange
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.PeriwinkleHero
import com.example.ui.theme.PeriwinkleSoft
import com.example.ui.theme.WarmStoneTile

@Composable
fun DeckScreen(
    state: DeckLinkUiState,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onRequestScreenMirror: () -> Unit,
    onStopScreenMirror: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenShareHub: () -> Unit,
    onToggleTorch: () -> Unit,
    onRunDeskMode: () -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onRotatePin: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyAdbCommand: () -> Unit,
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
    val primarySession = state.sessions.firstOrNull()

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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Pill Search Bar ("Search everywhere") + Brand Avatar
            item {
                SearchEverywhereHeader(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )
            }

            // Instant Search Results Card (inspired by Screenshot 1)
            item {
                AnimatedVisibility(visible = trimmedQuery.isNotEmpty()) {
                    SearchResultsCard(
                        deviceModel = state.telemetry.deviceModel,
                        apps = matchingApps,
                        files = matchingFiles,
                        onLaunchApp = onLaunchApp,
                        onOpenShareHub = onOpenShareHub
                    )
                }
            }

            // Centered Connected Laptop Hero Card (inspired by Screenshots 2 & 4)
            item {
                ConnectedDeviceHeroCard(
                    state = state,
                    activeSession = primarySession,
                    onStartHost = onStartHost,
                    onStopHost = onStopHost,
                    onCopyUrl = onCopyUrl,
                    onCopyPin = onCopyPin,
                    onRotatePin = onRotatePin
                )
            }

            // Full-Width Signature Periwinkle Primary Action Button (Screenshot 2 style)
            item {
                PeriwinkleHeroActionButton(
                    mirrorActive = state.readiness.screenMirrorActive,
                    onClick = {
                        if (state.readiness.screenMirrorActive) onStopScreenMirror()
                        else onRequestScreenMirror()
                    }
                )
            }

            // 2-Column Warm-Stone Tile Grid (Screenshot 2 style)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ControlTileCard(
                            icon = Icons.Rounded.TouchApp,
                            title = "Cross Control",
                            subtitle = if (state.readiness.accessibilityConnected) "Touch active" else "Tap to enable",
                            isActive = state.readiness.accessibilityConnected,
                            testTag = "accessibility_setup_button",
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.weight(1f)
                        )
                        ControlTileCard(
                            icon = Icons.Rounded.FolderShared,
                            title = "Share Hub",
                            subtitle = "${state.vaultFiles.size} vault files",
                            isActive = state.vaultFiles.isNotEmpty(),
                            testTag = "open_share_hub_tile",
                            onClick = onOpenShareHub,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ControlTileCard(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Notifications",
                            subtitle = if (state.readiness.notificationListenerConnected) "Bridge active" else "Tap to enable",
                            isActive = state.readiness.notificationListenerConnected,
                            testTag = "notification_setup_button",
                            onClick = onOpenNotificationSettings,
                            modifier = Modifier.weight(1f)
                        )
                        ControlTileCard(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Desk Mode",
                            subtitle = "Vibrate + 35% vol",
                            isActive = false,
                            testTag = "desk_mode_tile",
                            onClick = onRunDeskMode,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ControlTileCard(
                            icon = if (state.readiness.torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                            title = "Flashlight",
                            subtitle = if (state.readiness.torchEnabled) "Light ON" else "Light OFF",
                            isActive = state.readiness.torchEnabled,
                            testTag = "flashlight_tile",
                            onClick = onToggleTorch,
                            modifier = Modifier.weight(1f)
                        )
                        ControlTileCard(
                            icon = Icons.Rounded.Usb,
                            title = "USB Link",
                            subtitle = "Copy ADB cmd",
                            isActive = false,
                            testTag = "copy_usb_command_button",
                            onClick = onCopyAdbCommand,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Paired Laptops Section (Screenshot 4 card style)
            if (state.sessions.isNotEmpty()) {
                item {
                    PairedDevicesCard(
                        sessions = state.sessions,
                        onRevokeSession = onRevokeSession
                    )
                }
            }

            // Compact Phone Telemetry Card
            item {
                PhoneTelemetryStrip(state = state)
            }
        }

        // Floating Pale-Blue Activity Pill at Bottom-Right (directly inspired by Screenshot 2's "× Stop activities")
        Surface(
            onClick = { if (isOnline) onStopHost() else onStartHost() },
            shape = CircleShape,
            color = if (isOnline) PeriwinkleSoft else PeriwinkleHero,
            contentColor = DeepNavyInk,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.22f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 14.dp)
                .height(48.dp)
                .testTag(if (isOnline) "stop_server_button" else "start_server_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isOnline) stringResource(R.string.action_stop_server) else stringResource(R.string.action_start_server),
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepNavyInk
                )
            }
        }
    }
}

@Composable
private fun SearchEverywhereHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_decklink_icon),
            contentDescription = stringResource(R.string.cd_app_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
        )

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps and files...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_everywhere_input")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsCard(
    deviceModel: String,
    apps: List<com.example.model.LaunchableApp>,
    files: List<com.example.model.VaultFileItem>,
    onLaunchApp: (String, String) -> Unit,
    onOpenShareHub: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = deviceModel.ifBlank { "This Phone" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (apps.isEmpty() && files.isEmpty()) {
                Text(
                    text = "No matching apps or Share Hub files found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (apps.isNotEmpty()) {
                Text(
                    text = "APPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                apps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onLaunchApp(app.packageName, app.label) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = PeriwinkleSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (files.isNotEmpty()) {
                Text(
                    text = "SHARE HUB FILES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                files.forEach { file ->
                    val badgeColor = fileBadgeColorForName(file.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onOpenShareHub() }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.InsertDriveFile,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedDeviceHeroCard(
    state: DeckLinkUiState,
    activeSession: LaptopSession?,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onRotatePin: () -> Unit
) {
    val isOnline = state.readiness.serverRunning

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Custom Crisp Laptop Hardware Illustration (inspired by Screenshot 2 & 4)
            LaptopIllustration(isConnected = isOnline)

            Text(
                text = activeSession?.clientLabel ?: if (isOnline) "Ready for Laptop" else "Connect Your Laptop",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Compact inline status row (blue connection icon + Wi-Fi + battery)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isOnline) CobaltPrimary.copy(alpha = 0.12f) else WarmStoneTile,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (activeSession != null) "Connected" else if (isOnline) "Host Online" else "Standby",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = state.telemetry.networkType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = LiveGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${state.telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Warm-stone compact address + PIN strip
            Surface(
                color = WarmStoneTile,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OPEN ON LAPTOP BROWSER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.serverUrl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = CobaltPrimary,
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
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "6-DIGIT PAIRING PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.pairingPin.chunked(3).joinToString(" "),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onCopyPin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onRotatePin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rotate_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_pin),
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.readiness.serverError != null) {
                Text(
                    text = state.readiness.serverError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Crisp Connect / Disconnect Pill Button (matching Screenshot 4's clean pill button)
            OutlinedButton(
                onClick = { if (isOnline) onStopHost() else onStartHost() },
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = if (isOnline) "× Disconnect Host" else "Start Remote Host",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LaptopIllustration(isConnected: Boolean) {
    Canvas(
        modifier = Modifier
            .width(124.dp)
            .height(82.dp)
    ) {
        val w = size.width
        val h = size.height

        // Laptop lid bezel
        val lidLeft = w * 0.14f
        val lidWidth = w * 0.72f
        val lidHeight = h * 0.72f
        drawRoundRect(
            color = Color(0xFF1E242E),
            topLeft = Offset(lidLeft, 2f),
            size = Size(lidWidth, lidHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )

        // Inner vibrant wallpaper screen (Windows 11 bloom inspired blue ribbon)
        val inset = 4.dp.toPx()
        val screenTopLeft = Offset(lidLeft + inset, 2f + inset)
        val screenSize = Size(lidWidth - inset * 2, lidHeight - inset * 2)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = if (isConnected) {
                    listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1D4ED8))
                } else {
                    listOf(Color(0xFF93C5FD), Color(0xFF60A5FA), Color(0xFF3B82F6))
                },
                start = screenTopLeft,
                end = Offset(screenTopLeft.x + screenSize.width, screenTopLeft.y + screenSize.height)
            ),
            topLeft = screenTopLeft,
            size = screenSize,
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Abstract ribbon wave inside screen
        val wavePath = Path().apply {
            moveTo(screenTopLeft.x, screenTopLeft.y + screenSize.height * 0.65f)
            cubicTo(
                screenTopLeft.x + screenSize.width * 0.35f,
                screenTopLeft.y + screenSize.height * 0.15f,
                screenTopLeft.x + screenSize.width * 0.65f,
                screenTopLeft.y + screenSize.height * 0.95f,
                screenTopLeft.x + screenSize.width,
                screenTopLeft.y + screenSize.height * 0.45f
            )
            lineTo(screenTopLeft.x + screenSize.width, screenTopLeft.y + screenSize.height)
            lineTo(screenTopLeft.x, screenTopLeft.y + screenSize.height)
            close()
        }
        drawPath(
            path = wavePath,
            color = Color.White.copy(alpha = 0.24f)
        )

        // Metallic silver laptop keyboard base
        val baseTop = lidHeight + 4f
        drawRoundRect(
            color = Color(0xFFD6DBE4),
            topLeft = Offset(w * 0.04f, baseTop),
            size = Size(w * 0.92f, h * 0.14f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFFB4BDCB),
            topLeft = Offset(w * 0.38f, baseTop),
            size = Size(w * 0.24f, h * 0.06f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

@Composable
private fun PeriwinkleHeroActionButton(
    mirrorActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (mirrorActive) CobaltPrimary else PeriwinkleHero,
        contentColor = if (mirrorActive) Color.White else DeepNavyInk,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag("mirror_toggle_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Cast,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (mirrorActive) "Live Screen Mirror Active — Tap to Stop" else "Live Screen & App Streaming",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ControlTileCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = WarmStoneTile,
        border = BorderStroke(
            1.dp,
            if (isActive) CobaltPrimary.copy(alpha = 0.35f) else Color.Transparent
        ),
        modifier = modifier
            .height(106.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) CobaltPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LiveGreen)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PairedDevicesCard(
    sessions: List<LaptopSession>,
    onRevokeSession: (String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Paired Devices (${sessions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            sessions.forEach { session ->
                Surface(
                    color = WarmStoneTile,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.clientLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = session.clientIp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { onRevokeSession(session.sessionId) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("revoke_session_${session.sessionId}")
                        ) {
                            Text("× Disconnect", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneTelemetryStrip(state: DeckLinkUiState) {
    val t = state.telemetry
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryMiniStat("Battery", "${t.batteryPercent}%")
            TelemetryMiniStat("RAM", "${"%.1f".format(t.ramUsedMb / 1024f)}/${"%.0f".format(t.ramTotalMb / 1024f)}G")
            TelemetryMiniStat("Storage", "${"%.0f".format(t.storageUsedGb)}/${"%.0f".format(t.storageTotalGb)}G")
            TelemetryMiniStat("Temp", "${"%.1f".format(t.batteryTempC)}°C")
        }
    }
}

@Composable
private fun TelemetryMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun fileBadgeColorForName(name: String): Color {
    return when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> FilePdfRed
        "ppt", "pptx" -> FileSlideOrange
        "xls", "xlsx", "csv" -> FileSheetOlive
        "zip", "rar", "7z" -> FileArchivePurple
        else -> FileMediaBlue
    }
}
