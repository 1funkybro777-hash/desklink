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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddDevicesScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DeckScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartAvatarBg
import com.example.ui.theme.SmartBlue
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartCanvas
import com.example.ui.theme.SmartInk
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartPlusCircle
import com.example.ui.theme.SmartSlateTitle
import com.example.ui.theme.SmartSurfaceElevated
import kotlinx.coroutines.launch

@Composable
fun DeckLinkApp(viewModel: DeckLinkViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var pendingAutoAudio by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUsbDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen || selectedTab != DeckTab.HOME) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.selectTab(DeckTab.HOME)
        }
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

    val openDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }

    val navigateFromDrawer: (DeckTab) -> Unit = { tab ->
        coroutineScope.launch {
            drawerState.close()
            viewModel.selectTab(tab)
        }
    }

    // Slide-out Navigation Drawer (Exact match to uploaded Photo 2)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SmartCanvas,
                drawerContentColor = SmartInk,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Drawer Profile Header (Exact match to Photo 2: Avatar + Title + Status)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                if (state.readiness.serverRunning) {
                                    viewModel.selectTab(DeckTab.ADD_DEVICES)
                                } else {
                                    startHostWithPermissionCheck()
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("drawer_header_host")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SmartAvatarBg,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "DeckLink host profile",
                                    tint = SmartSlateTitle,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "DeckLink Host",
                                color = SmartSlateTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (state.readiness.serverRunning) {
                                    "Online · :${state.settings.port}"
                                } else {
                                    "Tap to start host"
                                },
                                color = SmartInkMuted,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Upper Navigation Items (Exact match to Photo 2)
                    DrawerNavRow(
                        icon = Icons.Outlined.Home,
                        label = "Home",
                        selected = selectedTab == DeckTab.HOME,
                        onClick = { navigateFromDrawer(DeckTab.HOME) },
                        testTag = "drawer_nav_home"
                    )
                    DrawerNavRow(
                        icon = Icons.Outlined.Devices,
                        label = "Devices",
                        selected = selectedTab == DeckTab.ADD_DEVICES,
                        onClick = { navigateFromDrawer(DeckTab.ADD_DEVICES) },
                        testTag = "drawer_nav_devices"
                    )
                    DrawerNavRow(
                        icon = Icons.Outlined.PanTool,
                        label = "Actions and gestures",
                        selected = selectedTab == DeckTab.ACTIONS,
                        onClick = { navigateFromDrawer(DeckTab.ACTIONS) },
                        testTag = "drawer_nav_actions"
                    )
                    DrawerNavRow(
                        icon = Icons.Outlined.Settings,
                        label = "Settings",
                        selected = selectedTab == DeckTab.SETTINGS,
                        onClick = { navigateFromDrawer(DeckTab.SETTINGS) },
                        testTag = "drawer_nav_settings"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Pinned Items (Exact match to Photo 2: Help, About, USB Bridge)
                    DrawerFooterRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        label = "Help",
                        onClick = { navigateFromDrawer(DeckTab.ACTIVITY) },
                        testTag = "drawer_footer_help"
                    )
                    DrawerFooterRow(
                        icon = Icons.Outlined.Info,
                        label = "About",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        testTag = "drawer_footer_about"
                    )
                    DrawerFooterRow(
                        icon = Icons.Outlined.Usb,
                        label = "USB Bridge",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showUsbDialog = true
                        },
                        testTag = "drawer_footer_usb"
                    )
                }
            }
        }
    ) {
        val showBottomBar = selectedTab == DeckTab.HOME || selectedTab == DeckTab.FILES

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = SmartCanvas,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Surface(
                        color = SmartSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SmartBorder),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = SmartSurfaceElevated,
                            contentColor = SmartInk,
                            actionColor = SmartBlue
                        )
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    SmartThreeIconBottomBar(
                        selectedTab = selectedTab,
                        onSelectHome = { viewModel.selectTab(DeckTab.HOME) },
                        onSelectFiles = { viewModel.selectTab(DeckTab.FILES) },
                        onClickAddDevice = { viewModel.selectTab(DeckTab.ADD_DEVICES) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    DeckTab.HOME -> DeckScreen(
                        state = state,
                        onOpenDrawer = openDrawer,
                        onStartHost = startHostWithPermissionCheck,
                        onStopHost = viewModel::stopServer,
                        onRequestScreenMirror = { launchMediaProjectionConsent(false) },
                        onStopScreenMirror = viewModel::stopScreenMirror,
                        onStartAudioForwarding = requestAudioForwarding,
                        onStopAudioForwarding = viewModel::stopAudioForwarding,
                        onSetMirrorRotation = viewModel::setMirrorRotation,
                        onUpdateMirrorQuality = viewModel::updateMirrorQuality,
                        onUpdateCapability = viewModel::updateCapability,
                        onOpenActions = { viewModel.selectTab(DeckTab.ACTIONS) },
                        onOpenActivity = { viewModel.selectTab(DeckTab.ACTIVITY) },
                        onOpenSettings = { viewModel.selectTab(DeckTab.SETTINGS) },
                        onLaunchApp = viewModel::launchAppLocally,
                        onRotatePin = viewModel::rotatePairingPin,
                        onCopyUrl = { viewModel.copyToClipboard("Console URL", state.serverUrl) },
                        onCopyPin = { viewModel.copyToClipboard("Pairing PIN", state.pairingPin) },
                        onCopyUsbCommand = { viewModel.copyToClipboard("USB Command", state.adbForwardCommand) },
                        onRevokeSession = viewModel::revokeSession,
                        onRevokeAllSessions = viewModel::revokeAllSessions
                    )

                    DeckTab.FILES -> FilesScreen(
                        state = state,
                        onOpenDrawer = openDrawer,
                        onOpenAddDevices = { viewModel.selectTab(DeckTab.ADD_DEVICES) },
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
                        onLaunchApp = viewModel::launchAppLocally,
                        onTogglePhoneAudio = {
                            if (state.readiness.audioForwardingActive) viewModel.stopAudioForwarding()
                            else requestAudioForwarding()
                        }
                    )

                    DeckTab.ADD_DEVICES -> AddDevicesScreen(
                        state = state,
                        onBack = { viewModel.selectTab(DeckTab.HOME) },
                        onOpenDrawer = openDrawer,
                        onStartHost = startHostWithPermissionCheck,
                        onRequestScreenMirror = { launchMediaProjectionConsent(false) },
                        onCopyUrl = { viewModel.copyToClipboard("Console URL", state.serverUrl) },
                        onCopyPin = { viewModel.copyToClipboard("Pairing PIN", state.pairingPin) },
                        onRotatePin = viewModel::rotatePairingPin,
                        onCopyUsbCommand = { viewModel.copyToClipboard("USB Command", state.adbForwardCommand) },
                        onRevokeSession = viewModel::revokeSession
                    )

                    DeckTab.ACTIONS -> ControlsScreen(
                        state = state,
                        onBack = { viewModel.selectTab(DeckTab.HOME) },
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
                        onCopyClipboard = { text -> viewModel.copyToClipboard("Clipboard Entry", text) }
                    )

                    DeckTab.ACTIVITY -> ActivityScreen(
                        state = state,
                        onBack = { viewModel.selectTab(DeckTab.HOME) },
                        onClearLogs = viewModel::clearLogs
                    )

                    DeckTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onBack = { viewModel.selectTab(DeckTab.HOME) },
                        onUpdatePort = viewModel::updatePort,
                        onUpdateQuality = viewModel::updateMirrorQuality,
                        onUpdateFps = viewModel::updateMirrorFps,
                        onUpdateKeepAwake = viewModel::updateKeepAwake,
                        onUpdateCapability = viewModel::updateCapability,
                        onRevokeAllSessions = viewModel::revokeAllSessions
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = SmartSurfaceElevated,
            title = { Text("About DeckLink", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Device: ${state.telemetry.deviceModel}", color = SmartInk, fontSize = 14.sp)
                    Text("Android: ${state.telemetry.androidVersion}", color = SmartInkMuted, fontSize = 13.sp)
                    Text("Local IP: ${state.telemetry.ipAddress}", color = SmartInkMuted, fontSize = 13.sp)
                    Text("Battery: ${state.telemetry.batteryPercent}% (${"%.1f".format(state.telemetry.batteryTempC)}°C)", color = SmartInkMuted, fontSize = 13.sp)
                    Text("RAM: ${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB", color = SmartInkMuted, fontSize = 13.sp)
                    Text("Storage: ${"%.1f".format(state.telemetry.storageUsedGb)} / ${"%.1f".format(state.telemetry.storageTotalGb)} GB", color = SmartInkMuted, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = SmartBlue)
                }
            }
        )
    }

    if (showUsbDialog) {
        AlertDialog(
            onDismissRequest = { showUsbDialog = false },
            containerColor = SmartSurfaceElevated,
            title = { Text("USB Cable Bridge", color = SmartSlateTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Connect your phone via USB cable and run this command on your laptop:",
                        color = SmartInkMuted,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SmartCanvas,
                        border = BorderStroke(1.dp, SmartBorder)
                    ) {
                        Text(
                            text = state.adbForwardCommand,
                            color = SmartInk,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        "Then open http://127.0.0.1:${state.settings.port} with PIN ${state.pairingPin}.",
                        color = SmartInkMuted,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.copyToClipboard("USB Command", state.adbForwardCommand)
                        showUsbDialog = false
                    }
                ) {
                    Text("Copy Command", color = SmartBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsbDialog = false }) {
                    Text("Close", color = SmartInkMuted)
                }
            }
        )
    }
}

