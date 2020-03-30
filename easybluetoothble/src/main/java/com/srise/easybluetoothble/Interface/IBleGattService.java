package com.srise.easybluetoothble.Interface;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.Map;


public interface IBleGattService {
    /**
     * 获取service
     *
     * @return
     */
    BluetoothGattService getGattService();

    /**
     * 获取service UUID
     *
     * @return
     */
    String getGattServiceUUID();


    /**
     * 获取对应service的dispatcher
     *
     * @return
     */
    Class<?> getDispatcher();

    /**
     * 获取对应service的BluetoothGattCharacteristic map
     *
     * @return
     */
    Map<String, BluetoothGattCharacteristic> getCharacteristicMap();
}
