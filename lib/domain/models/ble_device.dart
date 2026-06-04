// c:\BLE Clone Application\lib\domain\models\ble_device.dart

import 'ble_service.dart';

/// Represents a discovered BLE device and its resolved GATT profile.
class BleDevice {
  const BleDevice({
    required this.id,
    required this.name,
    required this.rssi,
    this.services = const <BleService>[],
  });

  final String id;
  final String? name;
  final int rssi;
  final List<BleService> services;

  factory BleDevice.fromJson(Map<String, dynamic> json) {
    return BleDevice(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString(),
      rssi: (json['rssi'] as num?)?.toInt() ?? 0,
      services: (json['services'] as List<dynamic>? ?? const <dynamic>[])
          .whereType<Map<String, dynamic>>()
          .map(BleService.fromJson)
          .toList(growable: false),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'name': name,
        'rssi': rssi,
        'services': services.map((item) => item.toJson()).toList(),
      };

  BleDevice copyWith({
    String? id,
    String? name,
    int? rssi,
    List<BleService>? services,
  }) {
    return BleDevice(
      id: id ?? this.id,
      name: name ?? this.name,
      rssi: rssi ?? this.rssi,
      services: services ?? this.services,
    );
  }
}