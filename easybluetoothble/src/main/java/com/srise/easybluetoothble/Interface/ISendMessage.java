package com.srise.easybluetoothble.Interface;

import com.srise.easybluetoothble.bean.BaseMessage;

public interface ISendMessage {
    void sendMessage(BaseMessage message);

    void destroy();
}
