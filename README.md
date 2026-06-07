# BLE Cloner

A Flutter application that discovers nearby BLE peripherals, resolves their full GATT profiles, and re-advertises the discovered structure as an Android BLE peripheral. This project is built **without** any third-party BLE plugins, using 100% native Kotlin implementation for the core BLE logic.

## Setup & Run

### Prerequisites
- **Flutter SDK**: v3.22.0 or newer.
- **Android Studio**: Latest version with Android SDK.
- **Physical Android Device**: Android 12 (API 31) or higher is required for BLE Peripheral features and the modern permission model. **Emulators are not supported.**

### Installation
1. Clone the repository.
2. Open the project in Android Studio.
3. Run `flutter pub get` in the terminal or click **Pub get** in `pubspec.yaml`.
4. Connect your physical Android device with USB Debugging enabled.
5. Ensure **Bluetooth** and **Location (GPS)** are turned **ON** globally on the phone.
6. Click the **Green Play button** in Android Studio to build and run the app.

##  Architecture & MethodChannel Mapping

The project follows a **Clean Architecture** approach to bridge the Flutter UI with the native Android Bluetooth stack.

### Data Flow
`Flutter UI (Riverpod)` ↔ `Domain Layer` ↔ `Data Repository` ↔ `MethodChannel` ↔ `Native Kotlin Plugin`

### MethodChannel Data Mapping
We use a centralized `BleRepository` that handles serialization between Dart and Kotlin.

| Method Name | Direction | Payload | Purpose |
|-------------|-----------|---------|---------|
| `startScan` | Dart → Kotlin | `null` | Starts LE scanning via `BluetoothLeScanner`. |
| `connectAndDiscover` | Dart → Kotlin | `deviceId` | Connects to GATT, discovers services, and returns a JSON-mapped profile. |
| `startPeripheral` | Dart → Kotlin | `profile` (Map) | Initializes `BluetoothGattServer` and starts `BluetoothLeAdvertiser`. |
| `checkPermissions` | Dart → Kotlin | `null` | Returns a Map of granted/denied states for modern BLE permissions. |

### EventChannel (Streaming)
The native layer uses an `EventChannel` to stream asynchronous updates back to Flutter without waiting for a request:
- `device_found`: Emitted whenever a new scan result is parsed.
- `scan_state`: Emitted when the hardware scanning status changes.
- `adapter_state`: Emitted when Bluetooth is toggled ON/OFF.
- `peripheral_state`: Emitted when the advertising status changes.

## Platform-Specific Limitations

During development, several native Android behaviors were addressed:

1. **Android 12+ Permission Model**:
   Starting with Android 12, `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` are runtime permissions separate from Location. The app implements a dual-check system to support both legacy (API < 31) and modern permission flows.

2. **GATT Connection Reliability**:
   Android hardware often returns a generic "Status 133" when too many connections are attempted or if the device is out of range. The implementation includes an explicit `gatt.close()` and a 20-second discovery timeout to prevent hardware resource leaking.

3. **Background Limitations**:
   By default, Android kills LE scanning and GATT advertising shortly after the app is backgrounded. For a production-ready "Cloner," a Foreground Service with `FOREGROUND_SERVICE_CONNECTED_DEVICE` would be required to maintain advertising.

4. **Characteristic Value Constraints**:
   While we can discover all characteristics, native Android does not allow reading values of "Write-Only" characteristics. The "Clone" functionality creates the same UUID structure but starts with empty or default values for characteristics that aren't readable.

5. **JVM Signature Clash**:
   To ensure clean interop, extension functions in Kotlin were carefully named to avoid signature clashes when compiled to JVM byte-code, ensuring a stable build environment.

---
