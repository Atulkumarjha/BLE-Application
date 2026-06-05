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

    fun adapterState(): String = when (adapter?.state) {
        BluetoothAdapter.STATE_ON -> "on"
        BluetoothAdapter.STATE_OFF -> "off"
        else -> "unauthorized"
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
        }
        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
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
        val bluetoothAdapter = adapter ?: throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
        if (!hasConnectPermission()) throw BleNativeException(BleErrorCodes.PERMISSION_DENIED, "Bluetooth permission required")
        val device = bluetoothAdapter.getRemoteDevice(deviceId)
        val latch = CountDownLatch(1)
        var profile: Map<String, Any?>? = null
        var failure: BleNativeException? = null
        activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothProfile.STATE_CONNECTED) {
                    failure = BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
                    latch.countDown()
                    gatt.close()
                    return
                }
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failure = BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
                } else {
                    profile = gatt.toProfileMap()
                }
                latch.countDown()
                gatt.close()
            }
        })
        if (!latch.await(15, TimeUnit.SECONDS)) {
            activeGatt?.close()
            activeGatt = null
            throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
        }
        activeGatt = null
        failure?.let { throw it }
        return profile ?: throw BleNativeException(BleErrorCodes.GATT_FAILURE, "Could not read device profile")
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