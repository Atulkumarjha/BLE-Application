// c:\BLE Clone Application\lib\presentation\screens\clone_screen.dart

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/models/ble_device.dart';
import '../providers/ble_providers.dart';

/// Shows the active peripheral advertising session.
class CloneScreen extends ConsumerStatefulWidget {
  const CloneScreen({super.key, required this.profile});

  final BleDevice profile;

  @override
  ConsumerState<CloneScreen> createState() => _CloneScreenState();
}

class _CloneScreenState extends ConsumerState<CloneScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<BlePeripheralState>(blePeripheralProvider, (previous, next) {
      if (next.errorMessage != null && next.errorMessage != previous?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
    });

    final state = ref.watch(blePeripheralProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Broadcasting')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              AnimatedBuilder(
                animation: _controller,
                builder: (context, child) => Transform.scale(
                  scale: 0.9 + (_controller.value * 0.2),
                  child: child,
                ),
                child: Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Theme.of(context).colorScheme.primaryContainer,
                  ),
                  child: Icon(
                    state.advertising ? Icons.radar : Icons.radar_outlined,
                    size: 56,
                  ),
                ),
              ),
              const SizedBox(height: 24),
              Text(
                state.advertising ? 'Advertising active' : 'Advertising stopped',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              Text('${state.serviceCount} services • ${state.characteristicCount} characteristics'),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: state.advertising ? _stopBroadcasting : null,
                  icon: const Icon(Icons.stop_circle_outlined),
                  label: const Text('Stop Broadcasting'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _stopBroadcasting() => ref.read(blePeripheralProvider.notifier).stop();
}