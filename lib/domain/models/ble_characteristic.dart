// c:\BLE Clone Application\lib\domain\models\ble_characteristic.dart

/// Represents a BLE characteristic with its declared behavior.
class BleCharacteristic {
  const BleCharacteristic({
    required this.uuid,
    required this.properties,
    required this.permissions,
    this.value,
  });

  final String uuid;
  final List<String> properties;
  final List<String> permissions;
  final String? value;

  factory BleCharacteristic.fromJson(Map<String, dynamic> json) {
    return BleCharacteristic(
      uuid: _normalizeUuid(json['uuid'] as String? ?? ''),
      properties: (json['properties'] as List<dynamic>? ?? const <dynamic>[])
          .map((value) => value.toString().trim().toUpperCase())
          .where((value) => value.isNotEmpty)
          .toList(growable: false),
      permissions: (json['permissions'] as List<dynamic>? ?? const <dynamic>[])
          .map((value) => value.toString().trim().toUpperCase())
          .where((value) => value.isNotEmpty)
          .toList(growable: false),
      value: json['value']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'uuid': uuid,
        'properties': properties,
        'permissions': permissions,
        if (value != null) 'value': value,
      };

  static String _normalizeUuid(String input) {
    final trimmed = input.trim().replaceAll('{', '').replaceAll('}', '');
    return trimmed.toUpperCase();
  }
}