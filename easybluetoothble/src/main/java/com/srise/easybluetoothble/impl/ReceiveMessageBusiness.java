package com.srise.easybluetoothble.impl;


import android.util.Log;

import com.srise.easybluetoothble.Interface.IReceiveDataEngine;
import com.srise.easybluetoothble.Interface.IReceiveMessage;
import com.srise.easybluetoothble.bean.BaseMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ReceiveMessageBusiness implements IReceiveMessage {
    private static final String TAG = "ReceiveMessageBusiness";
    private IReceiveDataEngine mReceiveDataEngine;
    private ExecutorService mExecutorService;

    public ReceiveMessageBusiness() {
        mReceiveDataEngine = new BleReceiveDataEngine();
        mExecutorService = Executors.newFixedThreadPool(1);
    }


    @Override
    public void receiveMessage(BaseMessage message) {
        Log.d(TAG, " receiveMessage");
        mExecutorService.submit(new ReceiveTask(message));
    }

    public void destroy() {
        Log.d(TAG, " destroy");
        mExecutorService.shutdownNow();
    }

    @Override
    public void resetData() {
        Log.d(TAG, " resetData");
        mReceiveDataEngine.resetData();
    }

    private class ReceiveTask implements Runnable {
        private BaseMessage mMessage;

        public ReceiveTask(BaseMessage message) {
            mMessage = message;
        }

        @Override
        public void run() {
            Log.d(TAG, " ReceiveTask run");

            mReceiveDataEngine.recreateDataFrame(mMessage);
        }
    }
}
