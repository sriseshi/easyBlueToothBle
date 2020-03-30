package com.srise.easybluetoothble.Interface;

import com.srise.easybluetoothble.bean.BaseMessage;

public interface IReceiveDataEngine {

    /**
     * 解析数据帧
     *
     * @param message
     */
    void recreateDataFrame(BaseMessage message);

    void resetData();
}
