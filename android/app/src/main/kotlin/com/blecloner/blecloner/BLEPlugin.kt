// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\BLEPlugin.kt

package com.blecloner.blecloner

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class BLEPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
    private lateinit var context: Context
    private lateinit var scanner: BLEScanner
    private lateinit var peripheral: BLEPeripheral
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private var eventSink: EventChannel.EventSink? = null
    private var activity: Activity? = null
    private var receiver: BroadcastReceiver? = null

    private val adapterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                scanner.emitAdapterState()
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        scanner = BLEScanner(context, ::postEvent)
        peripheral = BLEPeripheral(context, ::postEvent)
        methodChannel = MethodChannel(binding.binaryMessenger, BleChannelNames.METHOD)
        eventChannel = EventChannel(binding.binaryMessenger, BleChannelNames.EVENTS)
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        receiver = adapterReceiver
        ContextCompat.registerReceiver(
            context,
            adapterReceiver,
            IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scanner.emitAdapterState()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startScan" -> handleVoid(result) { scanner.startScan() }
            "stopScan" -> handleVoid(result) { scanner.stopScan() }
            "connectAndDiscover" -> scope.launch(Dispatchers.IO) {
                runCatching {
                    val deviceId = call.argument<String>("deviceId").orEmpty()
                    scanner.connectAndDiscover(deviceId)
                }.fold({ postResult(result, it) }, { handleError(result, it) })
            }
            "startPeripheral" -> handleVoid(result) {
                @Suppress("UNCHECKED_CAST")
                peripheral.startPeripheral(call.arguments as? Map<String, Any?> ?: emptyMap())
            }
            "stopPeripheral" -> handleVoid(result) { peripheral.stopPeripheral() }
            "checkPermissions" -> postResult(result, checkPermissions())
            "requestPermissions" -> handleVoid(result) { requestPermissions() }
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        scanner.emitAdapterState()
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun handleVoid(result: MethodChannel.Result, block: () -> Unit) {
        try {
            block()
            postResult(result, null)
        } catch (error: Throwable) {
            handleError(result, error)
        }
    }

    private fun checkPermissions(): Map<String, Boolean> {
        val scan = hasPermission(permissionForScan())
        val connect = hasPermission(permissionForConnect())
        val advertise = hasPermission(permissionForAdvertise())
        val location = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return mapOf(
            "bluetooth" to (scan && connect && advertise),
            "location" to location,
            "scan" to scan,
            "connect" to connect,
            "advertise" to advertise,
        )
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (!hasPermission(permissionForScan())) permissions += permissionForScan()
        if (!hasPermission(permissionForConnect())) permissions += permissionForConnect()
        if (!hasPermission(permissionForAdvertise())) permissions += permissionForAdvertise()
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) permissions += Manifest.permission.ACCESS_FINE_LOCATION
        val currentActivity = activity ?: return
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(currentActivity, permissions.distinct().toTypedArray(), 9001)
        }
    }

    private fun permissionForScan(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
    private fun permissionForConnect(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.ACCESS_FINE_LOCATION
    private fun permissionForAdvertise(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_ADVERTISE else Manifest.permission.ACCESS_FINE_LOCATION

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun postEvent(event: Map<String, Any?>) {
        mainHandler.post { eventSink?.success(event) }
    }

    private fun postResult(result: MethodChannel.Result, value: Any?) {
        mainHandler.post { result.success(value) }
    }

    private fun handleError(result: MethodChannel.Result, error: Throwable) {
        val mapped = if (error is BleNativeException) error else BleNativeException("UNKNOWN", error.message ?: "Unexpected BLE error", error)
        Log.e("BLEPlugin", "[${mapped.code}] ${mapped.message}", mapped)
        mainHandler.post { result.error(mapped.code, mapped.message, Log.getStackTraceString(mapped)) }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener { _, _, grantResults ->
            val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!granted) {
                postEvent(mapOf("type" to BleEventTypes.ADAPTER_STATE, "state" to "unauthorized"))
            }
            true
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}