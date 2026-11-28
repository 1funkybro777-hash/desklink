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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

private data class NavDestination(
    val tab: DeckTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(DeckTab.DECK, "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_deck"),
    NavDestination(DeckTab.CONTROLS, "Features", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_controls"),
    NavDestination(DeckTab.ACTIVITY, "Activity", Icons.Rounded.History, Icons.Outlined.History, "nav_activity"),
    NavDestination(DeckTab.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun DeckLinkApp(viewModel: DeckLinkViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAutoAudio by remember { mutableStateOf(false) }

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
            val hasRecordAudio = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.onMediaProjectionGranted(
                resultCode = result.resultCode,
                data = data,
                autoStartAudio = pendingAutoAudio || hasRecordAudio
            )
        }
        pendingAutoAudio = false
    }

    val launchMediaProjectionConsent: (Boolean) -> Unit = { startAudioToo ->
        pendingAutoAudio = startAudioToo
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    // Runtime RECORD_AUDIO permission required by Android 10+ AudioPlaybackCaptureConfiguration
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (state.readiness.screenMirrorActive) {
                viewModel.startAudioForwarding()
            } else {
                launchMediaProjectionConsent(true)
            }
        }
    }

    val requestAudioForwarding: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (state.readiness.screenMirrorActive) {
                viewModel.startAudioForwarding()
            } else {
                launchMediaProjectionConsent(true)
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
            containerColor = PureWhite,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Surface(
                        color = PureWhite,
                        border = BorderStroke(1.5.dp, PureBlack),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = PureWhite,
                            contentColor = PureBlack,
                            actionColor = PureBlack
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    androidx.compose.foundation.layout.Column {
                        HorizontalDivider(color = PureBlack, thickness = 1.dp)
                        NavigationBar(
                            containerColor = PureWhite,
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
                                            contentDescription = dest.label,
                                            tint = PureBlack
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            color = PureBlack,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PureBlack,
                                        selectedTextColor = PureBlack,
                                        unselectedIconColor = PureBlack,
                                        unselectedTextColor = PureBlack,
                                        indicatorColor = PureWhite
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
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
                    NavigationRail(containerColor = PureWhite) {
                        Destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                        tint = PureBlack
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        color = PureBlack,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureBlack,
                                    unselectedIconColor = PureBlack,
                                    unselectedTextColor = PureBlack,
                                    indicatorColor = PureWhite
                                ),
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
                        onRequestScreenMirror = { launchMediaProjectionConsent(false) },
                        onStopScreenMirror = viewModel::stopScreenMirror,
                        onStartAudioForwarding = requestAudioForwarding,
                        onStopAudioForwarding = viewModel::stopAudioForwarding,
                        onSetMirrorRotation = viewModel::setMirrorRotation,
                        onOpenShareHub = { viewModel.selectTab(DeckTab.CONTROLS) },
                        onLaunchApp = viewModel::launchAppLocally,
                        onRotatePin = viewModel::rotatePairingPin,
                        onCopyUrl = { viewModel.copyToClipboard("Console URL", state.serverUrl) },
                        onCopyPin = { viewModel.copyToClipboard("Pairing PIN", state.pairingPin) },
                        onRevokeSession = viewModel::revokeSession,
                        modifier = contentModifier
                    )

                    DeckTab.CONTROLS -> ControlsScreen(
                        state = state,
                        onUpdateCapability = viewModel::updateCapability,
                        onOpenAccessibilitySettings = {
                            runCatching { context.startActivity(viewModel.buildAccessibilitySettingsIntent()) }
                        },
                        onOpenNotificationSettings = {
                            runCatching { context.startActivity(viewModel.buildNotificationAccessSettingsIntent()) }
                        },
                        onToggleTorch = viewModel::toggleTorchLocally,
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
                        onCopyUsbCommand = { viewModel.copyToClipboard("USB Forward Command", state.adbForwardCommand) },
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
