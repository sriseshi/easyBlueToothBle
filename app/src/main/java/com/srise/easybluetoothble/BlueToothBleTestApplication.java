package com.srise.easybluetoothble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.srise.easybluetoothble.example.service.MessageGattService;
import com.srise.easybluetoothble.Interface.IBleGattService;
import com.srise.easybluetoothble.Util.BlueToothUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlueToothBleTestApplication extends Application {
    public static final String TAG = "VisbotApplication";

    private AutoDisconnectBlueToothTask mDisconnectTask;
    private BroadcastReceiver mBleReceiver;
    private boolean mNeedRestartBle = false;
    private static BlueToothBleTestApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        utilsInit();
    }

    /**
     * 初始化一些配置
     */
    private synchronized void utilsInit() {
        mDisconnectTask = new AutoDisconnectBlueToothTask();

        ThreadPoolUtils.run(new Runnable() {
            @Override
            public void run() {
                if (BlueToothUtil.isOpenBle()) {
                    mNeedRestartBle = true;
                    registerBleReceiver();
                    BlueToothUtil.closeBle();
                } else {
                    registerBleReceiver();
                    BlueToothUtil.openBle();
                }
            }
        });
    }

    /**
     * 监听蓝牙广播
     */
    private synchronized void registerBleReceiver() {
        Log.i(TAG, " registerBleReceiver");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        if (mBleReceiver == null) {
            mBleReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (Objects.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

                        Log.i(TAG, " blueState: " + blueState
                                + ", mNeedRestartBle: " + mNeedRestartBle);

                        switch (blueState) {
                            case BluetoothAdapter.STATE_ON:
                                if (!mNeedRestartBle) {
                                    ThreadPoolUtils.run(new Runnable() {
                                        @Override
                                        public void run() {
                                            initBlueTooth();
                                        }
                                    });
                                }

                                break;

                            case BluetoothAdapter.STATE_OFF:
                                UbtBlueToothConnManager.destroy();

                                if (mNeedRestartBle) {
                                    BlueToothUtil.openBle();
                                    mNeedRestartBle = false;
                                }

                                break;

                            default:
                                break;
                        }
                    }
                }
            };
        }

        registerReceiver(mBleReceiver, intentFilter);
    }

    private class AutoDisconnectBlueToothTask implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "AutoDisconnectBlueToothTask run");
            if (BlueToothUtil.isOpenBle()) {
                mNeedRestartBle = true;
                BlueToothUtil.closeBle();
            }
        }
    }

    /**
     * 初始化蓝牙
     */
    public void initBlueTooth() {
        Log.i(TAG, "initBlueTooth");

        List<IBleGattService> list = new ArrayList<>();
        list.add(new MessageGattService());
        UbtBlueToothConnManager.getInstance().addBleStateListener(listener);
        UbtBlueToothConnManager.getInstance().init(getApplicationContext(), Constants.BLUE_TOOTH_PREFIX, "123456", list);
    }

    public UbtBlueToothConnManager.IBleConnectListener getListener() {
        return listener;
    }

    private UbtBlueToothConnManager.IBleConnectListener listener = new UbtBlueToothConnManager.IBleConnectListener() {
        @Override
        public void onClientConnect(BluetoothDevice device) {
            Log.d(TAG, "onClientConnect, device:" + device.getName());

            UbtBlueToothConnManager.getInstance().stopAdvertising();
        }

        @Override
        public void onClientDisconnect() {
            Log.d(TAG, "onClientDisconnect");

            UbtBlueToothConnManager.getInstance().initAdvertiser();
        }

        @Override
        public void onClientFailed() {
            Log.d(TAG, "onClientFailed");

            UbtBlueToothConnManager.getInstance().initAdvertiser();
        }

        @Override
        public void onServerConnect() {

        }

        @Override
        public void onServerDisconnect() {

        }

        @Override
        public void onServerFailed() {

        }

        @Override
        public void advertiseSuccess() {

        }

        @Override
        public void scanResult(ScanResult scanResult) {

        }

        @Override
        public void discoverService(List<BluetoothGattService> obj) {

        }
    };


    public static BlueToothBleTestApplication getApplication(){
        return sInstance;
    }
}
