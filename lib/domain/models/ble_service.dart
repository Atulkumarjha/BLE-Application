// c:\BLE Clone Application\lib\domain\models\ble_service.dart

import 'ble_characteristic.dart';

/// Represents a GATT service and its characteristics.
class BleService {
  const BleService({required this.uuid, required this.characteristics});

  final String uuid;
  final List<BleCharacteristic> characteristics;

  factory BleService.fromJson(Map<String, dynamic> json) {
    return BleService(
      uuid: _normalizeUuid(json['uuid'] as String? ?? ''),
      characteristics: (json['characteristics'] as List<dynamic>? ??
              const <dynamic>[])
          .whereType<Map<String, dynamic>>()
          .map(BleCharacteristic.fromJson)
          .toList(growable: false),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'uuid': uuid,
        'characteristics': characteristics.map((item) => item.toJson()).toList(),
      };

  static String _normalizeUuid(String input) {
    final trimmed = input.trim().replaceAll('{', '').replaceAll('}', '');
    return trimmed.toUpperCase();
  }
}