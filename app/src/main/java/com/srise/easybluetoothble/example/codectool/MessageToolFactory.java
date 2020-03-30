package com.srise.easybluetoothble.example.codectool;


import com.srise.easybluetoothble.Interface.IMessageTool;
import com.srise.easybluetoothble.MessageSender;
import com.srise.easybluetoothble.Util.BlueToothUtil;
import com.srise.easybluetoothble.Util.ByteHexHelper;

/**
 * Copyright (C), 2015-2019
 * FileName: MessageToolFactory
 * Author: shi.xi
 * Date: 2019/3/20 16:39
 * Description:蓝牙数据MessageTool的工厂方法
 */

public class MessageToolFactory {
    public static final int ENCODE_TYPE_NEW_PROTOCOL = 1;

    public synchronized static IMessageTool createMessageTool(int encodeType) {
        IMessageTool mMessageTool;

        switch (encodeType) {
            case ENCODE_TYPE_NEW_PROTOCOL:
            default:
                mMessageTool = new encodeDataNormalWithNewProtocol();
                break;
        }

        return mMessageTool;
    }

    private static class encodeDataNormalWithNewProtocol implements IMessageTool {
        private byte[] finalByte = null;

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, int data) {
            byte[] message = BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, ByteHexHelper.Dec2Hex(data)));
            finalByte = BlueToothUtil.packNewProtocolFrameData((byte) MessageSender.TLV_TYPE, (byte) requestId, message);
            return finalByte;
        }

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, String data) {
            byte[] message = BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, ByteHexHelper.String2Hex(data)));
            finalByte = BlueToothUtil.packNewProtocolFrameData((byte) MessageSender.TLV_TYPE, (byte) requestId, message);
            return finalByte;
        }

        @Override
        public byte[] encodeData(int newProtocolType, int requestId, byte[] data) {
            switch (newProtocolType) {
                case MessageSender.TLV_TYPE:
                    byte[] message = BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody((byte) requestId, data));
                    finalByte = BlueToothUtil.packNewProtocolFrameData((byte) newProtocolType, (byte) requestId, message);
                    break;

                case MessageSender.PROTOBUF_TYPE:
                    finalByte = BlueToothUtil.packNewProtocolFrameData((byte) newProtocolType, (byte) requestId, data);
                    break;
                default:
                    break;
            }

            return finalByte;
        }
    }
}
