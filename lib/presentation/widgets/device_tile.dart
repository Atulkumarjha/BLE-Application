// c:\BLE Clone Application\lib\presentation\widgets\device_tile.dart

import 'package:flutter/material.dart';

import '../../domain/models/ble_device.dart';

/// Displays a discovered device with signal strength and identifier.
class DeviceTile extends StatelessWidget {
  const DeviceTile({super.key, required this.device, required this.onTap});

  final BleDevice device;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final name = device.name?.trim().isNotEmpty == true ? device.name! : 'Unknown Device';
    return ListTile(
      onTap: onTap,
      leading: _SignalBars(rssi: device.rssi),
      title: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text(device.id, maxLines: 2, overflow: TextOverflow.ellipsis),
      trailing: Text('${device.rssi} dBm', style: Theme.of(context).textTheme.labelMedium),
    );
  }
}

class _SignalBars extends StatelessWidget {
  const _SignalBars({required this.rssi});

  final int rssi;

  @override
  Widget build(BuildContext context) {
    final activeBars = rssi >= -55 ? 4 : rssi >= -67 ? 3 : rssi >= -80 ? 2 : 1;
    return SizedBox(
      width: 20,
      height: 18,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: List.generate(4, (index) {
          final height = 4.0 + (index * 3.0);
          final active = index < activeBars;
          return Padding(
            padding: const EdgeInsets.only(right: 1),
            child: Container(
              width: 3,
              height: height,
              decoration: BoxDecoration(
                color: active ? Theme.of(context).colorScheme.primary : Colors.grey.shade400,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          );
        }),
      ),
    );
  }
}