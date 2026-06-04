// c:\BLE Clone Application\lib\data\channels\ble_channel.dart

/// Centralized BLE channel and method names shared across the Flutter app.
class BleChannel {
  static const String methodChannelName = 'com.blecloner/ble';
  static const String eventChannelName = 'com.blecloner/ble_events';

  static const String startScan = 'startScan';
  static const String stopScan = 'stopScan';
  static const String connectAndDiscover = 'connectAndDiscover';
  static const String startPeripheral = 'startPeripheral';
  static const String stopPeripheral = 'stopPeripheral';
  static const String checkPermissions = 'checkPermissions';
  static const String requestPermissions = 'requestPermissions';

  static const String eventDeviceFound = 'device_found';
  static const String eventScanState = 'scan_state';
  static const String eventPeripheralState = 'peripheral_state';
  static const String eventAdapterState = 'adapter_state';
}