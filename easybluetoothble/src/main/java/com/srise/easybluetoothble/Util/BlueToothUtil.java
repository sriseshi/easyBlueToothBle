package com.srise.easybluetoothble.Util;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.srise.easybluetoothble.constant.BlueToothConstants;

public class BlueToothUtil {
    private static String TAG = "BlueToothUtil";

    /**
     * 打开蓝牙
     *
     * @return
     */
    public static boolean openBle() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        boolean success = bluetoothAdapter.enable();

        Log.d(TAG, "openBle, success: " + success);

        return success;
    }

    /**
     * 蓝牙是否打开
     *
     * @return
     */
    public static boolean isOpenBle() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter.isEnabled();
    }

    /**
     * 关闭蓝牙
     */
    public static void closeBle() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        boolean token = bluetoothAdapter.isEnabled();

        Log.d(TAG, "closeBle, token: " + token);

        if (token) {
            bluetoothAdapter.disable();
        }
    }

    /**
     * 组成TLV数据报的messag部分
     * 字节数组格式[requestId][length][length][data]...[data]
     *
     * @return
     */
    public static byte[] CombineTLVMessageBody(byte requestId, byte[] data) {
        if (data.length > BlueToothConstants.BleMathConstant.TwoByteNum - 1) {
            throw new RuntimeException("data is too long");
        }

        byte[] finalByteArray = new byte[data.length + 3];

        finalByteArray[0] = requestId;

        byte[] lengthArray;

        if (data.length > BlueToothConstants.BleMathConstant.OneByteNum) {
            lengthArray = ByteHexHelper.Dec2Hex(data.length);
        } else {
            lengthArray = ByteHexHelper.Dec2Hex(data.length, "00");
        }

        System.arraycopy(lengthArray, 0, finalByteArray, 1, lengthArray.length);
        System.arraycopy(data, 0, finalByteArray, 3, data.length);

        return finalByteArray;
    }

    /**
     * 打包TLV数据帧
     *
     * @param data
     * @return
     */
    public static byte[] packTLVFrameData(byte[] data) {
        byte[] check = new byte[]{0x04, 0x02, 0x00, 0x00};
        byte[] frameHead = new byte[]{0x3F, 0x00};
        byte[] extent;

        if ((data.length + check.length) > BlueToothConstants.BleMathConstant.TwoByteNum - 1) {
            throw new RuntimeException("data is too long");
        }

        if ((data.length + check.length) > 255 && (data.length + check.length) < BlueToothConstants.BleMathConstant.TwoByteNum) {
            byte[] lengthByte = ByteHexHelper.Dec2Hex((data.length + check.length));
            extent = new byte[]{0x02, 0x02, lengthByte[0], lengthByte[1]};
        } else {
            byte[] lengthByte = ByteHexHelper.Dec2Hex((data.length + check.length));
            extent = new byte[]{0x02, 0x01, lengthByte[0]};
        }


        byte[] finalDataPack = new byte[frameHead.length + extent.length + data.length + check.length];
        System.arraycopy(frameHead, 0, finalDataPack, 0, frameHead.length);
        System.arraycopy(extent, 0, finalDataPack, frameHead.length, extent.length);
        System.arraycopy(data, 0, finalDataPack, frameHead.length + extent.length, data.length);
        System.arraycopy(check, 0, finalDataPack, frameHead.length + extent.length + data.length, check.length);
        return finalDataPack;
    }

    /**
     * 打包新协议数据帧数据帧
     * |头部(0x4f, 0x00)|     协议类型(0x01)    |  内容长度(0x00,0x00)  |   命令码(0x01)  |   校验值(0x01, 0x00, 0x00)   |   内容   |
     * 头部： 两个字节，默认写法(0x4f, 0x00)
     * 协议类型： 一个字节， 0x01 TLV_type;  0x02 新协议
     * 内容长度： 两个字节 内容的长度
     * 命令码： 一个字节  命令id
     * 校验值: 三个字节，默认写法（0x01, 0x00, 0x00）
     * 内容：字节数组
     *
     * @param data
     * @return
     */
    public static byte[] packNewProtocolFrameData(byte newProtocolType, byte requestId, byte[] data) {
        byte[] frameHead = new byte[]{0x4F, 0x00};
        byte[] contentLength = ByteHexHelper.Dec2Hex(data.length, "00");
        byte[] check = new byte[]{0x01, 0x00, 0x00};

        byte[] finalDataPack = new byte[frameHead.length + 1 + contentLength.length + 1 + check.length + data.length];
        System.arraycopy(frameHead, 0, finalDataPack, 0, frameHead.length);
        finalDataPack[frameHead.length] = newProtocolType;
        System.arraycopy(contentLength, 0, finalDataPack, frameHead.length + 1, contentLength.length);
        finalDataPack[frameHead.length + 1 + contentLength.length] = requestId;
        System.arraycopy(check, 0, finalDataPack, frameHead.length + 1 + contentLength.length + 1, check.length);
        System.arraycopy(data, 0, finalDataPack, frameHead.length + 1 + contentLength.length + 1 + check.length, data.length);

        return finalDataPack;
    }

    /**
     * 获取新协议数据帧的内容
     *
     * @param data 新协议数据帧
     * @return
     */
    public static byte[] getContentFromNewProtocal(byte[] data) {
        byte[] contentLengthArray = new byte[2];
        System.arraycopy(data, 3, contentLengthArray, 0, 2);
        int contentLength = ByteHexHelper.Hex2DEC(contentLengthArray);
        byte[] content = new byte[contentLength];
        System.arraycopy(data, 9, content, 0, content.length);
        return content;
    }

    /**
     * 获取新协议数据帧的命令码
     *
     * @param data 新协议数据帧
     * @return
     */
    public static int getRequestIdFromNewProtocol(byte[] data) {
        return (int) data[5];
    }

    /**
     * 获取新协议数据帧的内容编码类型
     *
     * @param data 新协议数据帧
     * @return
     */
    public static int getContentTypeFromNewProtocol(byte[] data) {
        return (int) data[2];
    }


    /**
     * 获取TLV格式 message体
     *
     * @param data 数据帧
     * @return
     */
    public static byte[] getMessageData(byte[] data) {
        int messageLengthStartPosition = 6;
        int messageDataStartPosition = 5;

        if (data[3] == 0x02) {
            messageLengthStartPosition++;
            messageDataStartPosition++;
        }

        byte[] dataLength = new byte[2];
        System.arraycopy(data, messageLengthStartPosition, dataLength, 0, dataLength.length);
        int length = ByteHexHelper.Hex2DEC(dataLength);
        byte[] dataValue = new byte[length + 3];
        System.arraycopy(data, messageDataStartPosition, dataValue, 0, dataValue.length);

        return dataValue;
    }

    /**
     * 获取指令id
     *
     * @param data messageBody 字节组
     * @return
     */

    public static int getMessageRequestId(byte[] data) {
        return (int) data[0];
    }

    /**
     * 获取实际数据
     *
     * @param data messageBody 字节组
     * @return
     */

    public static byte[] getMessageTrueData(byte[] data) {
        int messageLengthStartPosition = 1;
        int messageDataStartPosition = 3;

        byte[] dataLength = new byte[2];
        System.arraycopy(data, messageLengthStartPosition, dataLength, 0, dataLength.length);
        int length = ByteHexHelper.Hex2DEC(dataLength);
        byte[] dataValue = new byte[length];
        System.arraycopy(data, messageDataStartPosition, dataValue, 0, dataValue.length);
        return dataValue;
    }

    /**
     * 根据第一帧获取协议总字段长度
     *
     * @param headFrame
     * @return
     */
    public static int getDataLength(byte[] headFrame) {
        switch (headFrame[0]) {
            case BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD:
                byte[] contentLengthArray = new byte[2];
                System.arraycopy(headFrame, 3, contentLengthArray, 0, 2);
                return ByteHexHelper.Hex2DEC(contentLengthArray) + 9;

            case BlueToothConstants.BaseProp.TLV_HEAD:
                int HeadContentLength = (int) headFrame[1]; // head部 内容长度占用字节数
                int extentContentLength = (int) headFrame[3 + HeadContentLength]; // (message + check)长度占用的字节数
                byte[] totalLengthByte = new byte[extentContentLength];  // 生成占用(message + check)长度占用的字节数组

                int headLength = 2 + HeadContentLength; // 头部占用字节数（0x3f + head内容长度 +  实际head内容所占字节数）
                int extentLength = 2 + extentContentLength; // extent部分占用的字节数(0x02 + (message + check)内容长度 + 实际(message + check)内容所占字节数)

                System.arraycopy(headFrame, (headLength + 2), totalLengthByte, 0, totalLengthByte.length);
                int contentLength = ByteHexHelper.Hex2DEC(totalLengthByte); // 获取(message + check)内容所占字节数
                return headLength + extentLength + contentLength;//总长度

            default:
                return -1;
        }

    }
}
