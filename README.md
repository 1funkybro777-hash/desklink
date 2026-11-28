# DeckLink

<p align="left">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 7.0+" />
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin & Jetpack Compose" />
  <img src="https://img.shields.io/badge/Design-Material%203-1B2A4A?style=flat-square" alt="Material 3" />
  <img src="https://img.shields.io/badge/Architecture-100%25%20Local%20LAN%20%2F%20USB-0F766E?style=flat-square" alt="Local LAN and USB" />
</p>

**DeckLink** turns your Android phone into a local, browser-accessible control hub for your laptop or desktop—no PC client installation, no cloud accounts, and no internet required. Run the Android app, open the local URL in any desktop browser over **Wi-Fi** or **USB (`adb forward`)**, pair with a 6-digit PIN or QR code, and mirror your screen, stream internal phone audio, transfer unlimited-size files at high speed, and control your device in real time.

---

## Key Features

### Screen Mirroring & Remote Control
- **YouTube-Style Full-Screen & Rotation**: Rotate the mirrored phone display (`0°`, `90°`, `180°`, `270°`), toggle **Fit / Fill** zoom, and enter true edge-to-edge fullscreen (`F` key or double-click) with an auto-hiding floating toolbar.
- **Dynamic Orientation Adaptation**: Automatically resizes the `MediaProjection` capture surface when the phone switches between portrait and landscape.
- **Full Remote Input**: Click/tap, drag/swipe (automatically inverse-rotated to match the current view angle), send text from your laptop keyboard, trigger Android global actions (Back, Home, Recents, Notifications, Power menu), and inspect/click live Accessibility UI tree nodes.

### Live Phone-to-Laptop Internal Audio
- **Real-Time Audio Streaming** *(Android 10 / API 29+)*: Captures internal app/media audio via `AudioPlaybackCaptureConfiguration` and streams raw 16-bit PCM (`24 kHz`) directly into the browser's Web Audio API with a live waveform visualizer.
- **Remote Sound Controls**: Adjust Media, Ring, Alarm, and Notification volumes, switch ringer modes, and send media playback keys (Play/Pause, Next, Previous).

### High-Speed Unlimited Secure File Vault
- **Unlimited File Size & Streaming I/O**: Transfer large videos, archives, or folders between phone and laptop using 128 KB buffered streaming I/O with `TCP_NODELAY`, 256 KB socket buffers, and atomic `.part` writes—no in-memory buffering limits.
- **Desktop Studio File Browser**: Drag-and-drop multi-file upload queue with live `%` progress and `MB/s` throughput, subfolder creation (`mkdir`), search, and HTTP `Range`-capable in-browser preview for images, videos, and audio.
- **Zero Broad Storage Permissions**: Files are shared via Android's Photo Picker and Storage Access Framework (SAF) into an app-scoped `shared_vault/` directory protected by strict canonical-path confinement against path traversal.

### Productivity & Device Hub
- **App Streaming & Search Everywhere**: Browse and launch installed phone apps directly from your laptop browser, or search across apps and Vault files simultaneously.
- **Bidirectional Clipboard Sync**: Read the phone clipboard or push text from your laptop to your phone in one click.
- **Notifications Bridge**: View and dismiss active Android notifications from your desktop *(via optional `NotificationListenerService`)*.
- **Hardware Controls & Macros**: Toggle the flashlight, ring/vibrate to find your phone, push URLs straight to the phone browser, and trigger multi-step automation macros.
- **Self-Contained Web Console**: Served directly from the phone (`web_console.html`) with Light & Dark themes and zero external CDN dependencies.

---

## Quick Start

