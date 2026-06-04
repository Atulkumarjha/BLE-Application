// c:\BLE Clone Application\lib\presentation\widgets\service_expansion_tile.dart

import 'package:flutter/material.dart';

import '../../domain/models/ble_service.dart';
import 'characteristic_tile.dart';

/// Groups the characteristics under a service UUID.
class ServiceExpansionTile extends StatelessWidget {
  const ServiceExpansionTile({super.key, required this.service});

  final BleService service;

  @override
  Widget build(BuildContext context) {
    return ExpansionTile(
      tilePadding: EdgeInsets.zero,
      title: Text(service.uuid, style: const TextStyle(fontWeight: FontWeight.w600)),
      childrenPadding: const EdgeInsets.only(left: 8, bottom: 8),
      children: service.characteristics
          .map((characteristic) => CharacteristicTile(characteristic: characteristic))
          .toList(growable: false),
    );
  }
}