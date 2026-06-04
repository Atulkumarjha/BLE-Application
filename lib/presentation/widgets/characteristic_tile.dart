// c:\BLE Clone Application\lib\presentation\widgets\characteristic_tile.dart

import 'package:flutter/material.dart';

import '../../domain/models/ble_characteristic.dart';

/// Renders a characteristic row with property and permission badges.
class CharacteristicTile extends StatelessWidget {
  const CharacteristicTile({super.key, required this.characteristic});

  final BleCharacteristic characteristic;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      dense: true,
      contentPadding: EdgeInsets.zero,
      title: Text(characteristic.uuid),
      subtitle: Wrap(
        spacing: 6,
        runSpacing: 6,
        children: [
          ...characteristic.properties.map((value) => _Badge(text: value)),
          ...characteristic.permissions.map((value) => _Badge(text: value, subdued: true)),
        ],
      ),
      trailing: characteristic.value == null ? null : Text(characteristic.value!, maxLines: 1),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, this.subdued = false});

  final String text;
  final bool subdued;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: subdued ? colors.surfaceContainerHighest : colors.primaryContainer,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(text, style: Theme.of(context).textTheme.labelSmall),
    );
  }
}