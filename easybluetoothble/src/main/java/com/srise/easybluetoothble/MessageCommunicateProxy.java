package com.srise.easybluetoothble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.srise.easybluetoothble.Interface.IMessageDataProcessFinish;
import com.srise.easybluetoothble.Interface.IReceiveMessage;
import com.srise.easybluetoothble.Interface.ISendMessage;
import com.srise.easybluetoothble.bean.BaseMessage;
import com.srise.easybluetoothble.constant.BlueToothConstants;

import java.util.List;

/**
 * Copyright (C), 2015-2019
 * FileName: MessageCommunicateProxy
 * Author: shi.xi
 * Date: 2019/3/20 14:00
 * Description:手机蓝牙通信代理类
 */
public class MessageCommunicateProxy implements IMessageDataProcessFinish {
    private static final String TAG = "MessageCommunicateProxy";
    private IReceiveMessage mReceiveBusiness;
    private ISendMessage mSendBusiness;
    private Boolean mSendingProcessing;
    private Boolean mReceivingProcessing;
    private BluetoothGattCharacteristic mCurrentCharacteristic;

    public MessageCommunicateProxy(IReceiveMessage receiveMessage, ISendMessage sendMessage) {
        mSendingProcessing = false;
        mReceivingProcessing = false;
        mReceiveBusiness = receiveMessage;
        mSendBusiness = sendMessage;
    }

    /**
     * 发送消息
     *
     * @param message
     * @return
     */
    public boolean sendMessage(BaseMessage message) {
        Log.d(TAG, " sendMessage");

        if (mSendBusiness != null && !mSendingProcessing) {
            mSendingProcessing = true;
            message.setFinishCallBack(this);
            mSendBusiness.sendMessage(message);
            return true;
        } else {
            Log.d(TAG, " mSendBusiness: " + mSendBusiness + ", mSendingProcessing: " + mSendingProcessing);

            return false;
        }
    }

    /**
     * 接收消息
     *
     * @param message
     * @return
     */
    public boolean receiveMessage(BaseMessage message) {
        Log.d(TAG, " receiveMessage");

        if (mReceiveBusiness != null) {
            if (!mReceivingProcessing) {
                mReceivingProcessing = true;
                mCurrentCharacteristic = message.getCharacteristic();
                byte[] headFrame = message.getMessageBody();

                if (headFrame[0] != BlueToothConstants.BaseProp.TLV_HEAD
                        && headFrame[0] != BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD) {
                    Log.e(TAG, "not the right data frame: " + headFrame[0]);
                    mReceivingProcessing = false;
                    mReceiveBusiness.resetData();
                    return false;
                }
            } else if (mCurrentCharacteristic == null
                    || !mCurrentCharacteristic.getUuid().toString().equals(message.getCharacteristic().getUuid().toString())) {

                Log.e(TAG, " mReceiveBusiness , mCurrentCharacteristic: "
                        + (mCurrentCharacteristic == null ? "null" : mCurrentCharacteristic.getUuid().toString())
                        + "messageCharacteristic: " + (message.getCharacteristic() == null ? "null" : message.getCharacteristic().getUuid().toString()));
                mReceivingProcessing = false;
                mReceiveBusiness.resetData();
                return false;
            }

            message.setFinishCallBack(this);
            mReceiveBusiness.receiveMessage(message);
            return true;
        } else {
            Log.e(TAG, " mReceiveBusiness is null");

            return false;
        }
    }


    public void destroy() {
        Log.d(TAG, " destroy");

        mReceiveBusiness.destroy();
        mSendBusiness.destroy();
        mCurrentCharacteristic = null;
    }

    public Boolean isSendingProcessing() {
        return mSendingProcessing;
    }

    public Boolean isReceivingProcessing() {
        return mReceivingProcessing;
    }

    public void setSendingProcessing(Boolean sendingProcessing) {
        Log.d(TAG, " setSendingProcessing, " + this.mSendingProcessing + " ==> " + sendingProcessing);
        this.mSendingProcessing = sendingProcessing;
    }

    public void setReceivingProcessing(Boolean receivingProcessing) {
        Log.d(TAG, " setSendingProcessing, " + this.mReceivingProcessing + " ==> " + receivingProcessing);

        this.mReceivingProcessing = receivingProcessing;
    }

    /**
     * 准备向客户端发送消息
     *
     * @param list
     */
    @Override
    public void readyToSend(List<UbtBlueToothConnManager.DataToNotifyPhoneEntity> list) {
        Log.d(TAG, " readyToSend, list: " + (list == null ? "null" : list.size()));

        UbtBlueToothConnManager.getInstance().sendDataFrame(list);
    }

    /**
     * 手机向机器端发送的消息接受完毕
     */
    @Override
    public void finishReceiving() {
        Log.d(TAG, " finishReceiving");

        mReceivingProcessing = false;
        mCurrentCharacteristic = null;
    }
}
