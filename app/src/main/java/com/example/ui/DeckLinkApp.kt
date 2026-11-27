package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DeckScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.PeriwinkleSoft

private data class NavDestination(
    val tab: DeckTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(DeckTab.DECK, R.string.nav_deck, Icons.Rounded.Devices, Icons.Outlined.Devices, "nav_deck"),
    NavDestination(DeckTab.CONTROLS, R.string.nav_controls, Icons.Rounded.FolderShared, Icons.Outlined.FolderShared, "nav_controls"),
    NavDestination(DeckTab.ACTIVITY, R.string.nav_activity, Icons.Rounded.History, Icons.Outlined.History, "nav_activity"),
    NavDestination(DeckTab.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun DeckLinkApp(viewModel: DeckLinkViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != DeckTab.DECK) {
        viewModel.selectTab(DeckTab.DECK)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshReadinessAndTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startServer()
    }

    val startHostWithPermissionCheck: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.startServer()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.startServer()
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            viewModel.onMediaProjectionGranted(result.resultCode, data)
        }
    }

    val requestScreenMirror: () -> Unit = {
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importVaultFile(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importVaultFile(uri)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CobaltPrimary,
                                    selectedTextColor = CobaltPrimary,
                                    indicatorColor = PeriwinkleSoft
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                val contentModifier = Modifier.weight(1f)
                when (selectedTab) {
                    DeckTab.DECK -> DeckScreen(
                        state = state,
                        onStartHost = startHostWithPermissionCheck,
                        onStopHost = viewModel::stopServer,
                        onRequestScreenMirror = requestScreenMirror,
                        onStopScreenMirror = viewModel::stopScreenMirror,
                        onOpenAccessibilitySettings = {
                            runCatching { context.startActivity(viewModel.buildAccessibilitySettingsIntent()) }
                        },
                        onOpenNotificationSettings = {
                            runCatching { context.startActivity(viewModel.buildNotificationAccessSettingsIntent()) }
                        },
                        onOpenShareHub = { viewModel.selectTab(DeckTab.CONTROLS) },
                        onToggleTorch = viewModel::toggleTorchLocally,
                        onRunDeskMode = { viewModel.runMacroLocally("desk_mode") },
                        onLaunchApp = viewModel::launchAppLocally,
                        onRotatePin = viewModel::rotatePairingPin,
                        onCopyUrl = { viewModel.copyToClipboard("Console URL", state.serverUrl) },
                        onCopyAdbCommand = { viewModel.copyToClipboard("USB Forward Command", state.adbForwardCommand) },
                        onCopyPin = { viewModel.copyToClipboard("Pairing PIN", state.pairingPin) },
                        onRevokeSession = viewModel::revokeSession,
                        modifier = contentModifier
                    )

                    DeckTab.CONTROLS -> ControlsScreen(
                        state = state,
                        onUpdateCapability = viewModel::updateCapability,
                        onRunMacro = viewModel::runMacroLocally,
                        onStopFinderRing = viewModel::stopFinderRingLocally,
                        onPushClipboard = viewModel::pushClipboardText,
                        onCopyClipboard = { text -> viewModel.copyToClipboard("Clipboard Entry", text) },
                        onPickMediaForVault = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onPickDocumentForVault = {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        },
                        onShareVaultFile = { fileName ->
                            viewModel.buildShareVaultIntent(fileName)?.let { shareIntent ->
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
                                }
                            }
                        },
                        onDeleteVaultFile = viewModel::deleteVaultFile,
                        modifier = contentModifier
                    )

                    DeckTab.ACTIVITY -> ActivityScreen(
                        state = state,
                        onClearLogs = viewModel::clearLogs,
                        modifier = contentModifier
                    )

                    DeckTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onUpdatePort = viewModel::updatePort,
                        onUpdateQuality = viewModel::updateMirrorQuality,
                        onUpdateFps = viewModel::updateMirrorFps,
                        onUpdateKeepAwake = viewModel::updateKeepAwake,
                        onRevokeAllSessions = viewModel::revokeAllSessions,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}
