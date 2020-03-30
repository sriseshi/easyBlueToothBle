package com.srise.easybluetoothble.impl;


import android.util.Log;

import com.srise.easybluetoothble.Interface.IReceiveDataEngine;
import com.srise.easybluetoothble.MessageSender;
import com.srise.easybluetoothble.UBTBlueToothMsgDispatchManager;
import com.srise.easybluetoothble.Util.BlueToothUtil;
import com.srise.easybluetoothble.bean.BaseMessage;
import com.srise.easybluetoothble.constant.BlueToothConstants;

import java.util.ArrayList;
import java.util.List;

public class BleReceiveDataEngine implements IReceiveDataEngine {
    private static final String TAG = "BleReceiveDataEngine";
    private List<byte[]> mReceiveList;
    private int mTotalLength = 0;
    private int mReceiveLength = 0;
    private int mNowProcessingMessageType = BlueToothConstants.BleReceiveState.IDLE;

    public BleReceiveDataEngine() {
        mReceiveList = new ArrayList<>();
    }

    @Override
    public void recreateDataFrame(BaseMessage message) {
        Log.d(TAG, " recreateDataFrame");

        if (mTotalLength == 0 && mNowProcessingMessageType == BlueToothConstants.BleReceiveState.IDLE) {
            byte[] headFrame = message.getMessageBody();
            switch (headFrame[0]) {
                case BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD:
                    mNowProcessingMessageType = BlueToothConstants.BleReceiveState.NEW_PROTOCOL;
                    break;

                case BlueToothConstants.BaseProp.TLV_HEAD:
                    mNowProcessingMessageType = BlueToothConstants.BleReceiveState.TLV;
                    break;

                default:
                    break;
            }
        }

        mReceiveLength += message.getMessageBody().length;

        if (mTotalLength == 0) {
            mTotalLength = BlueToothUtil.getDataLength(message.getMessageBody());
        }

        mReceiveList.add(message.getMessageBody());

        if (mTotalLength == mReceiveLength) {
            byte[] innerBytes = new byte[mTotalLength];

            for (int index = 0; index < mReceiveList.size(); index++) {
                System.arraycopy(mReceiveList.get(index), 0, innerBytes, index * 20, mReceiveList.get(index).length);
            }

            byte[] messageBody = null;
            int requestId = 0;
            switch (mNowProcessingMessageType) {
                case BlueToothConstants.BleReceiveState.NEW_PROTOCOL:
                    int encodeType = BlueToothUtil.getContentTypeFromNewProtocol(innerBytes);
                    byte[] contentByte = BlueToothUtil.getContentFromNewProtocal(innerBytes);

                    if (encodeType == MessageSender.PROTOBUF_TYPE) {
                        messageBody = contentByte;
                        requestId = BlueToothUtil.getRequestIdFromNewProtocol(innerBytes);
                    } else if (encodeType == MessageSender.TLV_TYPE) {
                        messageBody = BlueToothUtil.getMessageData(contentByte);
                        requestId = BlueToothUtil.getMessageRequestId(messageBody);
                    }

                    break;

                case BlueToothConstants.BleReceiveState.TLV:
                    messageBody = BlueToothUtil.getMessageData(innerBytes);
                    requestId = BlueToothUtil.getMessageRequestId(messageBody);
                    break;

                default:
                    break;
            }

            if (message.getFinishCallBack() != null) {
                message.getFinishCallBack().finishReceiving();
            }


            UBTBlueToothMsgDispatchManager.getInstance()
                    .dispatch(message.getCharacteristic(),
                            requestId,
                            messageBody);

            resetData();
        }

    }

    @Override
    public void resetData() {
        Log.d(TAG, " resetData");

        mReceiveList.clear();
        mTotalLength = 0;
        mReceiveLength = 0;
        mNowProcessingMessageType = BlueToothConstants.BleReceiveState.IDLE;
    }
}
