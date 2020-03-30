package com.srise.easybluetoothble.impl;

import android.util.Log;

import com.srise.easybluetoothble.Interface.ISendDataEngine;
import com.srise.easybluetoothble.Interface.ISendMessage;
import com.srise.easybluetoothble.bean.BaseMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SendMessageBusiness implements ISendMessage {
    private final static String TAG = "SendMessageBusiness";

    private ISendDataEngine mSendEngine;
    private ExecutorService mExecutorService;

    public SendMessageBusiness() {
        mSendEngine = new BleSendDataEngine();
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void sendMessage(BaseMessage message) {
        Log.d(TAG , " sendMessage");
        mExecutorService.submit(new SendTask(message));
    }

    public void destroy() {
        Log.d(TAG , " destroy");
        mExecutorService.shutdownNow();
    }

    private class SendTask implements Runnable {
        private BaseMessage mMessage;

        public SendTask(BaseMessage message) {
            mMessage = message;
        }

        @Override
        public void run() {
            Log.d(TAG , " SendTask run");

            mSendEngine.sendData(mMessage);
        }
    }
}
