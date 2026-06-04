// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\MainActivity.kt

package com.blecloner.blecloner

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    flutterEngine.plugins.add(BLEPlugin())
  }
}// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\MainActivity.kt

package com.blecloner.blecloner

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine.plugins.add(BLEPlugin())
    }
}