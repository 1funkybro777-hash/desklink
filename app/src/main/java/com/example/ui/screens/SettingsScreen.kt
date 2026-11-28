package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.MirrorQuality
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun SettingsScreen(
    state: DeckLinkUiState,
    onUpdatePort: (String) -> Unit,
    onUpdateQuality: (MirrorQuality) -> Unit,
    onUpdateFps: (Int) -> Unit,
    onUpdateKeepAwake: (Boolean) -> Unit,
    onRevokeAllSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var portInput by remember(state.settings.port) { mutableStateOf(state.settings.port.toString()) }

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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureBlack
                    )
                    Text(
                        text = "Server port, streaming quality, and laptop security controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureBlack
                    )
                }
            }

            // SECTION 1: NETWORK PORT & WAKE LOCK
            item {
                MonoSectionCard(title = "1. SERVER PORT & POWER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { if (it.length <= 5) portInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Port (1024-65535)", color = PureBlack) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                .weight(1f)
                                .testTag("port_input_field")
                        )
                        MonoActionButton(
                            text = "Apply",
                            testTag = "apply_port_button",
                            onClick = { onUpdatePort(portInput) },
                            modifier = Modifier.weight(0.45f)
                        )
                    }

                    MonoSwitchRow(
                        title = "Keep Awake While Hosting",
                        subtitle = "Prevents Wi-Fi sleep while laptop is connected",
                        checked = state.settings.keepAwakeWhileHosting,
                        testTag = "keep_awake_switch",
                        onCheckedChange = onUpdateKeepAwake
                    )
                }
            }

            // SECTION 2: STREAMING QUALITY & FPS
            item {
                MonoSectionCard(title = "2. SCREEN MIRROR QUALITY & FPS") {
                    Text("Resolution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PureBlack)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MirrorQuality.entries.forEach { quality ->
                            MonoChoiceChip(
                                label = quality.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = state.settings.mirrorQuality == quality,
                                testTag = "quality_${quality.name.lowercase()}",
                                onClick = { onUpdateQuality(quality) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Target FPS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PureBlack)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { fps ->
                            MonoChoiceChip(
                                label = "$fps FPS",
                                selected = state.settings.mirrorFps == fps,
                                testTag = "fps_$fps",
                                onClick = { onUpdateFps(fps) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // SECTION 3: SECURITY & REVOKE
            item {
                MonoSectionCard(title = "3. PRIVACY & SECURITY") {
                    Text(
                        text = "• Local Wi-Fi & USB hosting only — no cloud servers.\n" +
                            "• 6-digit random Pairing PIN with 60s lockout after 5 wrong tries.\n" +
                            "• Memory-only laptop Bearer tokens revocable anytime.\n" +
                            "• Sandboxed File Vault with separate permission toggles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureBlack
                    )
                    MonoActionButton(
                        text = "Disconnect All Laptops",
                        emphasized = true,
                        testTag = "revoke_all_sessions_button",
                        onClick = onRevokeAllSessions
                    )
                }
            }

            // SECTION 4: CONNECTION GUIDE
            item {
                MonoSectionCard(title = "4. HOW TO CONNECT LAPTOP") {
                    Text(
                        text = "Wi-Fi:\n" +
                            "1. Connect phone and laptop to the same Wi-Fi or hotspot.\n" +
                            "2. Tap Start Remote Host on Home tab.\n" +
                            "3. Open ${state.serverUrl} in laptop browser and enter the 6-digit PIN.\n\n" +
                            "USB Cable:\n" +
                            "1. Plug phone via USB with USB Debugging enabled.\n" +
                            "2. Run: ${state.adbForwardCommand}\n" +
                            "3. Open http://127.0.0.1:${state.settings.port} on laptop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureBlack
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}
