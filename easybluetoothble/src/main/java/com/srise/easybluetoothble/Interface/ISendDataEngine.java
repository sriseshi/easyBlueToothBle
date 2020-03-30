package com.srise.easybluetoothble.Interface;

import com.srise.easybluetoothble.bean.BaseMessage;

import java.util.List;

public interface ISendDataEngine {
    /**
     * 根据情况分包
     *
     * @param data
     * @return
     */

    List<byte[]> splitDataPacket(byte[] data);


    /**
     * 发送数据
     *
     * @param bleSendMessage
     * @return
     */
     void sendData(BaseMessage bleSendMessage);
}
