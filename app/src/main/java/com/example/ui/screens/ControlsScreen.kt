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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemoteCapabilities
import com.example.ui.DeckLinkUiState
import com.example.ui.components.SmartSectionHeaderWithLine
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartLiveGreen
import com.example.ui.theme.SmartLiveGreenTint
import com.example.ui.theme.SmartSearchPill
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated

@Composable
fun ControlsScreen(
    state: DeckLinkUiState,
    onBack: () -> Unit,
    onUpdateCapability: ((RemoteCapabilities) -> RemoteCapabilities) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggleTorch: () -> Unit,
    onRunMacro: (String) -> Unit,
    onStopFinderRing: () -> Unit,
    onPushClipboard: (String) -> Unit,
    onCopyClipboard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var clipboardDraft by remember { mutableStateOf("") }
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
                        modifier = Modifier.testTag("actions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SmartInk
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Actions and gestures",
                        color = SmartSlateTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Remote Touch & Accessibility
            item {
                SmartSectionHeaderWithLine(title = "Remote touch & keys")
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accessibility touch service", color = SmartInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "Enables mouse taps, rotated swipes, keyboard typing, and Back/Home keys from laptop.",
                                    color = SmartInkMuted,
                                    fontSize = 12.sp
                                )
                            }
                            StatusPill(
                                text = if (state.readiness.accessibilityConnected) "Active" else "Off",
                                active = state.readiness.accessibilityConnected
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenAccessibilitySettings,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, SmartBorder),
                                modifier = Modifier.testTag("open_accessibility_settings")
                            ) {
                                Text("Open Accessibility Settings", color = SmartBlue)
                            }
                            Switch(
                                checked = caps.remoteTouchAndInput,
                                onCheckedChange = { v -> onUpdateCapability { it.copy(remoteTouchAndInput = v) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = SmartBlue)
                            )
                        }
                    }
                }
            }

            // Notification Bridge
            item {
                SmartSectionHeaderWithLine(title = "Notification bridge")
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mirror phone notifications", color = SmartInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "View live phone alerts on your laptop and dismiss them remotely.",
                                    color = SmartInkMuted,
                                    fontSize = 12.sp
                                )
                            }
                            StatusPill(
                                text = if (state.readiness.notificationListenerConnected) "Active" else "Off",
                                active = state.readiness.notificationListenerConnected
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenNotificationSettings,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, SmartBorder),
                                modifier = Modifier.testTag("open_notification_settings")
                            ) {
                                Text("Open Notification Access", color = SmartBlue)
                            }
                            Switch(
                                checked = caps.notificationBridge,
                                onCheckedChange = { v -> onUpdateCapability { it.copy(notificationBridge = v) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = SmartBlue)
                            )
                        }
                    }
                }
            }

            // Hardware & Automation Macros
            item {
                SmartSectionHeaderWithLine(title = "Hardware & macros")
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
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onToggleTorch,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.readiness.torchEnabled) SmartBlue else SmartSearchPill,
                                    contentColor = if (state.readiness.torchEnabled) PureWhite else SmartInk
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("actions_torch_button")
                            ) {
                                Text(if (state.readiness.torchEnabled) "Flashlight: ON" else "Flashlight: OFF")
                            }

                            if (state.readiness.finderRingActive) {
                                Button(
                                    onClick = onStopFinderRing,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SmartSlateTitle, contentColor = PureWhite),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Stop Ringing")
                                }
                            }
                        }

                        HorizontalDivider(color = SmartBorder)

                        state.macros.forEach { macro ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(macro.title, color = SmartInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(macro.subtitle, color = SmartInkMuted, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onRunMacro(macro.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SmartBorder)
                                ) {
                                    Text("Run", color = SmartBlue)
                                }
                            }
                        }
                    }
                }
            }

            // Clipboard Sync
            item {
                SmartSectionHeaderWithLine(title = "Clipboard sync")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = clipboardDraft,
                            onValueChange = { clipboardDraft = it },
                            placeholder = { Text("Paste text to sync with laptop...") },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SmartBlue,
                                unfocusedBorderColor = SmartBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (clipboardDraft.isNotBlank()) {
                                    onPushClipboard(clipboardDraft)
                                    clipboardDraft = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SmartBlue, contentColor = PureWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save to Shared Clipboard")
                        }

                        state.clipboardHistory.take(4).forEach { entry ->
                            Surface(
                                onClick = { onCopyClipboard(entry.text) },
                                shape = RoundedCornerShape(12.dp),
                                color = SmartCanvas,
                                border = BorderStroke(1.dp, SmartBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(entry.source, color = SmartBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(entry.text, color = SmartInk, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) SmartLiveGreenTint else SmartSearchPill
    ) {
        Text(
            text = text,
            color = if (active) SmartLiveGreen else SmartInkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
