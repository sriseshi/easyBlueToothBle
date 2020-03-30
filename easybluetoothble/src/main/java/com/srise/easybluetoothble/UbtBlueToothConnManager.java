package com.srise.easybluetoothble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import com.srise.easybluetoothble.Interface.IBleGattService;
import com.srise.easybluetoothble.Util.BlueToothUtil;
import com.srise.easybluetoothble.bean.BaseMessage;
import com.srise.easybluetoothble.constant.BlueToothConstants;
import com.srise.easybluetoothble.impl.ReceiveMessageBusiness;
import com.srise.easybluetoothble.impl.SendMessageBusiness;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Copyright (C), 2015-2019
 * FileName: UbtBlueToothConnManager
 * Author: shi.xi
 * Date: 2019/3/20 13:59
 * Description: 蓝牙管理类
 */
public class UbtBlueToothConnManager {
    private static final String TAG = "UbtBlueToothConnManager";
    private Context mContext;
    private String mBleNamePrefix;
    private String mSerialNumber;
    private BluetoothAdapter mBluetoothAdapter;
    private volatile BluetoothGattServer mBlueGattServer;
    private List<IBleConnectListener> mBleConnectListeners;
    private volatile BluetoothDevice mCurrentConnectedClientDevice;
    private LinkedBlockingQueue<DataToNotifyPhoneEntity> mDataQueue;
    private boolean mIsInit = false;
    private ConditionVariable mQueueLock;
    private MessageCommunicateProxy mCommunicateProxy;
    private BluetoothLeAdvertiser mAdvertiser;
    private List<IBleGattService> mGattServices;
    private BluetoothGattService mCurrentService;
    private AdvertiseCallback mAdvCallback;
    private Iterator<IBleGattService> mServiceIterator;
    private BleNotifyThread mNotifyThread;
    private Handler mMainHandler;
    private static UbtBlueToothConnManager mInstance;
    private Map<String, BluetoothGattCharacteristic> mAllCharacteristicMap;
    private int mServiceTryAddTime = 0;
    private BluetoothLeScanner mScanner;
    private BluetoothGatt mServerBluetoothGatt;

    private UbtBlueToothConnManager() {
        mBleConnectListeners = new ArrayList<>();
    }

    /**
     * 初始化蓝牙管理类
     *
     * @param context
     * @param bleNamePrefix
     * @param serialNumber
     * @param gattServices
     */
    public synchronized void init(Context context,
                                  String bleNamePrefix,
                                  String serialNumber,
                                  List<IBleGattService> gattServices) {
        if (mIsInit) {
            Log.i(TAG, " already init");
        } else {
            Log.i(TAG, " init");
            mContext = context;
            mBleNamePrefix = bleNamePrefix;
            mSerialNumber = serialNumber;
            mDataQueue = new LinkedBlockingQueue<>();
            mQueueLock = new ConditionVariable();
            mCommunicateProxy = new MessageCommunicateProxy(new ReceiveMessageBusiness(), new SendMessageBusiness());
            setServices(gattServices);
            mMainHandler = new MyMainHandler(context.getMainLooper(), mBleConnectListeners);
            initBluetooth(mContext);
            UBTBlueToothMsgDispatchManager.getInstance().init(mGattServices);
            mAllCharacteristicMap = UBTBlueToothMsgDispatchManager.getInstance().getCharacteristicMap();
            mIsInit = true;
        }
    }

    private static class MyMainHandler extends Handler {
        private final WeakReference<List<IBleConnectListener>> mBleConnectListenersWeakReference;

        public MyMainHandler(Looper mainLooper, List<IBleConnectListener> list) {
            super(mainLooper);
            mBleConnectListenersWeakReference = new WeakReference<>(list);
        }

