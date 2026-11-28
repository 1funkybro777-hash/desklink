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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MirrorQuality
import com.example.model.RemoteCapabilities
import com.example.ui.DeckLinkUiState
import com.example.ui.components.SmartSectionHeaderWithLine
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBlueTint
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: DeckLinkUiState,
    onBack: () -> Unit,
    onUpdatePort: (String) -> Unit,
    onUpdateQuality: (MirrorQuality) -> Unit,
    onUpdateFps: (Int) -> Unit,
    onUpdateKeepAwake: (Boolean) -> Unit,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onRevokeAllSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var portInput by remember(state.settings.port) { mutableStateOf(state.settings.port.toString()) }
    val caps = state.settings.capabilities

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SmartInk
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        color = SmartSlateTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Network & Server Port
            item {
                SmartSectionHeaderWithLine(title = "Host server")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it.filter { ch -> ch.isDigit() }.take(5) },
                                label = { Text("Local Port (1024-65535)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SmartBlue,
                                    unfocusedBorderColor = SmartBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onUpdatePort(portInput) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SmartBlue, contentColor = PureWhite),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("settings_save_port_button")
                            ) {
                                Text("Apply")
                            }
                        }

                        HorizontalDivider(color = SmartBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Keep screen awake while hosting", color = SmartInk, fontWeight = FontWeight.Medium)
                                Text("Prevents standby drops during screen mirroring", color = SmartInkMuted, fontSize = 12.sp)
                            }
                            Switch(
                                checked = state.settings.keepAwakeWhileHosting,
                                onCheckedChange = onUpdateKeepAwake,
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = SmartBlue)
                            )
                        }
                    }
                }
            }

            // Stream Quality & FPS
            item {
                SmartSectionHeaderWithLine(title = "Mirror & stream quality")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MirrorQuality.entries.forEach { quality ->
                                val selected = state.settings.mirrorQuality == quality
                                Surface(
                                    onClick = { onUpdateQuality(quality) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) SmartBlueTint else SmartCanvas,
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

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Target Frame Rate", color = SmartInk, fontSize = 14.sp)
                                Text("${state.settings.mirrorFps} FPS", color = SmartBlue, fontWeight = FontWeight.SemiBold)
                            }
                            Slider(
                                value = state.settings.mirrorFps.toFloat(),
                                onValueChange = { onUpdateFps(it.roundToInt()) },
                                valueRange = 3f..20f,
                                colors = SliderDefaults.colors(thumbColor = SmartBlue, activeTrackColor = SmartBlue)
                            )
                        }
                    }
                }
            }

            // Remote Permissions
            item {
                SmartSectionHeaderWithLine(title = "Remote permissions")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SettingSwitchRow("Screen share & rotation", caps.screenMirror) { v -> onUpdateCapability { it.copy(screenMirror = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Phone audio to laptop", caps.audioForwarding) { v -> onUpdateCapability { it.copy(audioForwarding = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Remote touch & keyboard", caps.remoteTouchAndInput) { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Volume, ringer & media", caps.audioAndMedia) { v -> onUpdateCapability { it.copy(audioAndMedia = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("App streaming launcher", caps.appLauncher) { v -> onUpdateCapability { it.copy(appLauncher = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("File vault transfers", caps.fileVault) { v -> onUpdateCapability { it.copy(fileVault = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Clipboard sync", caps.clipboardSync) { v -> onUpdateCapability { it.copy(clipboardSync = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Notifications bridge", caps.notificationBridge) { v -> onUpdateCapability { it.copy(notificationBridge = v) } }
                        HorizontalDivider(color = SmartBorder)
                        SettingSwitchRow("Hardware actions & macros", caps.hardwareActions) { v -> onUpdateCapability { it.copy(hardwareActions = v) } }
                    }
                }
            }

            // Security Reset
            item {
                OutlinedButton(
                    onClick = onRevokeAllSessions,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_revoke_all_button")
                ) {
                    Text("Disconnect all paired devices", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
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
            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = SmartBlue)
        )
    }
}
