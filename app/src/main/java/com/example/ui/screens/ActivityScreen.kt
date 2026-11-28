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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeckLinkUiState
import com.example.ui.components.SmartSectionHeaderWithLine
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    state: DeckLinkUiState,
    onBack: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("activity_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = SmartInk
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Help & Activity",
                            color = SmartSlateTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (state.logs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearLogs,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SmartBorder)
                        ) {
                            Text("Clear", color = SmartInk)
                        }
                    }
                }
            }

            item {
                SmartSectionHeaderWithLine(title = "Quick setup guide")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SmartSurfaceElevated,
                    border = BorderStroke(1.dp, SmartBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. Connect your phone and laptop to the same Wi-Fi (or use USB Cable Bridge).",
                            color = SmartInk,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "2. Tap 'App streaming' or 'Mobile desktop' on Home to start the PC Host.",
                            color = SmartInk,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "3. Open ${state.serverUrl} in your laptop browser and enter the 6-digit PIN.",
                            color = SmartInk,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "4. Tap 'Screen share' and 'Phone audio' to stream video and sound to your laptop.",
                            color = SmartInk,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                SmartSectionHeaderWithLine(title = "Recent activity (${state.logs.size})")
            }

            if (state.logs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SmartSurfaceElevated,
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No remote commands recorded yet. Pair a laptop to see live actions here.",
                            color = SmartInkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(state.logs, key = { it.id }) { entry ->
                    Surface(
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
                                Text(entry.action, color = SmartInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (entry.detail.isNotBlank()) {
                                    Text(entry.detail, color = SmartInkMuted, fontSize = 12.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = timeFormatter.format(Date(entry.timestampMs)),
                                    color = SmartInkMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = entry.status.name,
                                    color = SmartBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
