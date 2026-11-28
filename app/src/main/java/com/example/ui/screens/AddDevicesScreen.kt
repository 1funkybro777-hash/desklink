package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.DeckLinkUiState
import com.example.ui.components.ScannableQrCodeCard
import com.example.ui.components.SmartProfileAvatarButton
import com.example.ui.components.SmartSectionHeaderWithLine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated
import com.example.ui.theme.SmartTooltipBg

private enum class PairingSheetType {
    NONE,
    QR_CODE,
    PIN_AND_PC,
    USB_BRIDGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevicesScreen(
    state: DeckLinkUiState,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onStartHost: () -> Unit,
    onRequestScreenMirror: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyPin: () -> Unit,
    onRotatePin: () -> Unit,
    onCopyUsbCommand: () -> Unit,
    onRevokeSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTipBanner by remember { mutableStateOf(true) }
    var activeSheet by remember { mutableStateOf(PairingSheetType.NONE) }

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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Bar: Back arrow on left, Profile avatar on right (Exact match to Photo 4)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_devices_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = SmartInk
                        )
                    }

                    SmartProfileAvatarButton(
                        onClick = onOpenDrawer,
                        size = 38.dp,
                        modifier = Modifier.testTag("add_devices_profile_button")
                    )
                }
            }

            // Dismissible Slate Tooltip Callout (Exact match to Photo 4)
            item {
                AnimatedVisibility(visible = showTipBanner) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SmartTooltipBg,
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Start PC Host and scan the QR code or enter the 6-digit PIN in your laptop browser to pair devices",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { showTipBanner = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Dismiss tip",
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Large Display Title "Add devices" (Exact match to Photo 4)
            item {
                Text(
                    text = "Add devices",
                    color = SmartSlateTitle,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Section 1: Quick pairing (Exact match to Photo 4)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SmartSectionHeaderWithLine(title = "Quick pairing")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        QuickPairingTile(
                            icon = Icons.Outlined.Pin,
                            label = "Pairing PIN",
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                activeSheet = PairingSheetType.PIN_AND_PC
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_pair_pin_tile")
                        )
                        QuickPairingTile(
                            icon = Icons.Outlined.QrCode2,
                            label = "Show QR code",
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                activeSheet = PairingSheetType.QR_CODE
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_pair_qr_tile")
                        )
                    }
                }
            }

            // Section 2: First-time pairing (Exact 2x2 Grid match to Photo 4 with generated studio artwork)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SmartSectionHeaderWithLine(title = "First-time pairing")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DevicePairingTile(
                            imageRes = R.drawable.img_device_laptop_1795856433396,
                            title = "PC",
                            subtitle = null,
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                activeSheet = PairingSheetType.PIN_AND_PC
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pair_device_pc")
                        )

                        DevicePairingTile(
                            imageRes = R.drawable.img_device_tablet_1795856434161,
                            title = "Tablet",
                            subtitle = null,
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                activeSheet = PairingSheetType.QR_CODE
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pair_device_tablet")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DevicePairingTile(
                            imageRes = R.drawable.img_device_display_1795856434868,
                            title = "Display",
                            subtitle = "Mirror & rotate screen",
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                onRequestScreenMirror()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pair_device_display")
                        )

                        DevicePairingTile(
                            imageRes = R.drawable.img_device_phone_1795856475046,
                            title = "USB Cable",
                            subtitle = "Zero-lag ADB bridge",
                            onClick = {
                                if (!state.readiness.serverRunning) onStartHost()
                                activeSheet = PairingSheetType.USB_BRIDGE
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pair_device_usb")
                        )
                    }
                }
            }

            // Connected Devices (if any laptop is paired right now)
            if (state.sessions.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmartSectionHeaderWithLine(title = "Connected devices")
                        state.sessions.forEach { session ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SmartSurfaceElevated,
                                border = BorderStroke(1.dp, SmartBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.clientLabel, color = SmartInk, fontWeight = FontWeight.SemiBold)
                                        Text(session.clientIp, color = SmartInkMuted, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onRevokeSession(session.sessionId) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Disconnect", color = SmartInk)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeSheet != PairingSheetType.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = PairingSheetType.NONE },
            sheetState = sheetState,
            containerColor = SmartCanvas,
            contentColor = SmartInk
        ) {
            when (activeSheet) {
                PairingSheetType.QR_CODE -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Scan QR to open console", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scan from your tablet or open the URL on your laptop, then enter the 6-digit PIN below.",
                        color = SmartInkMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    ScannableQrCodeCard(content = state.serverUrl, size = 196.dp)
                    Text(
                        text = state.serverUrl,
                        color = SmartInk,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SmartSurfaceElevated,
                        border = BorderStroke(1.dp, SmartBorder)
                    ) {
                        Text(
                            text = "PIN: ${state.pairingPin}",
                            color = SmartSlateTitle,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 3.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onCopyUrl, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Text("Copy URL", color = SmartInk)
                        }
                        Button(
                            onClick = onCopyPin,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SmartBlue, contentColor = PureWhite),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copy PIN")
                        }
                    }
                }

                PairingSheetType.PIN_AND_PC -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Pair with PC browser", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "1. Connect phone and laptop to the same Wi-Fi.\n2. Open the URL below in Chrome, Edge, or Safari.\n3. Enter the 6-digit pairing PIN.",
                        color = SmartInkMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SmartSurfaceElevated,
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Console URL", color = SmartInkMuted, fontSize = 12.sp)
                            Text(state.serverUrl, color = SmartInk, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("6-Digit PIN", color = SmartInkMuted, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(state.pairingPin, color = SmartSlateTitle, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 4.sp)
                                Row {
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
                    Button(
                        onClick = onCopyUrl,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmartBlue, contentColor = PureWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Laptop URL")
                    }
                }

                PairingSheetType.USB_BRIDGE -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("USB Cable Bridge (Zero-Lag)", color = SmartSlateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Connect your phone via USB cable with USB Debugging enabled, run this command on your laptop, then open http://127.0.0.1:${state.settings.port}",
                        color = SmartInkMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SmartSurfaceElevated,
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.adbForwardCommand,
                            color = SmartInk,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Button(
                        onClick = onCopyUsbCommand,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmartBlue, contentColor = PureWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy ADB Command")
                    }
                }

                PairingSheetType.NONE -> Unit
            }
        }
    }
}

@Composable
private fun QuickPairingTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SmartCanvas,
        border = BorderStroke(1.dp, SmartBorder),
        modifier = modifier.height(114.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SmartInk,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = SmartInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DevicePairingTile(
    @DrawableRes imageRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SmartCanvas,
        border = BorderStroke(1.dp, SmartBorder),
        modifier = modifier.height(148.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(76.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = SmartInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = SmartInkMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
