// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\BLEScanner.kt

package com.blecloner.blecloner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class BLEScanner(
    private val context: Context,
    private val emitEvent: (Map<String, Any?>) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private var callback: ScanCallback? = null
    private var activeGatt: BluetoothGatt? = null

    fun adapterState(): String {
        val adapter = adapter ?: return "unauthorized"
        if (!hasScanPermission() || !hasConnectPermission()) return "unauthorized"
        return when (adapter.state) {
            BluetoothAdapter.STATE_ON -> "on"
            BluetoothAdapter.STATE_OFF -> "off"
            else -> "off"
        }
    }

    fun emitAdapterState() {
        postEvent(mapOf("type" to BleEventTypes.ADAPTER_STATE, "state" to adapterState()))
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bluetoothAdapter = adapter ?: throw BleNativeException(BleErrorCodes.BLUETOOTH_OFF, "Bluetooth adapter unavailable")
        if (!bluetoothAdapter.isEnabled) throw BleNativeException(BleErrorCodes.BLUETOOTH_OFF, "Please enable Bluetooth")
        if (!hasScanPermission()) throw BleNativeException(BleErrorCodes.PERMISSION_DENIED, "Bluetooth permission required")
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: throw BleNativeException(BleErrorCodes.BLUETOOTH_OFF, "BLE scanner unavailable")
        stopScan()
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d("BLEScanner", "Found device: ${result.device.address} RSSI: ${result.rssi}")
                val device = result.device
                postEvent(
                    mapOf(
                        "type" to BleEventTypes.DEVICE_FOUND,
                        "device" to mapOf(
                            "id" to device.address,
                            "name" to device.name,
                            "rssi" to result.rssi,
                        ),
                    ),
                )
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLEScanner", "Scan failed with error code: $errorCode")
            }
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()
        scanner.startScan(null, settings, callback)
        postEvent(mapOf("type" to BleEventTypes.SCAN_STATE, "scanning" to true))
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        callback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        callback = null
        postEvent(mapOf("type" to BleEventTypes.SCAN_STATE, "scanning" to false))
    }

    @SuppressLint("MissingPermission")
    fun connectAndDiscover(deviceId: String): Map<String, Any?> {
        Log.d("BLEScanner", "Starting connectAndDiscover for: $deviceId")
        val bluetoothAdapter = adapter ?: throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
        if (!hasConnectPermission()) throw BleNativeException(BleErrorCodes.PERMISSION_DENIED, "Bluetooth permission required")
        
        var lastFailure: BleNativeException? = null
        
        // Retry logic: Attempt connection up to 3 times
        for (attempt in 1..3) {
            Log.d("BLEScanner", "Connection attempt $attempt for $deviceId")
            try {
                return performDiscovery(bluetoothAdapter, deviceId)
            } catch (e: BleNativeException) {
                lastFailure = e
                Log.w("BLEScanner", "Attempt $attempt failed: ${e.message}")
                // Short delay before retrying
                Thread.sleep(1000)
            }
        }
        
        throw lastFailure ?: BleNativeException(BleErrorCodes.GATT_FAILURE, "Failed after 3 attempts")
    }

    @SuppressLint("MissingPermission")
    private fun performDiscovery(adapter: BluetoothAdapter, deviceId: String): Map<String, Any?> {
        val device = adapter.getRemoteDevice(deviceId)
        val latch = CountDownLatch(1)
        var profile: Map<String, Any?>? = null
        var failure: BleNativeException? = null

        activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d("BLEScanner", "onConnectionStateChange: status=$status, newState=$newState")
                
                if (status == 133) {
                    Log.w("BLEScanner", "Status 133 detected. Attempting to refresh cache...")
                    refreshDeviceCache(gatt)
                }

                if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothProfile.STATE_CONNECTED) {
                    failure = BleNativeException(BleErrorCodes.GATT_FAILURE, "Connection failed with status $status")
                    latch.countDown()
                    gatt.close()
                    return
                }
                
                // Small delay often helps service discovery stability on some Android versions
                Handler(Looper.getMainLooper()).postDelayed({
                    gatt.discoverServices()
                }, 500)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d("BLEScanner", "onServicesDiscovered: status=$status")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failure = BleNativeException(BleErrorCodes.GATT_FAILURE, "Service discovery failed with status $status")
                } else {
                    profile = gatt.toProfileMap()
                }
                latch.countDown()
                gatt.close()
            }
        })

        if (!latch.await(25, TimeUnit.SECONDS)) {
            activeGatt?.close()
            activeGatt = null
            throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Timeout reading device profile")
        }
        
        activeGatt = null
        failure?.let { throw it }
        return profile ?: throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Profile null")
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean {
        return try {
            val method = gatt.javaClass.getMethod("refresh")
            val success = method.invoke(gatt) as Boolean
            Log.d("BLEScanner", "Cache refresh result: $success")
            success
        } catch (e: Exception) {
            Log.e("BLEScanner", "An error occurred while refreshing device cache", e)
            false
        }
    }

    private fun BluetoothGatt.toProfileMap(): Map<String, Any?> {
        val services = services.map { service ->
            mapOf(
                "uuid" to service.uuid.toString().normalizeUuid(),
                "characteristics" to service.characteristics.map { characteristic ->
                    mapOf(
                        "uuid" to characteristic.uuid.toString().normalizeUuid(),
                        "properties" to characteristic.properties.toPropertyFlags(),
                        "permissions" to characteristic.permissions.toPermissionFlags(),
                        "value" to characteristic.value?.let { String(it, Charsets.UTF_8) },
                    )
                },
            )
        }
        return mapOf(
            "id" to device.address,
            "name" to device.name,
            "rssi" to 0,
            "services" to services,
        )
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun postEvent(event: Map<String, Any?>) {
        handler.post { emitEvent(event) }
    }
}