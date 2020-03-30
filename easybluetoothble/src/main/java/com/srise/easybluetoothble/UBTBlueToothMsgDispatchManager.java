package com.srise.easybluetoothble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.srise.easybluetoothble.Interface.IBleGattService;
import com.srise.easybluetoothble.annotation.IDispatcherAnnotation;
import com.srise.easybluetoothble.annotation.IDispatcherMethodAnnotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C), 2015-2019
 * FileName: UBTBlueToothMsgDispatchManager
 * Author: shi.xi
 * Date: 2019/3/20 14:00
 * Description:消息转发管理类
 */
public class UBTBlueToothMsgDispatchManager {
    private static final String TAG = "UBTBlueToothMsgDispatchManager";
    private Map<String, Class> mDispatchClassMap;
    private static UBTBlueToothMsgDispatchManager mInstance;
    private Map<String, BluetoothGattCharacteristic> mAllCharacteristicMap;

    private UBTBlueToothMsgDispatchManager() {
        mDispatchClassMap = new HashMap<>();
        mAllCharacteristicMap = new HashMap<>();
    }

    public Map<String, BluetoothGattCharacteristic> getCharacteristicMap() {
        return mAllCharacteristicMap;
    }

    /**
     * 返回Manager实例
     *
     * @return
     */
    public static UBTBlueToothMsgDispatchManager getInstance() {
        synchronized (UBTBlueToothMsgDispatchManager.class) {
            if (mInstance == null) {
                mInstance = new UBTBlueToothMsgDispatchManager();
            }
        }

        return mInstance;
    }

    public void init(List<IBleGattService> list) {
        if (mAllCharacteristicMap != null) {
            mAllCharacteristicMap.clear();
        }

        for (IBleGattService service : list) {
            parseAnnotation(service.getDispatcher());
            mAllCharacteristicMap.putAll(service.getCharacteristicMap());
        }
    }

    public static synchronized void destroy() {
        Log.d(TAG, " destroy");

        if (mInstance != null) {
            mInstance.innerDestroy();
        }
        mInstance = null;
    }

    private void innerDestroy() {
        mDispatchClassMap.clear();
    }

    /**
     * 转发消息
     *
     * @param characteristic
     * @param requestId
     * @param data
     */
    public void dispatch(BluetoothGattCharacteristic characteristic, int requestId, byte[] data) {
        Log.d(TAG, " dispatch, characteristic: " + characteristic.getUuid().toString()
                + "requestId : " + requestId + ", data: " + (data == null ? "0" : data.length));

        String UUIDFromCharacteristic = characteristic.getUuid().toString();
        Class clazz = mDispatchClassMap.get(UUIDFromCharacteristic);

        if (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();

            if (methods != null) {
                for (Method method : methods) {
                    IDispatcherMethodAnnotation disPatchMethodInfo = method.getAnnotation(IDispatcherMethodAnnotation.class);

                    if (disPatchMethodInfo != null) {
                        String[] UUIDs = disPatchMethodInfo.CharacteristicUUID();
                        int requestIdPara = disPatchMethodInfo.requestId();

                        for (String UUID : UUIDs) {
                            if (UUID.equals(UUIDFromCharacteristic) && requestIdPara == requestId) {
                                try {
                                    method.invoke(clazz.newInstance(), characteristic, data);
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }

                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 适配Service 里 Characteristic 的 UUID 对应的 dispatcher
     *
     * @param clazz
     */
    private void parseAnnotation(Class clazz) {
        IDispatcherAnnotation disPatchInfo = (IDispatcherAnnotation) clazz.getAnnotation(IDispatcherAnnotation.class);

        if (disPatchInfo != null) {
            String[] UUIDList = disPatchInfo.CharacteristicUUID();

            if (UUIDList.length > 0) {
                for (String UUID : UUIDList) {
                    mDispatchClassMap.put(UUID, clazz);
                }
            }
        }
    }
}
