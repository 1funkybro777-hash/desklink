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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.model.CommandLogEntry
import com.example.model.LogStatus
import com.example.ui.DeckLinkUiState
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    state: DeckLinkUiState,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<LogStatus?>(null) }
    val filteredLogs = remember(state.logs, selectedFilter) {
        if (selectedFilter == null) state.logs else state.logs.filter { it.status == selectedFilter }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activity Log",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureBlack
                        )
                        Text(
                            text = "Every command received from paired laptops.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureBlack
                        )
                    }
                    if (state.logs.isNotEmpty()) {
                        Surface(
                            onClick = onClearLogs,
                            color = PureWhite,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.2.dp, PureBlack),
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureBlack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        MonoChoiceChip(
                            label = "All (${state.logs.size})",
                            selected = selectedFilter == null,
                            testTag = "filter_all",
                            onClick = { selectedFilter = null },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    item {
                        MonoChoiceChip(
                            label = "Allowed",
                            selected = selectedFilter == LogStatus.ALLOWED,
                            testTag = "filter_allowed",
                            onClick = { selectedFilter = LogStatus.ALLOWED }
                        )
                    }
                    item {
                        MonoChoiceChip(
                            label = "Blocked",
                            selected = selectedFilter == LogStatus.BLOCKED,
                            testTag = "filter_blocked",
                            onClick = { selectedFilter = LogStatus.BLOCKED }
                        )
                    }
                    item {
                        MonoChoiceChip(
                            label = "Auth",
                            selected = selectedFilter == LogStatus.AUTH_FAILED,
                            testTag = "filter_auth",
                            onClick = { selectedFilter = LogStatus.AUTH_FAILED }
                        )
                    }
                    item {
                        MonoChoiceChip(
                            label = "System",
                            selected = selectedFilter == LogStatus.SYSTEM,
                            testTag = "filter_system",
                            onClick = { selectedFilter = LogStatus.SYSTEM }
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    MonoSectionCard(title = "AUDIT EVENTS") {
                        Text(
                            text = "No activity recorded yet. Start the Remote Host and connect your laptop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureBlack
                        )
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { entry ->
                    AuditLogRow(entry = entry, formattedTime = timeFmt.format(Date(entry.timestampMs)))
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun AuditLogRow(entry: CommandLogEntry, formattedTime: String) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PureBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = entry.action,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.clientIp} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureBlack
                )
            }
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, PureBlack)
            ) {
                Text(
                    text = entry.status.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
