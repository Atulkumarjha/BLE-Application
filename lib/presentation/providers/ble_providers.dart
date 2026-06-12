// c:\BLE Clone Application\lib\presentation\providers\ble_providers.dart

import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/ble_repository_impl.dart';
import '../../domain/models/ble_device.dart';
import '../../domain/repositories/ble_repository.dart';

const Object _preserveValue = Object();

/// Shared repository injection point for all BLE features.
final bleRepositoryProvider = Provider<BleRepository>((ref) {
  return BleRepositoryImpl();
});

/// Holds the current scanning state and discovered devices.
class BleScannerState {
  const BleScannerState({
    required this.scanning,
    required this.devices,
    required this.adapterState,
    this.errorMessage,
  });

  factory BleScannerState.initial() => const BleScannerState(
        scanning: false,
        devices: <BleDevice>[],
        adapterState: 'unknown',
      );

  final bool scanning;
  final List<BleDevice> devices;
  final String adapterState;
  final String? errorMessage;

  BleScannerState copyWith({
    bool? scanning,
    List<BleDevice>? devices,
    String? adapterState,
    Object? errorMessage = _preserveValue,
  }) {
    return BleScannerState(
      scanning: scanning ?? this.scanning,
      devices: devices ?? this.devices,
      adapterState: adapterState ?? this.adapterState,
      errorMessage: identical(errorMessage, _preserveValue) ? this.errorMessage : errorMessage as String?,
    );
  }
}

/// Manages scan lifecycle and folds native events into view state.
class BleScannerNotifier extends StateNotifier<BleScannerState> {
  BleScannerNotifier(this._repository) : super(BleScannerState.initial()) {
    _subscription = _repository.events.listen(_handleEvent);
  }

  final BleRepository _repository;
  late final StreamSubscription<Map<String, dynamic>> _subscription;

  @override
  set state(BleScannerState value) {
    if (mounted) {
      super.state = value;
    }
  }

  Future<void> startScan() async {
    final permissions = await _repository.checkPermissions();
    // On Android 12+, we need scan, connect, and advertise.
    // On older versions, we just need location.
    final hasPermission = permissions['scan'] == true &&
                          permissions['connect'] == true &&
                          permissions['location'] == true;

    if (!hasPermission) {
      await _repository.requestPermissions();
      return;
    }
    try {
      await _repository.startScan();
    } on BleOperationException catch (error) {
      state = state.copyWith(errorMessage: error.message, scanning: false);
    }
  }

  Future<void> stopScan() async {
    try {
      await _repository.stopScan();
    } on BleOperationException catch (error) {
      state = state.copyWith(errorMessage: error.message);
    }
  }

  void _handleEvent(Map<String, dynamic> event) {
    final type = event['type']?.toString();
    if (type == 'device_found') {
      final device = BleDevice.fromJson(
        event['device'] as Map<String, dynamic>? ?? <String, dynamic>{},
      );
      final devices = [...state.devices];
      final index = devices.indexWhere((item) => item.id == device.id);
      if (index >= 0) {
        devices[index] = device;
      } else {
        devices.add(device);
      }
      state = state.copyWith(devices: devices, errorMessage: null);
      return;
    }
    if (type == 'scan_state') {
      state = state.copyWith(
        scanning: event['scanning'] == true,
        errorMessage: null,
      );
      return;
    }
    if (type == 'adapter_state') {
      state = state.copyWith(
        adapterState: event['state']?.toString() ?? 'unknown',
      );
    }
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

/// Tracks peripheral broadcasting state and profile summary.
class BlePeripheralState {
  const BlePeripheralState({
    required this.advertising,
    required this.serviceCount,
    required this.characteristicCount,
    this.errorMessage,
  });

  factory BlePeripheralState.initial() => const BlePeripheralState(
        advertising: false,
        serviceCount: 0,
        characteristicCount: 0,
      );

  final bool advertising;
  final int serviceCount;
  final int characteristicCount;
  final String? errorMessage;

  BlePeripheralState copyWith({
    bool? advertising,
    int? serviceCount,
    int? characteristicCount,
    Object? errorMessage = _preserveValue,
  }) {
    return BlePeripheralState(
      advertising: advertising ?? this.advertising,
      serviceCount: serviceCount ?? this.serviceCount,
      characteristicCount: characteristicCount ?? this.characteristicCount,
      errorMessage: identical(errorMessage, _preserveValue) ? this.errorMessage : errorMessage as String?,
    );
  }
}

/// Controls the lifecycle of the Android peripheral broadcaster.
class BlePeripheralNotifier extends StateNotifier<BlePeripheralState> {
  BlePeripheralNotifier(this._repository) : super(BlePeripheralState.initial()) {
    _subscription = _repository.events.listen(_handleEvent);
  }

  final BleRepository _repository;
  late final StreamSubscription<Map<String, dynamic>> _subscription;

  @override
  set state(BlePeripheralState value) {
    if (mounted) {
      super.state = value;
    }
  }

  Future<void> start(Map<String, dynamic> profile) async {
    final services = (profile['services'] as List<dynamic>? ?? const <dynamic>[])
        .whereType<Map<String, dynamic>>()
        .toList(growable: false);
    final characteristicCount = services.fold<int>(0, (total, service) {
      final list = service['characteristics'] as List<dynamic>? ?? const <dynamic>[];
      return total + list.length;
    });
    state = state.copyWith(
      serviceCount: services.length,
      characteristicCount: characteristicCount,
    );
    try {
      await _repository.startPeripheral(profile);
      state = state.copyWith(advertising: true, errorMessage: null);
    } on BleOperationException catch (error) {
      state = state.copyWith(advertising: false, errorMessage: error.message);
    }
  }

  Future<void> stop() async {
    try {
      await _repository.stopPeripheral();
      state = state.copyWith(advertising: false, errorMessage: null);
    } on BleOperationException catch (error) {
      state = state.copyWith(errorMessage: error.message);
    }
  }

  void _handleEvent(Map<String, dynamic> event) {
    if (event['type']?.toString() == 'peripheral_state') {
      state = state.copyWith(
        advertising: event['advertising'] == true,
        errorMessage: event['error']?.toString(),
      );
    }
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

/// Resolves and reads a device's full GATT profile.
final bleDeviceProvider = FutureProvider.family<BleDevice, String>((ref, deviceId) {
  return ref.read(bleRepositoryProvider).connectAndDiscover(deviceId);
});

/// Binds the scanner state notifier to the repository.
final bleScannerProvider =
    StateNotifierProvider.autoDispose<BleScannerNotifier, BleScannerState>((ref) {
  final notifier = BleScannerNotifier(ref.read(bleRepositoryProvider));
  ref.onDispose(notifier.dispose);
  return notifier;
});

/// Binds the peripheral state notifier to the repository.
final blePeripheralProvider =
    StateNotifierProvider.autoDispose<BlePeripheralNotifier, BlePeripheralState>((ref) {
  final notifier = BlePeripheralNotifier(ref.read(bleRepositoryProvider));
  ref.onDispose(notifier.dispose);
  return notifier;
});