        @Override
        public void handleMessage(Message msg) {
            List<IBleConnectListener> bleConnectListeners = mBleConnectListenersWeakReference.get();

            if (bleConnectListeners != null) {
                switch (msg.what) {
                    case BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_SUCCESS:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onClientConnect((BluetoothDevice) msg.obj);
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_FAILED:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onClientFailed();
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_DISCONNECT:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onClientDisconnect();
                            }
                        }

                        break;


                    case BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_SUCCESS:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onServerConnect();
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_DISCONNECT:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onServerDisconnect();
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_FAILED:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.onServerFailed();
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_ADVERTISE_SUCCESS:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.advertiseSuccess();
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_SCAN_RESULT:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.scanResult((ScanResult) msg.obj);
                            }
                        }

                        break;

                    case BlueToothConstants.BleConnectStateWhat.WHAT_DISCOVER_SERVICE:
                        for (IBleConnectListener listener : bleConnectListeners) {
                            if (listener != null) {
                                listener.discoverService((List<BluetoothGattService>) msg.obj);
                            }
                        }

                        break;

                    default:
                        break;
                }
            } else {
                Log.e(TAG, ", MyMainHandler: IBleConnectListener is null");
            }
        }
    }

    /**
     * 初始化蓝牙基本信息
     */
    private synchronized void initBluetooth(final Context context) {
        BluetoothManager mBluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBlueGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBlueGattServer != null && mGattServices != null) {
            Log.i(TAG, " initBluetooth mGattServices: " + mGattServices.size());

            if (mGattServices.size() == 0) {
                initAdvertiser();
            } else {
                mServiceIterator = mGattServices.iterator();
                mCurrentService = mServiceIterator.next().getGattService();
                mBlueGattServer.addService(mCurrentService);
            }

            mCurrentConnectedClientDevice = null;
            stopDataQueue();
            startDataQueue();
        }
    }

    /**
     * 开始读取机器端向手机发送消息的线程
     */
    private void startDataQueue() {
        Log.d(TAG, " startDataQueue");

        mDataQueue.clear();
        mNotifyThread = new BleNotifyThread();
        mNotifyThread.start();
    }

    /**
     * 关闭读取机器端向手机发送消息的线程
     */
    private void stopDataQueue() {
        Log.d(TAG, " stopDataQueue");

        try {
            if (mNotifyThread != null) {
                mNotifyThread.interrupt();
                mNotifyThread = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public synchronized static void destroy() {
        Log.d(TAG, " destroy");

        UBTBlueToothMsgDispatchManager.destroy();

        if (mInstance != null) {
            mInstance.innerDestroy();
            mInstance = null;
        }
    }

    public synchronized void innerDestroy() {
        Log.d(TAG, " innerDestroy");

        if (mIsInit) {
            stopDataQueue();
            closeGattServer();
            stopAdvertising();
            mCommunicateProxy.destroy();
            mCurrentConnectedClientDevice = null;
            mBleConnectListeners.clear();
            mDataQueue.clear();
            mGattServices.clear();
        }

        mIsInit = false;
    }

    public void closeGattServer() {
        Log.d(TAG, " closeGattServer");

        try {
            if (mBlueGattServer != null) {
                mBlueGattServer.close();
            }

            mBlueGattServer = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    public void setServices(List<IBleGattService> services) {
        mGattServices = services;
    }

    public List<IBleGattService> getServices() {
        return mGattServices;
    }

    public void reStart() {
        Log.d(TAG, " reStart");

        stopAdvertising();
        closeGattServer();
        BluetoothManager mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBlueGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        if (mGattServices.size() == 0) {
            initAdvertiser();
        } else {
            mBlueGattServer.getServices().clear();
            mServiceIterator = mGattServices.iterator();
            mCurrentService = mServiceIterator.next().getGattService();
            mBlueGattServer.addService(mCurrentService);
        }
    }

    /**
     * 初始化蓝牙广播信息
     */
    public synchronized void initAdvertiser() {
        Log.d(TAG, " initAdvertiser : " + (mBleNamePrefix + "-" + mSerialNumber));

        if (mAdvertiser == null && BlueToothUtil.isOpenBle()) {
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mBluetoothAdapter.setName(mBleNamePrefix + "-" + mSerialNumber);
            AdvertiseSettings mAdvSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                    .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
                    .build();


            AdvertiseData mAdvData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true) //设置设备的名字是否要在广播的packet
                    .addManufacturerData(0x01AC, new byte[]{0x64, 0x12})//扫描得到的data和设备制造商的id
                    .build();


            AdvertiseData.Builder builder = new AdvertiseData.Builder();

            for (IBleGattService service : mGattServices) {
                builder = builder.addServiceUuid(ParcelUuid.fromString(service.getGattServiceUUID()));
            }

            AdvertiseData scanResponse = builder
                    .addManufacturerData(2, new byte[]{0}) //设备厂商数据，自定义
                    .build();


            if (mAdvertiser != null) {
                if (mAdvCallback == null) {
                    mAdvCallback = new AdvertiseCallback() {
                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            super.onStartSuccess(settingsInEffect);

                            Log.d(TAG, " Advertising onStartSuccess: ");

                            mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_ADVERTISE_SUCCESS);
                        }

                        @Override
                        public void onStartFailure(int errorCode) {
                            super.onStartFailure(errorCode);

                            Log.d(TAG, " Advertising onStartFailure: " + errorCode);
                            mBlueGattServer = null;
                        }
                    };
                }

                Log.d(TAG, " startAdvertising ");

                mAdvertiser.startAdvertising(mAdvSettings, mAdvData, scanResponse, mAdvCallback);
            } else {
                Log.e(TAG, " mAdvertiser is null");
            }
        }
    }

    public void stopAdvertising() {
        Log.d(TAG, " stopAdvertising");

        try {
            if (mAdvertiser != null) {
                mAdvertiser.stopAdvertising(mAdvCallback);
                mAdvertiser = null;
                mAdvCallback = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    public void addBleStateListener(IBleConnectListener listener) {
        Log.d(TAG, "addBleStateListener, listener: " + listener);

        if (mBleConnectListeners != null) {
            mBleConnectListeners.add(listener);

            Log.d(TAG, "addBleStateListener, all start");

            for (IBleConnectListener bleConnectListener : mBleConnectListeners) {
                Log.d(TAG, "addBleStateListener, bleConnectListener: " + bleConnectListener);
            }

            Log.d(TAG, "addBleStateListener, all end");
        }
    }

    public void removeBleStateListener(IBleConnectListener listener) {
        Log.d(TAG, "removeBleStateListener, listener: " + listener);

        if (mBleConnectListeners != null) {
            mBleConnectListeners.remove(listener);

            Log.d(TAG, "addBleStateListener, all start");

            for (IBleConnectListener bleConnectListener : mBleConnectListeners) {
                Log.d(TAG, "addBleStateListener, bleConnectListener: " + bleConnectListener);
            }

            Log.d(TAG, "addBleStateListener, all end");
        }
    }


    public void clearAllListener() {
        Log.d(TAG, "clearAllListener");

        if (mBleConnectListeners != null) {
            mBleConnectListeners.clear();
        }
    }

    /**
     * 返回Manager实例
     *
     * @return
     */
    public static UbtBlueToothConnManager getInstance() {
        synchronized (UbtBlueToothConnManager.class) {
            if (mInstance == null) {
                mInstance = new UbtBlueToothConnManager();
            }
        }

        return mInstance;
    }


    /**
     * 开启一个线程不断的读取要向手机发送的数据
     */
    private class BleNotifyThread extends Thread {
        private int tryTimes = 0;
        private boolean isLock = false;

        public boolean isLock() {
            return isLock;
        }

        @Override
        public void run() {
            Log.d(TAG, " BleNotifyThread: start run");
            do {
                try {
                    DataToNotifyPhoneEntity dataObject = mDataQueue.take();
                    Log.d(TAG, " BleNotifyThread, dataObject.mData: " + dataObject.mData
                            + ", dataObject.getCharacteristic: " + dataObject.getCharacteristic()
                            + ", mBlueGattServer: " + mBlueGattServer
                            + ", mServerBluetoothGatt: " + mServerBluetoothGatt
                            + ", dataObject.getDevice: " + dataObject.getDevice());

                    if (dataObject.mData != null) {
                        if (dataObject.getCharacteristic() != null) {
                            try {
                                dataObject.getCharacteristic().setValue(dataObject.mData);
                                mQueueLock.close();

                                while (tryTimes < 3) {
                                    boolean success = false;


                                    if (dataObject.getDevice() != null && mBlueGattServer != null) {
                                        success = mBlueGattServer.notifyCharacteristicChanged(dataObject.getDevice()
                                                , dataObject.getCharacteristic(), true);

                                    } else if (mServerBluetoothGatt != null) {
                                        Log.d(TAG, " start write");
                                        success = mServerBluetoothGatt.writeCharacteristic(dataObject.getCharacteristic());
                                        Log.d(TAG, " end write");
                                    } else {
                                        success = true;
                                    }

                                    if (success) {
                                        tryTimes = 0;
                                        break;
                                    }

                                    tryTimes++;
                                }

                                if (tryTimes == 0) {
                                    isLock = true;
                                    Log.d(TAG, " start block");
                                    mQueueLock.block();
                                    Log.d(TAG, " end block");
                                    isLock = false;
                                } else {
                                    Log.e(TAG, " BleNotifyThread try failed");

                                    mDataQueue.clear();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!Thread.interrupted());

            Log.d(TAG, " BleNotifyThread: end run");
        }
    }

    /**
     * 向线程队列输入数据，发送到客户端
     *
     * @param dataFrames
     */
    protected synchronized void sendDataFrame(List<DataToNotifyPhoneEntity> dataFrames) {
        Log.d(TAG, " sendDataFrame dataFrames: " + dataFrames.size());

        if (mNotifyThread == null || !mNotifyThread.isAlive()) {
            Log.w(TAG, " mNotifyThread is restarting");
            startDataQueue();
        }

        if (mNotifyThread.isLock()) {
            Log.w(TAG, " mNotifyThread is lock, clear mQueueLock");
            mDataQueue.clear();
            mQueueLock.open();
        }

        if (dataFrames != null && dataFrames.size() > 0) {
            for (DataToNotifyPhoneEntity entity : dataFrames) {
                Log.d(TAG, " start send data, " + entity);
                mDataQueue.offer(entity);
                Log.d(TAG, " end send data, " + entity);
            }
        }

        mCommunicateProxy.setSendingProcessing(false);
    }

    /**
     * 发送数据
     *
     * @param messageSender
     * @return
     */
    public synchronized boolean sendDataToClient(MessageSender messageSender) {
        if (mCurrentConnectedClientDevice != null && (messageSender.mCharacteristic != null || !TextUtils.isEmpty(messageSender.mCharacteristicUUID))) {
            Log.d(TAG, " sendDataToClient data: " + (messageSender.mData == null ? "null " : messageSender.mData.length)
                    + ", mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + " mCharacteristic: " + (messageSender.mCharacteristic == null ? "null" : messageSender.mCharacteristic.getUuid().toString()));

            BaseMessage message = new BaseMessage();
            message.setMessageBody(messageSender.mData);
            message.setDevice(mCurrentConnectedClientDevice);
            if (messageSender.mCharacteristic == null) {
                BluetoothGattCharacteristic characteristic = mAllCharacteristicMap.get(messageSender.mCharacteristicUUID);

                if (characteristic == null) {
                    Log.e(TAG, " sendDataToClient fail, characteristic is null");
                    return false;
                }

                messageSender.mCharacteristic = characteristic;
            }

            message.setCharacteristic(messageSender.mCharacteristic);
            return mCommunicateProxy.sendMessage(message);
        } else {
            Log.e(TAG, " sendDataToClient fail, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + ", messageSender.mCharacteristic: " + messageSender.mCharacteristic
                    + ", messageSender.mCharacteristicUUID: " + messageSender.mCharacteristicUUID);
            return false;
        }
    }

    /**
     * 发送数据到server
     *
     * @param messageSender
     * @return
     */
    public synchronized boolean sendDataToServer(MessageSender messageSender) {
        if (mServerBluetoothGatt != null && (messageSender.mCharacteristic != null || !TextUtils.isEmpty(messageSender.mCharacteristicUUID))) {
            Log.d(TAG, " sendDataToServer data: " + (messageSender.mData == null ? "null " : messageSender.mData.length)
                    + ", mServerBluetoothGatt: " + mServerBluetoothGatt
                    + " mCharacteristic: " + (messageSender.mCharacteristic == null ? "null" : messageSender.mCharacteristic.getUuid().toString()));

            BaseMessage message = new BaseMessage();
            message.setMessageBody(messageSender.mData);
            message.setCharacteristic(messageSender.mCharacteristic);
            return mCommunicateProxy.sendMessage(message);
        } else {
            Log.e(TAG, " sendDataToServer fail, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + ", messageSender.mCharacteristic: " + messageSender.mCharacteristic
                    + ", messageSender.mCharacteristicUUID: " + messageSender.mCharacteristicUUID);
            return false;
        }
    }

    /**
     * 接收到消息
     *
     * @param characteristic
     * @param device
     * @return
     */
    private synchronized boolean receiveData(BluetoothGattCharacteristic characteristic, BluetoothDevice device) {
        Log.d(TAG, " receiveData data: " + (characteristic.getValue() == null ? "null " : characteristic.getValue().length)
                + ", device: " + device + " mCharacteristic: " + (characteristic == null ? "null" : characteristic.getUuid().toString()));

        BaseMessage message = new BaseMessage();
        message.setDevice(device);
        message.setCharacteristic(characteristic);
        message.setMessageBody(characteristic.getValue());
        return mCommunicateProxy.receiveMessage(message);
    }

    /**
     * 接收到消息
     *
     * @param characteristic
     * @return
     */
    private synchronized boolean receiveData(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, " receiveData data: " + (characteristic.getValue() == null ? "null " : characteristic.getValue().length)
                + " mCharacteristic: " + (characteristic == null ? "null" : characteristic.getUuid().toString()));

        BaseMessage message = new BaseMessage();
        message.setCharacteristic(characteristic);
        message.setMessageBody(characteristic.getValue());
        return mCommunicateProxy.receiveMessage(message);
    }

    /**
     * 蓝牙服务回调接口
     */
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            Log.d(TAG, " server onConnectionStateChange, status: " + status + ", newState: " + newState + ", device: " + device);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Message message = mMainHandler.obtainMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_SUCCESS, device);
                    mMainHandler.sendMessage(message);
                    mCurrentConnectedClientDevice = device;
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_DISCONNECT);
                    mCurrentConnectedClientDevice = null;
                    break;

                default:
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_FAILED);
                    mCurrentConnectedClientDevice = null;
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, " onCharacteristicReadRequest ");

            mBlueGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d(TAG, " onNotificationSent");

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, " onNotificationSent: clear mDataQueue");

                mDataQueue.clear();
            }

            mQueueLock.open();
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            Log.d(TAG, " onCharacteristicWriteRequest, value: " + value.length);

            characteristic.setValue(value);
            boolean success = receiveData(characteristic, device);
            mBlueGattServer.sendResponse(device, requestId, success ? BluetoothGatt.GATT_SUCCESS : -1, 0, null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            mBlueGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }


        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            mBlueGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d(TAG, " onServiceAdded: " + service.getUuid().toString());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mServiceTryAddTime = 0;

                if (mServiceIterator.hasNext()) {
                    mBlueGattServer.addService(mServiceIterator.next().getGattService());
                } else {
                    initAdvertiser();
                }
            } else {
                mServiceTryAddTime++;
                Log.e(TAG, " onServiceAdded failed, status:" + status + ", tryTime: "
                        + mServiceTryAddTime + ", mCurrentService: " + mCurrentService.getUuid());

                if (mServiceTryAddTime > BlueToothConstants.BaseProp.SERVICE_ADD_TRY_TIME) {
                    Log.e(TAG, " onServiceAdded final failed");
                } else {
                    mBlueGattServer.addService(mCurrentService);
                }
            }
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!TextUtils.isEmpty(result.getDevice().getName())
                    && result.getDevice().getName().contains("xiaofang-")) {

                Message message = Message.obtain(mMainHandler, BlueToothConstants.BleConnectStateWhat.WHAT_SCAN_RESULT, result);
                mMainHandler.sendMessage(message);
            }
        }
    };

    public void disConnectServer() {
        Log.d(TAG, "disConnectServer start, mServerBluetoothGatt: " + mServerBluetoothGatt);

        if (mServerBluetoothGatt != null) {
            mServerBluetoothGatt.disconnect();
        }

        Log.d(TAG, "disConnectServer end");
    }

    public void disConnectClient() {
        Log.d(TAG, "disConnectClient start, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                + ", mBlueGattServer : " + mBlueGattServer);

        if (mBlueGattServer != null && mCurrentConnectedClientDevice != null) {
            mBlueGattServer.cancelConnection(mCurrentConnectedClientDevice);
        }

        Log.d(TAG, "disConnectClient start");
    }

    public void connectServer(BluetoothDevice device) {
        Log.d(TAG, "connectServer start");

        device.connectGatt(mContext, false, mBluetoothGattCallback);

        Log.d(TAG, "connectServer end");
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Log.d(TAG, " my onConnectionStateChange, status: " + status
                    + ", newState: " + newState
                    + ", service: " + (gatt.getServices() == null ? "null" : gatt.getServices().size()));

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mServerBluetoothGatt = gatt;
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_SUCCESS);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_DISCONNECT);
                    mServerBluetoothGatt = null;
                    break;

                default:
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_FAILED);
                    mServerBluetoothGatt = null;
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");

            mQueueLock.open();
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");

            receiveData(characteristic);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, " my onServicesDiscovered, status: " + status
                    + ", service: " + (gatt.getServices() == null ? "null" : gatt.getServices().size()));

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    if (gatt.getServices() != null && gatt.getServices().size() > 0) {
                        mServerBluetoothGatt = gatt;
                        Message message = Message.obtain(mMainHandler,
                                BlueToothConstants.BleConnectStateWhat.WHAT_DISCOVER_SERVICE,
                                gatt.getServices());
                        mMainHandler.sendMessage(message);
                    }

                    break;

                default:
                    break;
            }
        }
    };

    public BluetoothGatt getServerBluetoothGatt() {
        return mServerBluetoothGatt;
    }

    public void startScan() {
        Log.d(TAG, "startScan start");
        mScanner.startScan(mScanCallback);
        Log.d(TAG, "startScan end");
    }

    public void stopScan() {
        Log.d(TAG, "stopScan start");
        mScanner.stopScan(mScanCallback);
        Log.d(TAG, "stopScan end");
    }

    public static class DataToNotifyPhoneEntity {
        private byte[] mData;
        private BluetoothDevice mDevice;
        private BluetoothGattCharacteristic mCharacteristic;

        public byte[] getData() {
            return mData;
        }

        public void setData(byte[] data) {
            this.mData = data;
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public void setDevice(BluetoothDevice device) {
            this.mDevice = device;
        }

        public BluetoothGattCharacteristic getCharacteristic() {
            return mCharacteristic;
        }

        public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
            this.mCharacteristic = characteristic;
        }
    }

    public interface IBleConnectListener {
        void onClientConnect(BluetoothDevice device);

        void onClientDisconnect();

        void onClientFailed();

        void onServerConnect();

        void onServerDisconnect();

        void onServerFailed();

        void advertiseSuccess();

        void scanResult(ScanResult scanResult);

        void discoverService(List<BluetoothGattService> obj);
    }
}
