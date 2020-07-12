package c.example.dell.ktlionble.inter

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

interface IBleGattService {
    fun getCharacteristicMap(): Map<out String, BluetoothGattCharacteristic>

    fun getDispatcher() : Class<*>

    fun getGattService(): BluetoothGattService?

    fun getGattServiceUUID(): String?
}