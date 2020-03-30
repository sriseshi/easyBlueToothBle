package com.srise.easybluetoothble.Interface;


import com.srise.easybluetoothble.UbtBlueToothConnManager;

import java.util.List;

public interface IMessageDataProcessFinish {
    void readyToSend(List<UbtBlueToothConnManager.DataToNotifyPhoneEntity> list);

    void finishReceiving();
}
