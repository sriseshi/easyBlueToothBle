package com.srise.easybluetoothble.example.dispatcher;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.srise.easybluetoothble.Constants;
import com.srise.easybluetoothble.example.service.MessageGattService;
import com.srise.easybluetoothble.MessageSender;
import com.srise.easybluetoothble.UbtBlueToothConnManager;
import com.srise.easybluetoothble.annotation.IDispatcherAnnotation;
import com.srise.easybluetoothble.annotation.IDispatcherMethodAnnotation;

@IDispatcherAnnotation(CharacteristicUUID = {MessageGattService.UBT_GATT_SERVICE_WRITE_CHARA_UUID})
public class MessageDispatcher {
    private static final String TAG = MessageDispatcher.class.getName();

    public MessageDispatcher() {

    }

    @IDispatcherMethodAnnotation(CharacteristicUUID = MessageGattService.UBT_GATT_SERVICE_WRITE_CHARA_UUID, requestId = MessageGattService.ID_ROBOT_SN)
    public void queryRotSn(BluetoothGattCharacteristic characteristic, byte[] data) {
        Log.d(TAG, " : querySn");

        MessageSender messageSender = new MessageSender.MessageSenderBuilder()
                .data(Constants.SN)
                .Characteristic(MessageGattService.UBT_GATT_SERVICE_READ_CHARA_UUID)
                .requestId(MessageGattService.ID_ROBOT_SN)
                .createMessageSender();

        UbtBlueToothConnManager.getInstance().sendDataToClient(messageSender);
    }
}
