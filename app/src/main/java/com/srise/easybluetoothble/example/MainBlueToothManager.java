package com.srise.easybluetoothble.example;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.Log;

import com.srise.easybluetoothble.BlueToothBleTestApplication;
import com.srise.easybluetoothble.Constants;
import com.srise.easybluetoothble.example.service.MessageGattService;
import com.srise.easybluetoothble.Interface.IBleGattService;
import com.srise.easybluetoothble.MessageSender;
import com.srise.easybluetoothble.UBTBlueToothMsgDispatchManager;
import com.srise.easybluetoothble.UbtBlueToothConnManager;

import java.util.List;

public class MainBlueToothManager {
    private static MainBlueToothManager sInstance;
    private static final String TAG = "MainBlueToothManager";
    private boolean mDeviceSearched = false;
    private int mRoleState = -1;
    private static final int SERVER = 1;
    private static final int CLIENT = 0;
    private boolean mReceiveClientShake;
    private boolean mServerHasShake;
    private BluetoothGattCharacteristic mWriteCharacteristic = null;
    private BluetoothGattCharacteristic mReadCharacteristic = null;
    private int mNowState;
    private static final int IDEL = 0;
    private static final int OPENING = 1;
    private static final int OPENED = 2;
    private static final int CLOSING = 3;
    private static final int CLOSED = 4;


    private MainBlueToothManager() {

    }

    public void setReceiveClientShake(boolean receiveClientShake) {
        this.mReceiveClientShake = receiveClientShake;
    }

    public boolean isServerHasShake() {
        return mServerHasShake;
    }

    public BluetoothGattCharacteristic getWriteCharacteristic() {
        return mWriteCharacteristic;
    }

    public BluetoothGattCharacteristic getReadCharacteristic() {
        return mReadCharacteristic;
    }

    public static MainBlueToothManager getInstance() {

        synchronized (MainBlueToothManager.class) {
            if (sInstance == null) {
                sInstance = new MainBlueToothManager();
            }
        }

        return sInstance;
    }

    public void startMakeFriend() {
        Log.d(TAG, "startMakeFriend, mRoleState: " + mRoleState);

        if (mRoleState == -1) {
            UbtBlueToothConnManager.getInstance().clearAllListener();
            UbtBlueToothConnManager.getInstance().addBleStateListener(listener);

            List<IBleGattService> serviceList = UbtBlueToothConnManager.getInstance().getServices();

            if (serviceList != null) {
                serviceList.clear();
//                serviceList.add(new UBTGattFriendService());
                UbtBlueToothConnManager.getInstance().setServices(serviceList);
                UBTBlueToothMsgDispatchManager.getInstance().init(serviceList);
                UbtBlueToothConnManager.getInstance().reStart();
            }
        }
    }


    public void startShake() {
        Log.d(TAG, "startShake, mRoleState: " + mRoleState
                + ", mReceiveClientShake: " + mReceiveClientShake);

        if (mRoleState != -1) {
            MessageSender messageSender = null;

            switch (mRoleState) {
                case SERVER:
                    if (mReceiveClientShake) {
//                        messageSender = new MessageSender.MessageSenderBuilder()
//                                .data("1")
//                                .Characteristic(UBTGattFriendService.UBT_GATT_FRIEND_SERVICE_READ_CHARA_UUID)
//                                .requestId(UBTGattFriendService.ID_SHAKE)
//                                .createMessageSender();

                        UbtBlueToothConnManager.getInstance().sendDataToClient(messageSender);
                    } else {
                        mServerHasShake = true;
                    }

                    break;

                case CLIENT:
//                    messageSender = new MessageSender.MessageSenderBuilder()
//                            .data("1")
//                            .Characteristic(mWriteCharacteristic)
//                            .requestId(UBTGattFriendService.ID_SHAKE)
//                            .createMessageSender();

                    UbtBlueToothConnManager.getInstance().sendDataToServer(messageSender);

                    break;

                default:
                    break;
            }
        }
    }

