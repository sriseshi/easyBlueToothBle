package com.srise.easybluetoothble.bean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.srise.easybluetoothble.Interface.IMessageDataProcessFinish;

public class BaseMessage {
    private byte[] mMessageBody;
    private BluetoothDevice mDevice;
    private BluetoothGattCharacteristic mCharacteristic;
    private IMessageDataProcessFinish mFinishCallBack;

    public IMessageDataProcessFinish getFinishCallBack() {
        return mFinishCallBack;
    }

    public void setFinishCallBack(IMessageDataProcessFinish finishCallBack) {
        this.mFinishCallBack = finishCallBack;
    }

    public byte[] getMessageBody() {
        return mMessageBody;
    }

    public void setMessageBody(byte[] messageBody) {
        this.mMessageBody = messageBody;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.mCharacteristic = characteristic;
    }
}