### 1. Install DeckLink on Android
- Download the prebuilt APK from the **Actions** tab (`Build DeckLink APK` workflow -> `DeckLink-debug-apk` artifact), or build from source (see [Building from Source](#building-from-source)).
- Open **DeckLink** on your Android phone and tap **Start Host** to start the local server (default port `8765`).

### 2. Connect Your Laptop

#### Option A: Over Wi-Fi (Same Network)
1. Ensure your phone and laptop are connected to the same Wi-Fi network.
2. Open **Add Devices** in DeckLink to view your local IP address (e.g., `http://192.168.1.42:8765`), scannable QR code, and rotating **6-digit PIN**.
3. Open the URL in any modern desktop browser (Chrome, Edge, Firefox, Safari) and enter the 6-digit PIN to pair.

#### Option B: Over USB Cable (Zero-Lag ADB Bridge)
1. Enable **USB Debugging** on your Android phone and connect it to your laptop via USB.
2. Forward the DeckLink port in a terminal:
   ```bash
   adb forward tcp:8765 tcp:8765
   ```
3. Open `http://127.0.0.1:8765` in your laptop browser and enter the 6-digit PIN.

### 3. Enable Optional Device Capabilities
Inside the Android app (**Actions & Gestures** / **Dashboard**), you can selectively grant and toggle capabilities at any time:
- **Screen Share (`MediaProjection`)**: Required for live screen mirroring and internal audio streaming.
- **Accessibility Service**: Required for remote touch, swipes, text input, global navigation keys, and UI tree inspection.
- **Notification Access**: Required to mirror and dismiss phone notifications on your laptop.

---

## Security & Privacy

DeckLink is built **100% local-first**—no traffic ever leaves your LAN or USB cable:

- **PIN Pairing & Lockout**: Sessions require a 6-digit pairing PIN with rate-limited lockout protection against brute-force attempts.
- **In-Memory Session Auth**: Bearer session tokens and short-lived stream tickets are held strictly in browser memory (never persisted to `localStorage`) and can be revoked instantly from the phone.
- **Granular Kill-Switches**: Every capability (screen mirroring, audio streaming, remote touch, file vault, clipboard, notifications, hardware controls) can be independently toggled on/off from the Android app.
- **Strict Content Security Policy & Path Confinement**: The embedded web console enforces a strict CSP with zero external scripts/assets, and all file operations are confined to the app-scoped Vault using canonical path validation.

---

## Architecture & Project Structure

```text
.
├── .github/workflows/
│   └── build-apk.yml                 # Automated GitHub Actions Debug APK build
└── app/src/main/
    ├── AndroidManifest.xml           # Services, permissions, FileProvider & launcher queries
    ├── assets/
    │   └── web_console.html          # Self-contained desktop studio web UI (Light/Dark)
    └── java/com/example/
        ├── MainActivity.kt           # Entry point, edge-to-edge host, MediaProjection & permissions
        ├── control/
        │   ├── AudioCaptureManager.kt    # Android 10+ internal PCM audio capture
        │   ├── ScreenCaptureManager.kt   # MediaProjection + VirtualDisplay MJPEG encoder
        │   └── ...                       # Device, media, clipboard, app & macro controllers
        ├── data/
        │   ├── VaultFileManager.kt       # High-speed streaming File Vault with path confinement
        │   └── ...                       # DataStore preferences & host state repository
        ├── model/                        # Immutable state & domain models
        ├── server/
        │   └── DeckLinkHttpServer.kt     # Embedded local HTTP server, auth, streams & REST API
        ├── service/
        │   ├── DeckLinkHostService.kt          # Foreground service (dataSync | mediaProjection)
        │   ├── DeckLinkAccessibilityService.kt # Remote gestures, typing & UI tree inspection
        │   └── DeckLinkNotificationListener.kt # Active notification bridge
        └── ui/
            ├── DeckLinkApp.kt        # Material 3 navigation drawer & bottom bar shell
            ├── components/           # Pure-Kotlin ISO/IEC 18004 QR code renderer & UI tiles
            ├── screens/              # Dashboard, Files Hub, Add Devices, Controls, Settings, Activity
            └── theme/                # Material 3 color scheme, typography & shapes
```

---

## Local HTTP & Streaming API Overview

All endpoints (except `/api/ping` and `/api/auth/pair`) require an authenticated Bearer token or stream ticket issued after PIN pairing.

| Category | Endpoints |
| :--- | :--- |
| **Health & Auth** | `GET /api/ping`, `POST /api/auth/pair`, `POST /api/auth/logout`, `GET /api/status` |
| **Screen & Audio** | `GET /api/stream/mjpeg`, `GET /api/screen/frame`, `POST /api/screen/rotate`, `GET /api/stream/audio` |
| **Secure File Vault** | `GET /api/files`, `POST /api/files/upload`, `GET /api/files/download` *(supports `Range`)*, `POST /api/files/mkdir`, `DELETE /api/files` |
| **Remote Input & UI** | `POST /api/input/tap`, `POST /api/input/swipe`, `POST /api/input/text`, `POST /api/input/global`, `GET /api/ui/tree`, `POST /api/input/node-click` |
| **Volume & Media** | `POST /api/audio/volume`, `POST /api/audio/ringer`, `POST /api/media/key` |
| **Apps & Clipboard** | `GET /api/apps`, `POST /api/apps/launch`, `GET /api/clipboard`, `POST /api/clipboard` |
| **Notifications** | `GET /api/notifications`, `POST /api/notifications/dismiss` |
| **Device & Macros** | `POST /api/device/torch`, `POST /api/device/ring`, `POST /api/device/vibrate`, `POST /api/device/open-url`, `GET /api/macros`, `POST /api/macros/run` |

---

## Requirements & Permissions

- **Minimum Android Version**: Android 7.0 (API 24)
- **Internal Audio Streaming**: Android 10 (API 29) or higher
- **Target / Compile SDK**: API 36

| Permission / Service | Purpose |
| :--- | :--- |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Local LAN HTTP server and IP discovery |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Keeps the host server and screen/audio capture active reliably |
| `POST_NOTIFICATIONS` *(Android 13+)* | Displays the persistent foreground host status notification |
| `RECORD_AUDIO` *(Android 10+, optional)* | Required by Android's `AudioPlaybackCapture` API to capture internal phone audio |
| `VIBRATE`, `WAKE_LOCK` | Find-my-phone alerts and optional keep-awake during sessions |
| `DeckLinkAccessibilityService` *(optional)* | Enables remote touch, swipe, typing, navigation keys, and UI tree inspection |
| `DeckLinkNotificationListener` *(optional)* | Bridges active phone notifications to the laptop console |

> DeckLink intentionally does **not** request `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, or `QUERY_ALL_PACKAGES`.

---

## Building from Source

### Prerequisites
- **JDK 21** (CI runs on Temurin JDK 21; Kotlin/Java target compatibility is Java 11)
- **Android SDK** with Platform `36`
- **Gradle 9.3.1** (or Android Studio with Gradle sync)

### Local Command-Line Build
```bash
# Clone the repository
git clone https://github.com/1funkybro777-hash/desklink.git
cd desklink

# Build the Debug APK
gradle :app:assembleDebug
```
The compiled APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Automated GitHub Actions Builds
Every push or pull request to `main` (as well as manual `workflow_dispatch` runs) triggers `.github/workflows/build-apk.yml`, which compiles the Debug APK and publishes it as the downloadable **`DeckLink-debug-apk`** artifact under the **Actions** tab.

---

## Tech Stack

- **Android UI**: Kotlin, Jetpack Compose, Material Design 3, Edge-to-Edge WindowInsets
- **State & Storage**: ViewModel, Kotlin Coroutines & StateFlow, Jetpack DataStore Preferences
- **Capture & Device APIs**: `MediaProjection`, `VirtualDisplay`, `ImageReader`, `AudioPlaybackCaptureConfiguration`, `AccessibilityService`, `NotificationListenerService`, Android Photo Picker & SAF
- **Desktop Web Client**: Vanilla HTML5, CSS3, and JavaScript (Web Audio API, Fetch/XHR streaming uploads, Range media playback, Fullscreen API) with zero external dependencies
