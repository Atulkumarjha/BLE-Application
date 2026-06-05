// c:\BLE Clone Application\android\app\src\main\kotlin\com\blecloner\blecloner\BLEModels.kt

package com.blecloner.blecloner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

internal object BleChannelNames {
    const val METHOD = "com.blecloner/ble"
    const val EVENTS = "com.blecloner/ble_events"
}

internal object BleEventTypes {
    const val DEVICE_FOUND = "device_found"
    const val SCAN_STATE = "scan_state"
    const val PERIPHERAL_STATE = "peripheral_state"
    const val ADAPTER_STATE = "adapter_state"
}

internal object BleErrorCodes {
    const val BLUETOOTH_OFF = "BLUETOOTH_OFF"
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val GATT_FAILURE = "GATT_FAILURE"
    const val ADVERTISE_FAILED = "ADVERTISE_FAILED"
}

internal data class BleCharacteristicProfile(
    val uuid: String,
    val properties: List<String>,
    val permissions: List<String>,
    val value: String? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uuid" to normalizeUuid(uuid),
        "properties" to properties.map { it.trim().uppercase() },
        "permissions" to permissions.map { it.trim().uppercase() },
        "value" to value,
    )
}

internal data class BleServiceProfile(
    val uuid: String,
    val characteristics: List<BleCharacteristicProfile>,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uuid" to normalizeUuid(uuid),
        "characteristics" to characteristics.map { it.toMap() },
    )
}

internal data class BleDeviceProfile(
    val id: String,
    val name: String?,
    val rssi: Int,
    val services: List<BleServiceProfile>,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "rssi" to rssi,
        "services" to services.map { it.toMap() },
    )

    companion object {
        fun fromMap(raw: Map<String, Any?>): BleDeviceProfile {
            val services = (raw["services"] as? List<*>)?.mapNotNull { service ->
                val serviceMap = service as? Map<*, *> ?: return@mapNotNull null
                val characteristics = (serviceMap["characteristics"] as? List<*>)?.mapNotNull { characteristic ->
                    val item = characteristic as? Map<*, *> ?: return@mapNotNull null
                    BleCharacteristicProfile(
                        uuid = item["uuid"]?.toString().orEmpty(),
                        properties = (item["properties"] as? List<*>)?.map { it.toString() }.orEmpty(),
                        permissions = (item["permissions"] as? List<*>)?.map { it.toString() }.orEmpty(),
                        value = item["value"]?.toString(),
                    )
                }.orEmpty()
                BleServiceProfile(
                    uuid = serviceMap["uuid"]?.toString().orEmpty(),
                    characteristics = characteristics,
                )
            }.orEmpty()
            return BleDeviceProfile(
                id = raw["id"]?.toString().orEmpty(),
                name = raw["name"]?.toString(),
                rssi = (raw["rssi"] as? Number)?.toInt() ?: 0,
                services = services,
            )
        }

        fun fromGatt(device: BluetoothDevice, gatt: BluetoothGatt, rssi: Int, name: String?): BleDeviceProfile {
            return BleDeviceProfile(
                id = device.address,
                name = name ?: device.name,
                rssi = rssi,
                services = gatt.services.map { it.toProfile() },
            )
        }
    }
}

internal fun BluetoothGattService.toProfile(): BleServiceProfile = BleServiceProfile(
    uuid = uuid.toString(),
    characteristics = characteristics.map { it.toProfile() },
)

internal fun BluetoothGattCharacteristic.toProfile(): BleCharacteristicProfile = BleCharacteristicProfile(
    uuid = uuid.toString(),
    properties = properties.toPropertyFlags(),
    permissions = permissions.toPermissionFlags(),
)

internal fun Int.toPropertyFlags(): List<String> = buildList {
    if (this@toPropertyFlags and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
    if (this@toPropertyFlags and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
    if (this@toPropertyFlags and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
    if (this@toPropertyFlags and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
}

internal fun Int.toPermissionFlags(): List<String> = buildList {
    if (this@toPermissionFlags and BluetoothGattCharacteristic.PERMISSION_READ != 0) add("READ")
    if (this@toPermissionFlags and BluetoothGattCharacteristic.PERMISSION_WRITE != 0) add("WRITE")
    if (this@toPermissionFlags and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED != 0) add("WRITE_ENCRYPTED")
    if (this@toPermissionFlags and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED != 0) add("READ_ENCRYPTED")
}

internal class BleNativeException(
    val code: String,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

internal fun normalizeUuid(input: String): String {
    return input.trim().removePrefix("{").removeSuffix("}").uppercase()
}

internal fun String.normalizeUuid(): String = normalizeUuid(this)

internal fun String.toUuid(): java.util.UUID = java.util.UUID.fromString(this)
