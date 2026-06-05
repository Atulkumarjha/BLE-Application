# BLE Cloner — Local run & test

This repository contains the Flutter frontend and Android native Kotlin plugin for the BLE Cloner app.

Local test helper scripts
- PowerShell (Windows): `tools/run_local_tests.ps1`
- POSIX (macOS / Linux): `tools/run_local_tests.sh`

Quick steps to run locally
1. Install Flutter and Android SDK and ensure `flutter` is on your PATH.
2. From the project root run the helper script for your platform to perform checks and create `logs/`:

Windows (PowerShell):

```powershell
.\tools\run_local_tests.ps1
```

macOS / Linux:

```bash
./tools/run_local_tests.sh
```

3. If the script shows `gradle-wrapper.jar` is missing, either open the project in Android Studio (it will generate the wrapper) or run `gradle wrapper` from the `android` folder on a machine that has Gradle installed.
4. Run the app and capture logs (recommended):

```bash
# capture flutter output
flutter run -d <device-id> | tee logs/flutter_run.log

# capture Android system logs
adb logcat -v time > logs/adb_logcat.log
```

5. Reproduce the BLE flow (scan, connect, clone). Attach the two log files from `logs/` here and I'll analyze them.

If you want, I can produce a short PowerShell/Batch wrapper to run both `flutter run` and `adb logcat` concurrently and save logs automatically — tell me which OS/device you're using and I will add it.

APK install & log helper
After the GitHub Actions workflow completes you can download the `app-debug-apk` artifact and install it. I added helper scripts to simplify installing and capturing logs:

Windows (PowerShell):

```powershell
.\tools\install_and_log.ps1 -ApkPath C:\path\to\app-debug.apk
```

macOS / Linux:

```bash
./tools/install_and_log.sh path/to/app-debug.apk
```

The scripts will install the APK to the first connected device and stream logs to `logs/adb_logcat.log`.
<!-- c:\BLE Clone Application\README.md -->
# BLE Cloner

A Flutter app that discovers nearby BLE peripherals, reads their GATT profile, and re-advertises the discovered structure as an Android BLE peripheral through native Kotlin code.

## Setup & Run

### Prerequisites

- Flutter 3.22 or newer
- Android Studio with a recent Android SDK and platform tools
- Android 12+ device or emulator with Bluetooth LE support

### Install

```bash
flutter pub get
```

### Android local.properties

Create `android/local.properties` locally if Flutter has not already done so. It should point to your Flutter SDK and Android SDK, for example:

```properties
flutter.sdk=C:\src\flutter
sdk.dir=C:\Users\YOUR_USER\AppData\Local\Android\Sdk
```

### Android permissions

The Android manifest includes the required permissions for scan, connect, advertise, and legacy location support:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
```

### Run

```bash
flutter run
```

## Architecture Overview

```text
Flutter UI (Riverpod)
  -> BleRepositoryImpl
  -> MethodChannel: com.blecloner/ble
  -> EventChannel:  com.blecloner/ble_events
  -> Android Kotlin plugin
     -> BLEScanner (scan + GATT discovery)
     -> BLEPeripheral (GattServer + advertiser)
```

### State machine

- Scan screen checks permissions and Bluetooth adapter state before scan starts.
- Native scan events stream discovered devices into the scanner state notifier.
- Device detail screen resolves the full GATT tree for one device.
- Clone action stops scanning, starts the peripheral, and navigates to the clone screen.
- Clone screen listens for advertising state changes from the native event stream.

## MethodChannel Data Map

| Method name | Direction | Dart type | Native type |
| --- | --- | --- | --- |
| `startScan()` | Dart -> Kotlin | `Future<void>` | `void` |
| `stopScan()` | Dart -> Kotlin | `Future<void>` | `void` |
| `connectAndDiscover(deviceId)` | Dart -> Kotlin | `Future<BleDevice>` | `Map<String, Any?>` |
| `startPeripheral(profile)` | Dart -> Kotlin | `Future<void>` | `void` |
| `stopPeripheral()` | Dart -> Kotlin | `Future<void>` | `void` |
| `checkPermissions()` | Dart -> Kotlin | `Future<Map<String, bool>>` | `Map<String, Boolean>` |
| `requestPermissions()` | Dart -> Kotlin | `Future<void>` | `void` |

Event payloads emitted over `com.blecloner/ble_events`:

- `device_found`: `{ type, device: { id, name, rssi } }`
- `scan_state`: `{ type, scanning }`
- `peripheral_state`: `{ type, advertising, error? }`
- `adapter_state`: `{ type, state: "on" | "off" | "unauthorized" }`

### Cloned profile JSON

```json
{
  "id": "AA:BB:CC:DD:EE:FF",
  "name": "Example Device",
  "rssi": -52,
  "services": [
    {
      "uuid": "0000180D-0000-1000-8000-00805F9B34FB",
      "characteristics": [
        {
          "uuid": "00002A37-0000-1000-8000-00805F9B34FB",
          "properties": ["READ", "NOTIFY"],
          "permissions": ["READ"],
          "value": null
        }
      ]
    }
  ]
}
```

UUIDs are normalized to uppercase strings with braces removed before they are persisted or re-advertised.

## Platform Limitations

- Android 12+ uses the new runtime permission model for `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_ADVERTISE`.
- BLE scanning, discovery, and advertising are unreliable or unavailable while the app is backgrounded on many devices unless a foreground service is added.
- Characteristic values are constrained by the BLE specification and practical MTU limits; large values may need chunking or compression.
- Re-advertising an exact peripheral profile is limited by Android peripheral APIs, which primarily expose service UUIDs and characteristic behavior rather than a perfect clone of another device.

## Notes

- Native Android entry is registered from `MainActivity.kt` via the in-app `BLEPlugin`.
- No pre-made Flutter BLE package is used for the core BLE flow.# BLE-Application
- No pre-made Flutter BLE package is used for the core BLE flow.
