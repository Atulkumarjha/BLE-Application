// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\BLEPeripheral.kt

package com.blecloner.blecloner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

internal class BLEPeripheral(
    private val context: Context,
    private val emitEvent: (Map<String, Any?>) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private val valueStore = mutableMapOf<String, ByteArray>()

    @SuppressLint("MissingPermission")
    fun startPeripheral(profile: Map<String, Any?>) {
        stopPeripheral()
        val bluetoothAdapter = adapter ?: throw BleNativeException(BleErrorCodes.BLUETOOTH_OFF, "Bluetooth adapter unavailable")
        if (!bluetoothAdapter.isEnabled) throw BleNativeException(BleErrorCodes.BLUETOOTH_OFF, "Please enable Bluetooth")
        if (!hasAdvertisePermission()) throw BleNativeException(BleErrorCodes.PERMISSION_DENIED, "Bluetooth permission required")
        val parsedProfile = BleDeviceProfile.fromMap(profile)
        val bluetoothAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            ?: throw BleNativeException(BleErrorCodes.ADVERTISE_FAILED, "Could not start peripheral mode")
        gattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: android.bluetooth.BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic,
            ) {
                val value = valueStore[characteristic.uuid.toString()] ?: ByteArray(0)
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }

            override fun onCharacteristicWriteRequest(
                device: android.bluetooth.BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                valueStore[characteristic.uuid.toString()] = value
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        }) ?: throw BleNativeException(BleErrorCodes.ADVERTISE_FAILED, "Could not start peripheral mode")
        parsedProfile.services.forEach { serviceProfile ->
            val service = BluetoothGattService(serviceProfile.uuid.toUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY)
            serviceProfile.characteristics.forEach { characteristicProfile ->
                val characteristic = BluetoothGattCharacteristic(
                    characteristicProfile.uuid.toUuid(),
                    characteristicProfile.properties.toCharacteristicMask(),
                    characteristicProfile.permissions.toPermissionMask(),
                )
                characteristicProfile.value?.toByteArray(Charsets.UTF_8)?.let { bytes ->
                    valueStore[characteristic.uuid.toString()] = bytes
                    characteristic.value = bytes
                }
                service.addCharacteristic(characteristic)
            }
            gattServer?.addService(service)
        }
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .apply { parsedProfile.services.firstOrNull()?.let { addServiceUuid(android.os.ParcelUuid(it.uuid.toUuid())) } }
            .build()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()
        advertiser = bluetoothAdvertiser
        advertiser?.startAdvertising(settings, advertiseData, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopPeripheral() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        gattServer?.close()
        gattServer = null
        valueStore.clear()
        postEvent(mapOf("type" to BleEventTypes.PERIPHERAL_STATE, "advertising" to false))
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            postEvent(
                mapOf(
                    "type" to BleEventTypes.PERIPHERAL_STATE,
                    "advertising" to false,
                    "error" to "Could not start peripheral mode",
                ),
            )
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            postEvent(mapOf("type" to BleEventTypes.PERIPHERAL_STATE, "advertising" to true))
        }
    }

    private fun List<String>.toCharacteristicMask(): Int {
        var flags = 0
        forEach { value ->
            flags = flags or when (value.uppercase()) {
                "READ" -> BluetoothGattCharacteristic.PROPERTY_READ
                "WRITE" -> BluetoothGattCharacteristic.PROPERTY_WRITE
                "WRITE_NO_RESPONSE" -> BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                "NOTIFY" -> BluetoothGattCharacteristic.PROPERTY_NOTIFY
                "INDICATE" -> BluetoothGattCharacteristic.PROPERTY_INDICATE
                else -> 0
            }
        }
        return flags
    }

    private fun List<String>.toPermissionMask(): Int {
        var flags = 0
        forEach { value ->
            flags = flags or when (value.uppercase()) {
                "READ" -> BluetoothGattCharacteristic.PERMISSION_READ
                "WRITE" -> BluetoothGattCharacteristic.PERMISSION_WRITE
                "READ_ENCRYPTED" -> BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                "WRITE_ENCRYPTED" -> BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
                else -> 0
            }
        }
        return flags
    }

    private fun hasAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun postEvent(event: Map<String, Any?>) {
        handler.post { emitEvent(event) }
    }
}