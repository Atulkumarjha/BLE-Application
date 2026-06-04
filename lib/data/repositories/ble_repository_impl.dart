// c:\BLE Clone Application\lib\data\repositories\ble_repository_impl.dart

import 'dart:async';

import 'package:flutter/services.dart';

import '../../domain/models/ble_device.dart';
import '../../domain/repositories/ble_repository.dart';
import '../channels/ble_channel.dart';

/// Maps platform BLE failures to user-facing exceptions.
class BleOperationException implements Exception {
  const BleOperationException(this.code, this.message);

  final String code;
  final String message;

  @override
  String toString() => 'BleOperationException($code): $message';
}

/// Default BLE repository backed by MethodChannel and EventChannel.
class BleRepositoryImpl implements BleRepository {
  BleRepositoryImpl()
      : _methodChannel = const MethodChannel(BleChannel.methodChannelName),
        _eventChannel = const EventChannel(BleChannel.eventChannelName) {
    _eventSubscription = _eventChannel.receiveBroadcastStream().listen(
          (event) => _events.add(_asStringKeyMap(event)),
          onError: (Object error) {
            _events.add(<String, dynamic>{
              'type': 'error',
              'message': error.toString(),
            });
          },
        );
  }

  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;
  final StreamController<Map<String, dynamic>> _events =
      StreamController<Map<String, dynamic>>.broadcast();
  late final StreamSubscription<dynamic> _eventSubscription;

  @override
  Stream<Map<String, dynamic>> get events => _events.stream;

  @override
  Future<void> startScan() => _invokeVoid(BleChannel.startScan);

  @override
  Future<void> stopScan() => _invokeVoid(BleChannel.stopScan);

  @override
  Future<BleDevice> connectAndDiscover(String deviceId) async {
    try {
      final Map<dynamic, dynamic>? result = await _methodChannel.invokeMapMethod<dynamic, dynamic>(
        BleChannel.connectAndDiscover,
        <String, dynamic>{'deviceId': deviceId},
      );
      if (result == null) {
        throw const BleOperationException('GATT_FAILURE', 'Could not read device profile');
      }
      return BleDevice.fromJson(_asStringKeyMap(result));
    } on PlatformException catch (error) {
      throw _mapPlatformException(error);
    }
  }

  @override
  Future<void> startPeripheral(Map<String, dynamic> profile) =>
      _invokeVoid(BleChannel.startPeripheral, profile);

  @override
  Future<void> stopPeripheral() => _invokeVoid(BleChannel.stopPeripheral);

  @override
  Future<Map<String, bool>> checkPermissions() async {
    try {
      final result = await _methodChannel.invokeMapMethod<dynamic, dynamic>(
        BleChannel.checkPermissions,
      );
      final permissions = _asStringKeyMap(result ?? const <dynamic, dynamic>{});
      return permissions.map((key, value) => MapEntry(key, value == true));
    } on PlatformException catch (error) {
      throw _mapPlatformException(error);
    }
  }

  @override
  Future<void> requestPermissions() => _invokeVoid(BleChannel.requestPermissions);

  Future<void> _invokeVoid(String method, [Map<String, dynamic>? arguments]) async {
    try {
      await _methodChannel.invokeMethod<void>(method, arguments);
    } on PlatformException catch (error) {
      throw _mapPlatformException(error);
    }
  }

  BleOperationException _mapPlatformException(PlatformException error) {
    final message = switch (error.code) {
      'BLUETOOTH_OFF' => 'Please enable Bluetooth',
      'PERMISSION_DENIED' => 'Bluetooth permission required',
      'GATT_FAILURE' => 'Could not read device profile',
      'ADVERTISE_FAILED' => 'Could not start peripheral mode',
      _ => error.message ?? 'Unexpected BLE error',
    };
    return BleOperationException(error.code, message);
  }

  Map<String, dynamic> _asStringKeyMap(dynamic value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map((key, dynamic entry) => MapEntry(key.toString(), entry));
    }
    return <String, dynamic>{};
  }
}