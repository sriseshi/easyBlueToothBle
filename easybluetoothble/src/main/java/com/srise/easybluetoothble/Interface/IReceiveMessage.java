package com.srise.easybluetoothble.Interface;

import com.srise.easybluetoothble.bean.BaseMessage;

public interface IReceiveMessage {
    void receiveMessage(BaseMessage message);

    void destroy();

    void resetData();
}
