package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VaultFileItem
import com.example.ui.DeckLinkUiState
import com.example.ui.components.SmartProfileAvatarButton
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBlueLight
import com.example.ui.theme.SmartBlueTint
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartSearchPill
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated

@Composable
fun FilesScreen(
    state: DeckLinkUiState,
    onOpenDrawer: () -> Unit,
    onOpenAddDevices: () -> Unit,
    onPickMediaForVault: () -> Unit,
    onPickDocumentForVault: () -> Unit,
    onShareVaultFile: (String) -> Unit,
    onDeleteVaultFile: (String) -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onTogglePhoneAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<String?>(null) }

    val filteredFiles = remember(searchQuery, activeFilter, state.vaultFiles) {
        val q = searchQuery.trim().lowercase()
        state.vaultFiles.filter { file ->
            val matchesQuery = q.isEmpty() || file.name.lowercase().contains(q)
            val matchesCat = when (activeFilter) {
                "Images" -> file.mimeType.startsWith("image/")
                "Video" -> file.mimeType.startsWith("video/")
                "Documents" -> !file.mimeType.startsWith("image/") && !file.mimeType.startsWith("video/") && !file.mimeType.startsWith("audio/")
                "Audio" -> file.mimeType.startsWith("audio/")
                else -> true
            }
            matchesQuery && matchesCat
        }
    }

    val matchedApps = remember(searchQuery, state.installedApps) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else state.installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(6)
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Row: Avatar Circle + Pill Search Bar (Exact match to Photo 3)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SmartProfileAvatarButton(
                        onClick = onOpenDrawer,
                        size = 42.dp,
                        modifier = Modifier.testTag("files_avatar_drawer_button")
                    )

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = SmartSearchPill,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = SmartInkMuted,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search", color = SmartInkMuted, fontSize = 15.sp)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = SmartInk, fontSize = 15.sp),
                                    cursorBrush = SolidColor(SmartBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("files_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = SmartInkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Search Results (when user types in Search bar)
            if (searchQuery.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Search results (${matchedApps.size + filteredFiles.size})",
                            color = SmartSlateTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (matchedApps.isEmpty() && filteredFiles.isEmpty()) {
                            Text(
                                text = "No matching apps or shared files found.",
                                color = SmartInkMuted,
                                fontSize = 13.sp
                            )
                        } else {
                            matchedApps.forEach { app ->
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
                                            Text(app.label, color = SmartInk, fontWeight = FontWeight.SemiBold)
                                            Text("Phone app", color = SmartInkMuted, fontSize = 12.sp)
                                        }
                                        Text("Open", color = SmartBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                            filteredFiles.forEach { file ->
                                VaultFileRowCard(
                                    file = file,
                                    onShare = { onShareVaultFile(file.relativePath) },
                                    onDelete = { onDeleteVaultFile(file.relativePath) }
                                )
                            }
                        }
                    }
                }
            }

            // Top 2 Side-by-Side Featured Outlined Cards: Recent files | Gallery (Exact match to Photo 3)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeaturedFilesCard(
                        icon = Icons.Outlined.FolderCopy,
                        title = "Recent files",
                        subtitle = "View recent files from all devices",
                        isActive = activeFilter == "Recent",
                        onClick = {
                            if (state.vaultFiles.isEmpty()) {
                                onPickDocumentForVault()
                            } else {
                                activeFilter = if (activeFilter == "Recent") null else "Recent"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("files_recent_card")
                    )

                    FeaturedFilesCard(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = "Gallery",
                        subtitle = "Access photos from all devices",
                        isActive = activeFilter == "Images",
                        onClick = onPickMediaForVault,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("files_gallery_card")
                    )
                }
            }

            // "Files" Section Header + "+ Add a device" Outlined Pill Card (Exact match to Photo 3)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Files",
                            color = SmartInk,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (activeFilter != null) {
                            Surface(
                                onClick = { activeFilter = null },
                                shape = RoundedCornerShape(999.dp),
                                color = SmartBlueTint
                            ) {
                                Text(
                                    text = "$activeFilter ×",
                                    color = SmartBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenAddDevices,
                        shape = RoundedCornerShape(20.dp),
                        color = SmartCanvas,
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .testTag("files_add_device_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = SmartBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add a device",
                                color = SmartBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Shared Vault Files List (shown whenever files exist in Vault)
            if (filteredFiles.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shared vault (${filteredFiles.size})",
                                color = SmartSlateTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "+ Add file",
                                color = SmartBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onPickDocumentForVault() }
                                    .padding(4.dp)
                            )
                        }
                        filteredFiles.forEach { file ->
                            VaultFileRowCard(
                                file = file,
                                onShare = { onShareVaultFile(file.relativePath) },
                                onDelete = { onDeleteVaultFile(file.relativePath) }
                            )
                        }
                    }
                }
            }

            // "Shortcuts" Section Header + Unified Outlined List Card (Exact match to Photo 3)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Shortcuts",
                        color = SmartInk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SmartCanvas,
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ShortcutRowItem(
                                icon = Icons.Outlined.Image,
                                label = "Images",
                                trailingText = state.vaultFiles.count { it.mimeType.startsWith("image/") }
                                    .takeIf { it > 0 }?.toString(),
                                onClick = onPickMediaForVault,
                                testTag = "shortcut_images"
                            )
                            ShortcutRowItem(
                                icon = Icons.Outlined.Slideshow,
                                label = "Video",
                                trailingText = state.vaultFiles.count { it.mimeType.startsWith("video/") }
                                    .takeIf { it > 0 }?.toString(),
                                onClick = onPickMediaForVault,
                                testTag = "shortcut_video"
                            )
                            ShortcutRowItem(
                                icon = Icons.Outlined.Description,
                                label = "Documents",
                                trailingText = null,
                                onClick = onPickDocumentForVault,
                                testTag = "shortcut_documents"
                            )
                            ShortcutRowItem(
                                icon = Icons.Outlined.AudioFile,
                                label = "Audio",
                                trailingText = if (state.readiness.audioForwardingActive) "Live" else null,
                                onClick = onTogglePhoneAudio,
                                testTag = "shortcut_audio"
                            )
                            ShortcutRowItem(
                                icon = Icons.Outlined.Download,
                                label = "Downloads",
                                trailingText = state.vaultFiles.size.takeIf { it > 0 }?.toString(),
                                onClick = {
                                    if (state.vaultFiles.isEmpty()) onPickDocumentForVault()
                                    else activeFilter = null
                                },
                                testTag = "shortcut_downloads"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedFilesCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (isActive) SmartBlueTint else SmartCanvas,
        border = BorderStroke(1.dp, if (isActive) SmartBlue else SmartBorder),
        modifier = modifier.height(172.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SmartBlueTint,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = SmartBlueLight,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                color = SmartSlateTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SmartInkMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShortcutRowItem(
    icon: ImageVector,
    label: String,
    trailingText: String?,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SmartInkMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            color = SmartInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                color = SmartBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun VaultFileRowCard(
    file: VaultFileItem,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SmartSurfaceElevated,
        border = BorderStroke(1.dp, SmartBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = SmartInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(file.sizeBytes),
                    color = SmartInkMuted,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share ${file.name}", tint = SmartBlue)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${file.name}", tint = SmartInkMuted)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
