// c:\BLE Clone Application\lib\main.dart

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'presentation/screens/scan_screen.dart';

void main() {
  runApp(const ProviderScope(child: BleClonerApp()));
}

/// Root application widget for BLE Cloner.
class BleClonerApp extends StatelessWidget {
  const BleClonerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'BLE Cloner',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1976D2)),
        useMaterial3: true,
      ),
      home: const ScanScreen(),
    );
  }
}