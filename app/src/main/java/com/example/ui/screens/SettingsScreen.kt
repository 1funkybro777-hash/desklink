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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MirrorQuality
import com.example.ui.DeckLinkUiState

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
                        text = "Host & Security",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Configure local server port, live mirror streaming, and laptop access security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Network Port
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
                            text = "LOCAL SERVER PORT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { if (it.length <= 5) portInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Port (1024-65535)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("port_input_field")
                            )
                            Button(
                                onClick = { onUpdatePort(portInput) },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("apply_port_button")
                            ) {
                                Text("Apply")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Keep CPU Awake While Hosting",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Prevents Wi-Fi server sleep during active laptop sessions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.settings.keepAwakeWhileHosting,
                                onCheckedChange = onUpdateKeepAwake,
                                modifier = Modifier.testTag("keep_awake_switch")
                            )
                        }
                    }
                }
            }

            // Stream Tuning
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
                            text = "SCREEN MIRROR PERFORMANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Resolution & Compression",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MirrorQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = state.settings.mirrorQuality == quality,
                                    onClick = { onUpdateQuality(quality) },
                                    label = { Text(quality.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.testTag("quality_${quality.name.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = "Frame Rate Target",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15).forEach { fps ->
                                FilterChip(
                                    selected = state.settings.mirrorFps == fps,
                                    onClick = { onUpdateFps(fps) },
                                    label = { Text("$fps FPS") },
                                    modifier = Modifier.testTag("fps_$fps")
                                )
                            }
                        }
                    }
                }
            }

            // Security Safeguards & Emergency Revoke
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GppGood,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "ACTIVE SECURITY SAFEGUARDS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = "• Local-only Wi-Fi / USB hosting — zero third-party cloud relays.\n" +
                                "• 6-digit random Pairing PIN with automatic 60s lockout after 5 wrong guesses.\n" +
                                "• High-entropy Bearer session tokens kept in laptop memory only.\n" +
                                "• Sandboxed File Vault with strict path checks and per-module kill switches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onRevokeAllSessions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("revoke_all_sessions_button")
                        ) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_revoke_all))
                        }
                    }
                }
            }

            // Connection Guide
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HOW TO CONNECT FROM YOUR LAPTOP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Option 1 — Wi-Fi (Wireless):\n" +
                                "1. Connect both phone and laptop to the same Wi-Fi or phone hotspot.\n" +
                                "2. Tap Start Remote Host on the Deck tab.\n" +
                                "3. Open ${state.serverUrl} in your laptop browser and enter the 6-digit PIN.\n\n" +
                                "Option 2 — USB Cable (Ultra-Low Latency):\n" +
                                "1. Plug your phone into your laptop with USB Debugging enabled.\n" +
                                "2. Run: ${state.adbForwardCommand}\n" +
                                "3. Open http://127.0.0.1:${state.settings.port} in your laptop browser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
