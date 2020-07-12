package c.example.dell.ktlionble.bean

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import c.example.dell.ktlionble.inter.IMessageDataProcessFinish

class BaseMessage {
    var mMessageBody :  ByteArray? = null
    var mDevice: BluetoothDevice? = null
    var mCharacteristic : BluetoothGattCharacteristic? = null
    var mFinishCallBack : IMessageDataProcessFinish? = null
}