package c.example.dell.ktlionble.inter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult

interface IBleConnectListener {
    fun onClientConnect(device: BluetoothDevice?)

    fun onClientDisconnect()

    fun onClientFailed()

    fun onServerConnect()

    fun onServerDisconnect()

    fun onServerFailed()

    fun advertiseSuccess()

    fun scanResult(scanResult: ScanResult?)

    fun discoverService(list : List<BluetoothGattService?>?)
}