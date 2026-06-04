// c:\BLE Clone Application\lib\domain\repositories\ble_repository.dart

import '../models/ble_device.dart';

/// Contract for all BLE operations exposed to Flutter.
abstract class BleRepository {
  Stream<Map<String, dynamic>> get events;

  Future<void> startScan();

  Future<void> stopScan();

  Future<BleDevice> connectAndDiscover(String deviceId);

  Future<void> startPeripheral(Map<String, dynamic> profile);

  Future<void> stopPeripheral();

  Future<Map<String, bool>> checkPermissions();

  Future<void> requestPermissions();
}