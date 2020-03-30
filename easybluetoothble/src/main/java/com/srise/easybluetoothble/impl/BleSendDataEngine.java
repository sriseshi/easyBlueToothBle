package com.srise.easybluetoothble.impl;

import android.util.Log;

import com.srise.easybluetoothble.Interface.ISendDataEngine;
import com.srise.easybluetoothble.UbtBlueToothConnManager;
import com.srise.easybluetoothble.bean.BaseMessage;

import java.util.ArrayList;
import java.util.List;


public class BleSendDataEngine implements ISendDataEngine {
    private static final String TAG = "BleSendDataEngine";

    /**
     * 如果数据帧长度超过20则分包
     *
     * @param data
     * @return
     */
    @Override
    public List<byte[]> splitDataPacket(byte[] data) {
        Log.d(TAG, " splitDataPacket");

        List<byte[]> bytesList = new ArrayList<>();
        int splitSize = (int) (Math.ceil(data.length / 20.f));
        int remainder = data.length % 20;

        for (int index = 0; index < splitSize; index++) {
            byte[] partData;

            if (index == splitSize - 1) {
                partData = new byte[remainder];
                System.arraycopy(data, (index * 20), partData, 0, remainder);

            } else {
                partData = new byte[20];
                System.arraycopy(data, (index * 20), partData, 0, 20);
            }

            bytesList.add(partData);
        }

        return bytesList;
    }

    /**
     * 发送数据帧
     *
     * @param bleSendMessage
     */
    @Override
    public void sendData(BaseMessage bleSendMessage) {
        Log.d(TAG, " sendData");

        byte[] frameData = bleSendMessage.getMessageBody();
        List<UbtBlueToothConnManager.DataToNotifyPhoneEntity> listBytes = new ArrayList<>();

        if (frameData.length > 20) {
            List<byte[]> bytesArray = splitDataPacket(frameData);

            for (byte[] bytes : bytesArray) {
                listBytes.add(createEntity(bleSendMessage, bytes));
            }
        } else {
            listBytes.add(createEntity(bleSendMessage, frameData));
        }

        if (bleSendMessage.getFinishCallBack() != null) {
            bleSendMessage.getFinishCallBack().readyToSend(listBytes);
        }
    }

    private UbtBlueToothConnManager.DataToNotifyPhoneEntity createEntity(BaseMessage bleSendMessage, byte[] bytes) {
        UbtBlueToothConnManager.DataToNotifyPhoneEntity entity = new UbtBlueToothConnManager.DataToNotifyPhoneEntity();
        entity.setData(bytes);
        entity.setCharacteristic(bleSendMessage.getCharacteristic());
        entity.setDevice(bleSendMessage.getDevice());
        return entity;
    }
}
