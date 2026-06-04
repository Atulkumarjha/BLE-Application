// c:\BLE Clone Application\lib\presentation\screens\scan_screen.dart

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/ble_repository_impl.dart';
import '../../domain/models/ble_device.dart';
import '../providers/ble_providers.dart';
import '../widgets/device_tile.dart';
import 'device_detail_screen.dart';

/// Entry screen for scanning and browsing nearby BLE devices.
class ScanScreen extends ConsumerStatefulWidget {
  const ScanScreen({super.key});

  @override
  ConsumerState<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends ConsumerState<ScanScreen> {
  @override
  Widget build(BuildContext context) {
    ref.listen<BleScannerState>(bleScannerProvider, (previous, next) {
      if (next.errorMessage != null && next.errorMessage != previous?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
      if (previous?.adapterState != next.adapterState && next.adapterState == 'off') {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enable Bluetooth')),
        );
      }
    });

    final state = ref.watch(bleScannerProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('BLE Cloner'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: _StatusChip(adapterState: state.adapterState),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: state.scanning ? _stopScan : _startScan,
        icon: Icon(state.scanning ? Icons.stop : Icons.search),
        label: Text(state.scanning ? 'Stop Scan' : 'Start Scan'),
      ),
      body: state.devices.isEmpty
          ? const Center(child: Text('Start a scan to discover nearby BLE devices.'))
          : ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: state.devices.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final device = state.devices[index];
                return Card(
                  child: DeviceTile(
                    device: device,
                    onTap: () => _openDevice(device),
                  ),
                );
              },
            ),
    );
  }

  Future<void> _startScan() => ref.read(bleScannerProvider.notifier).startScan();

  Future<void> _stopScan() => ref.read(bleScannerProvider.notifier).stopScan();

  void _openDevice(BleDevice device) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => DeviceDetailScreen(device: device)),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.adapterState});

  final String adapterState;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final color = adapterState == 'on'
        ? colors.primaryContainer
        : adapterState == 'off'
            ? colors.errorContainer
            : colors.surfaceContainerHighest;
    return Chip(
      label: Text(adapterState.toUpperCase()),
      backgroundColor: color,
      visualDensity: VisualDensity.compact,
    );
  }
}