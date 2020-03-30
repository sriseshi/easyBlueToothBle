package com.srise.easybluetoothble.example.service;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.srise.easybluetoothble.example.dispatcher.MessageDispatcher;
import com.srise.easybluetoothble.Interface.IBleGattService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright (C), 2015-2019
 * FileName: MessageGattService
 * Author: shi.xi
 * Date: 2019/3/20 16:39
 * Description:BleGatt服务
 */

public class MessageGattService implements IBleGattService {
    public static final String TAG = MessageGattService.class.getName();
    public static final String UBT_GATT_SERVICE_UUID = "0000ccc0-0000-1000-8000-00805f9b34fb";
    public static final String UBT_GATT_SERVICE_WRITE_CHARA_UUID = "0000ccc1-0000-1000-8000-00805f9b34fb";
    public static final String UBT_GATT_SERVICE_READ_CHARA_UUID = "0000ccc2-0000-1000-8000-00805f9b34fb";

    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic normalBluetoothGattWriteCharacteristic;
    private BluetoothGattCharacteristic normalBluetoothGattReadCharacteristic;
    private Map<String, BluetoothGattCharacteristic> mAllCharacteristicMap;

    public static final byte ID_GO_FORWARD = 0x11;

    public static final byte ID_GO_BACKFORWARD = 0x12;

    public static final byte ID_TURN_LEFT = 0x13;

    public static final byte ID_TURN_RIGHT = 0x14;

    public static final byte ID_STOP = 0x30;

    public static final byte ID_POW_OFF = 0x12;

    public static final byte ID_ACTION_ROBOT = 0x15;
    /**
     * 应用端查询机器端版本号
     */
    public static final byte ID_QUERY_MACHINE_VERSION = 0x46;

    /**
     * 应用端查询机器端wifi名称的type
     */
    public static final byte ID_QUERY_WIFI_SSID = 0x43;

    /**
     * 设备wifi扫描结果
     */
    public static final byte ID_SCAN_WIFI = 0x42;

    /**
     * 设备序列号
     */
    public static final byte ID_ROBOT_SN = 0x41;

    /**
     * 电池电量
     */
    public static final byte ID_ROBOT_BATTERY = 0x40;

    /**
     * 设备端查询机器端wifi开关状态
     */
    public static final byte ID_QUERY_WIFI_SWITCH = 0x44;

    /**
     * 设备端查询机器端wifi连接的ssid和状态
     */
    public static final byte ID_QUERY_WIFI_SSID_AND_STATUS = 0x45;

    /**
     * 根据设备端传输的wifi信息连接wifi
     */
    public static final byte ID_CONNECT_WIFI = (byte) 0x80;

    /**
     * wifi名称的type
     */
    public static final byte ID_WIFI_SSID = 0x05;

    /**
     * wifi加密方式的type
     */
    public static final byte ID_WIFI_CIPHER = 0x06;

    /**
     * wifi信号强度的type
     */
    public static final byte ID_WIFI_RSSI = 0x07;

    /**
     * wifi密码的type
     */
    public static final byte ID_WIFI_PASSWORD = 0x08;

    /**
     * wifi连接状态
     */
    public static final byte ID_WIFI_CONNECT_STATUS = 0x09;

    public MessageGattService() {
        mAllCharacteristicMap = new HashMap<>();
        mBluetoothGattService = new BluetoothGattService(UUID.fromString(UBT_GATT_SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        normalBluetoothGattWriteCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(UBT_GATT_SERVICE_WRITE_CHARA_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

        normalBluetoothGattReadCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(UBT_GATT_SERVICE_READ_CHARA_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

        mAllCharacteristicMap.put(UBT_GATT_SERVICE_WRITE_CHARA_UUID, normalBluetoothGattWriteCharacteristic);
        mAllCharacteristicMap.put(UBT_GATT_SERVICE_READ_CHARA_UUID, normalBluetoothGattReadCharacteristic);

        mBluetoothGattService.addCharacteristic(normalBluetoothGattWriteCharacteristic);
        mBluetoothGattService.addCharacteristic(normalBluetoothGattReadCharacteristic);

    }

    @Override
    public BluetoothGattService getGattService() {
        Log.d(TAG, ", getGattService, UBT_GATT_SERVICE_UUID: "
                + UBT_GATT_SERVICE_UUID + ", UBT_GATT_SERVICE_WRITE_CHARA_UUID: " + UBT_GATT_SERVICE_WRITE_CHARA_UUID);
        return mBluetoothGattService;
    }

    public Map<String, BluetoothGattCharacteristic> getCharacteristicMap() {
        return mAllCharacteristicMap;
    }

    @Override
    public String getGattServiceUUID() {
        return UBT_GATT_SERVICE_UUID;
    }

    @Override
    public Class<?> getDispatcher() {
        return MessageDispatcher.class;
    }
}
