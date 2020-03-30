package com.srise.easybluetoothble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.srise.easybluetoothble.Interface.IMessageTool;
import com.srise.easybluetoothble.Util.BlueToothUtil;
import com.srise.easybluetoothble.Util.ByteHexHelper;

/**
 * Copyright (C), 2015-2019
 * FileName: MessageSender
 * Author: shi.xi
 * Date: 2019/3/20 14:00
 * Description:蓝牙发送实体类
 */
public class MessageSender {
    public static final int TLV_TYPE = 1; //新协议中的的消息体为TLV
    public static final int PROTOBUF_TYPE = 2; //新协议中的的消息体为PB格式

    public byte[] mData;
    public BluetoothGattCharacteristic mCharacteristic;
    public String mCharacteristicUUID;

    public static class MessageSenderBuilder {
        private String mStringData; //字符串数据
        private int mIntData; //整形数据
        private byte[] mData; //二进制数据
        private int mRequestId; //任务ID
        private int mNewProtocolBodyType = -1; //新协议消息体的类型
        private IMessageTool mMessageTool; //消息打包的方法对象（可由用户自定义）
        private String mCharacteristicUUID;//任务对应的CharacteristicUUID
        private BluetoothGattCharacteristic mCharacteristic; // BluetoothGattCharacteristic

        public MessageSenderBuilder() {

        }

        public MessageSenderBuilder data(String data) {
            mStringData = data;
            return this;
        }

        public MessageSenderBuilder data(int data) {
            mIntData = data;
            return this;
        }

        public MessageSenderBuilder data(byte[] data) {
            mData = data;
            return this;
        }

        public MessageSenderBuilder Characteristic(BluetoothGattCharacteristic characteristic) {
            mCharacteristic = characteristic;
            return this;
        }

        public MessageSenderBuilder Characteristic(String characteristicUUID) {
            mCharacteristicUUID = characteristicUUID;
            return this;
        }

        public MessageSenderBuilder requestId(int requestId) {
            mRequestId = requestId;
            return this;
        }

        public MessageSenderBuilder IMessageTool(IMessageTool IMessageTool) {
            mMessageTool = IMessageTool;
            return this;
        }

        public MessageSenderBuilder newProtocolBodyType(int newProtocolBodyType) {
            mNewProtocolBodyType = newProtocolBodyType;
            return this;
        }

        public MessageSender createMessageSender() {
            MessageSender sender = new MessageSender();
            byte[] data;

            if (mMessageTool == null) {
                mMessageTool = new DefaultMessageTool();
            }

            if (mData != null) {
                data = mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mData);
            } else if (mStringData == null) {
                data = mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mIntData);
            } else {
                data = mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mStringData);
            }

            sender.mData = data;
            sender.mCharacteristic = mCharacteristic;
            sender.mCharacteristicUUID = mCharacteristicUUID;
            return sender;
        }
    }


    private static class DefaultMessageTool implements IMessageTool {

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, int data) {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, ByteHexHelper.Dec2Hex(data)));
        }

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, String data) {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, ByteHexHelper.String2Hex(data)));
        }

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, byte[] data) {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, data));
        }
    }
}
