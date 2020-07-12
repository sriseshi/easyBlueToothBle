package c.example.dell.ktlionble.bean

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

class DataToNotifyPhoneEntity () {
    lateinit var mData : ByteArray
    lateinit var mDevice : BluetoothDevice
    lateinit var mCharacteristic : BluetoothGattCharacteristic
}