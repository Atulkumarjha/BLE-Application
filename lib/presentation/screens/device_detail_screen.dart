// c:\BLE Clone Application\lib\presentation\screens\device_detail_screen.dart

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/ble_repository_impl.dart';
import '../../domain/models/ble_device.dart';
import '../providers/ble_providers.dart';
import '../widgets/service_expansion_tile.dart';
import 'clone_screen.dart';

/// Displays the selected device's full GATT profile and clone action.
class DeviceDetailScreen extends ConsumerWidget {
  const DeviceDetailScreen({super.key, required this.device});

  final BleDevice device;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(bleDeviceProvider(device.id));
    return Scaffold(
      appBar: AppBar(title: Text(device.name?.trim().isNotEmpty == true ? device.name! : 'Device Details')),
      body: profile.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(child: Text(_messageForError(error))),
        data: (resolved) {
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: _Header(device: resolved),
              ),
              Expanded(
                child: ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  itemCount: resolved.services.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (context, index) => Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: ServiceExpansionTile(service: resolved.services[index]),
                    ),
                  ),
                ),
              ),
              SafeArea(
                minimum: const EdgeInsets.all(16),
                child: SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () => _cloneDevice(context, ref, resolved),
                    icon: const Icon(Icons.wifi_tethering),
                    label: const Text('Clone This Device'),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _cloneDevice(BuildContext context, WidgetRef ref, BleDevice profile) async {
    await ref.read(bleScannerProvider.notifier).stopScan();
    
    // Add a custom "Atul's Message" service to the profile before cloning
    final modifiedProfile = profile.copyWith(
      services: [
        ...profile.services,
        BleService(
          uuid: '0000A701-0000-1000-8000-00805F9B34FB', // Custom "Atul" Service
          characteristics: [
            BleCharacteristic(
              uuid: '0000A702-0000-1000-8000-00805F9B34FB', // Custom Message Char
              properties: const ['READ'],
              permissions: const ['READ'],
              value: "Hi! I'm Atul and I'm successfully connected to your phone",
            ),
          ],
        ),
      ],
    );

    try {
      await ref.read(blePeripheralProvider.notifier).start(modifiedProfile.toJson());
      if (!context.mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => CloneScreen(profile: modifiedProfile)),
      );
    } on Object catch (error) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_messageForError(error))));
    }
  }

  String _messageForError(Object error) {
    if (error is BleOperationException) {
      return error.message;
    }
    return error.toString();
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.device});

  final BleDevice device;

  @override
  Widget build(BuildContext context) {
    final name = device.name?.trim().isNotEmpty == true ? device.name! : 'Unknown Device';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(name, style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(device.id, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 12),
        Text('${device.services.length} services', style: Theme.of(context).textTheme.labelLarge),
      ],
    );
  }
}