/**
 * Exact 3-Icon Bottom Navigation Bar from uploaded Photos 1 & 3:
 * Home icon | Folder icon | Soft circular "+" button
 */
@Composable
private fun SmartThreeIconBottomBar(
    selectedTab: DeckTab,
    onSelectHome: () -> Unit,
    onSelectFiles: () -> Unit,
    onClickAddDevice: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SmartCanvas)
            .navigationBarsPadding()
            .padding(horizontal = 36.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSelectHome,
            modifier = Modifier
                .size(48.dp)
                .testTag("bottom_nav_home")
        ) {
            Icon(
                imageVector = if (selectedTab == DeckTab.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                contentDescription = "Home dashboard",
                tint = if (selectedTab == DeckTab.HOME) SmartInk else SmartInkMuted,
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(
            onClick = onSelectFiles,
            modifier = Modifier
                .size(48.dp)
                .testTag("bottom_nav_files")
        ) {
            Icon(
                imageVector = if (selectedTab == DeckTab.FILES) Icons.Rounded.Folder else Icons.Outlined.Folder,
                contentDescription = "Files and Share Hub",
                tint = if (selectedTab == DeckTab.FILES) SmartInk else SmartInkMuted,
                modifier = Modifier.size(26.dp)
            )
        }

        Surface(
            onClick = onClickAddDevice,
            shape = CircleShape,
            color = SmartPlusCircle,
            modifier = Modifier
                .size(48.dp)
                .testTag("bottom_nav_add_device")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add devices",
                    tint = SmartInk,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) SmartBlue else SmartSlateTitle,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = if (selected) SmartBlue else SmartSlateTitle,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun DrawerFooterRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SmartInkMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = SmartInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