    private final UbtBlueToothConnManager.IBleConnectListener listener = new UbtBlueToothConnManager.IBleConnectListener() {
        @Override
        public void onClientConnect(BluetoothDevice device) {
            Log.d(TAG, "onClientConnect, device: " + device.getName() + ", mRoleState: " + mRoleState);

            switch (mRoleState) {
                case SERVER:
                    UbtBlueToothConnManager.getInstance().stopAdvertising();
                    break;

                case -1:
                    mDeviceSearched = true;
                    mRoleState = SERVER;
                    UbtBlueToothConnManager.getInstance().stopScan();
                    UbtBlueToothConnManager.getInstance().stopAdvertising();

                default:
                    break;

            }
        }

        @Override
        public void onClientDisconnect() {
            Log.d(TAG, "onClientDisconnect");

            resetService();
        }

        @Override
        public void onClientFailed() {
            Log.d(TAG, "onClientFailed");
        }

        @Override
        public void onServerConnect() {
            Log.d(TAG, "onServerConnect");

            if (mRoleState == CLIENT) {
                UbtBlueToothConnManager.getInstance().getServerBluetoothGatt().discoverServices();
            }
        }

        @Override
        public void onServerDisconnect() {
            Log.d(TAG, "onServerDisconnect");
        }

        @Override
        public void onServerFailed() {
            Log.d(TAG, "onServerFailed");
        }

        @Override
        public void advertiseSuccess() {
            Log.d(TAG, "advertiseSuccess");

            UbtBlueToothConnManager.getInstance().startScan();
        }

        @Override
        public void scanResult(ScanResult scanResult) {
            if (!mDeviceSearched) {
                BluetoothDevice device = scanResult.getDevice();
                ScanRecord scanRecord = scanResult.getScanRecord();

                Log.d(TAG, "scanResult, name: " + device.getName());

//                if (scanRecord != null) {
//                    List<ParcelUuid> list = scanRecord.getServiceUuids();
//                    Log.d(TAG, "scanResult, list: " + (list == null ? "null" : list.size()));
//
//                    if (list != null) {
//                        for (ParcelUuid uuid : list) {
//                            if (UBTGattFriendService.UBT_GATT_FRIEND_SERVICE_UUID.equals(uuid.getUuid().toString())) {
//                                mDeviceSearched = true;
//                                UbtBlueToothConnManager.getInstance().stopScan();
//                                long myDeviceId = Long.parseLong(Constants.SN);
//                                long searchedDeviceId = Long.parseLong(device.getName().substring(device.getName().indexOf("-") + 1));
//
//                                Log.d(TAG, "scanResult, myDeviceId :" + myDeviceId
//                                        + ", searchedDeviceId: " + searchedDeviceId);
//
//                                if (myDeviceId < searchedDeviceId) {
//                                    mRoleState = CLIENT;
//                                    UbtBlueToothConnManager.getInstance().stopAdvertising();
//                                    UbtBlueToothConnManager.getInstance().connectServer(device);
//                                } else {
//                                    mRoleState = SERVER;
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }

        @Override
        public void discoverService(List<BluetoothGattService> obj) {
            for (BluetoothGattService service : obj) {
                Log.d(TAG, " UUID" + service.getUuid().toString());

//                if (service.getUuid().toString().equals(UBTGattFriendService.UBT_GATT_FRIEND_SERVICE_UUID)) {
//
//                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//                        if (UBTGattFriendService.UBT_GATT_FRIEND_SERVICE_WRITE_CHARA_UUID.equals(characteristic.getUuid().toString())) {
//                            mWriteCharacteristic = characteristic;
//
//                        }
//
//                        if (UBTGattFriendService.UBT_GATT_FRIEND_SERVICE_READ_CHARA_UUID.equals(characteristic.getUuid().toString())) {
//                            mReadCharacteristic = characteristic;
//                        }
//                    }
//
//                    UbtBlueToothConnManager.getInstance().getServerBluetoothGatt().setCharacteristicNotification(mReadCharacteristic, true);
//                    MessageSender messageSender = new MessageSender.MessageSenderBuilder()
//                            .data(Constants.SN)
//                            .Characteristic(mWriteCharacteristic)
//                            .requestId(UBTGattFriendService.ID_MAKE_FRIEND)
//                            .createMessageSender();
//
//                    UbtBlueToothConnManager.getInstance().sendDataToServer(messageSender);
//
//                    break;
//                }
            }
        }
    };

    public void disconnect() {
        Log.d(TAG, "disconnect, mRoleState: " + mRoleState);

        if (mRoleState == CLIENT) {
            UbtBlueToothConnManager.getInstance().disConnectServer();
        } else if (mRoleState == SERVER) {
            UbtBlueToothConnManager.getInstance().disConnectClient();
        } else {
            resetService();
        }
    }

    private void resetService() {
        Log.d(TAG, "resetService");

        List<IBleGattService> serviceList = UbtBlueToothConnManager.getInstance().getServices();

        if (serviceList != null) {
            UbtBlueToothConnManager.getInstance().clearAllListener();
            UbtBlueToothConnManager.getInstance().addBleStateListener(BlueToothBleTestApplication.getApplication().getListener());
            serviceList.clear();
            serviceList.add(new MessageGattService());
            UbtBlueToothConnManager.getInstance().setServices(serviceList);
            UBTBlueToothMsgDispatchManager.getInstance().init(serviceList);
            UbtBlueToothConnManager.getInstance().reStart();
        }

        mRoleState = -1;
        mDeviceSearched = false;
        mReceiveClientShake = false;
        mServerHasShake = false;
        mWriteCharacteristic = null;
        mReadCharacteristic = null;
    }
}
