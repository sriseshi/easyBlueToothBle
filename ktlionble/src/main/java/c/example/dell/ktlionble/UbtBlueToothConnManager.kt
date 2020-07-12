package c.example.dell.ktlionble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.*
import android.text.TextUtils
import android.util.Log
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.bean.DataToNotifyPhoneEntity
import c.example.dell.ktlionble.constants.BlueToothConstants
import c.example.dell.ktlionble.impl.ReceiveMessageBusiness
import c.example.dell.ktlionble.impl.SendMessageBusiness
import c.example.dell.ktlionble.inter.IBleConnectListener
import c.example.dell.ktlionble.inter.IBleGattService
import c.example.dell.ktlionble.util.BlueToothUtil
import java.lang.ref.WeakReference
import java.util.concurrent.LinkedBlockingQueue

class UbtBlueToothConnManager private constructor() {
    private lateinit var mContext: Context
    private lateinit var mBleNamePrefix: String
    private lateinit var mSerialNumber: String
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    @Volatile
    private var mBlueGattServer: BluetoothGattServer? = null

    @Volatile
    private var mCurrentConnectedClientDevice: BluetoothDevice? = null

    private var mIsInit: Boolean = false
    private lateinit var mQueueLock: ConditionVariable
    private lateinit var mCommunicateProxy: MessageCommunicateProxy
    private var mAdvertiser: BluetoothLeAdvertiser? = null
    private lateinit var mGattServices: MutableList<IBleGattService>
    private var mAdvCallback: AdvertiseCallback? = null
    private var mCurrentService: BluetoothGattService? = null
    private lateinit var mServiceIterator: Iterator<IBleGattService>
    private var mNotifyThread: BleNotifyThread? = null//TODO
    private lateinit var mMainHandler: Handler //TODO
    private lateinit var mAllCharacteristicMap: Map<String, BluetoothGattCharacteristic>
    private var mServiceTryAddTime: Int = 0
    private lateinit var mScanner: BluetoothLeScanner
    private var mServerBluetoothGatt: BluetoothGatt? = null
    private lateinit var mDataQueue: LinkedBlockingQueue<DataToNotifyPhoneEntity>
    private val mBleConnectListeners: MutableList<IBleConnectListener> by lazy() {
        ArrayList<IBleConnectListener>()
    }

    companion object {
        const val TAG = "UbtBlueToothConnManager"

        val sInstance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UbtBlueToothConnManager()
        }

        fun destroy() {
            Log.d(TAG, " destroy")

            UbtBlueToothMsgDispatchManager.destroy()

            sInstance.innerDestroy()
        }
    }

    @Synchronized
    fun init(context: Context, bleNamePrefix: String, serialNumber: String, gattServices: MutableList<IBleGattService>) {
        when (mIsInit) {
            true -> {
                Log.i(TAG, " already init")
            }

            else -> {
                Log.i(TAG, " init")

                mContext = context
                mBleNamePrefix = bleNamePrefix
                mSerialNumber = serialNumber
                mQueueLock = ConditionVariable()
                mDataQueue = LinkedBlockingQueue<DataToNotifyPhoneEntity>()
                mCommunicateProxy = MessageCommunicateProxy(ReceiveMessageBusiness(), SendMessageBusiness())
                mGattServices = gattServices
                mMainHandler = MyMainHandler(context.mainLooper, mBleConnectListeners)
                initBluetooth(mContext)
                UbtBlueToothMsgDispatchManager.sInstance.init(mGattServices)
                mAllCharacteristicMap = UbtBlueToothMsgDispatchManager.sInstance.mAllCharacteristicMap
                mIsInit = true
            }
        }
    }

    @Synchronized
    private fun initBluetooth(context: Context) {
        var bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        mBlueGattServer = bluetoothManager.openGattServer(mContext, mGattServerCallback)
        mScanner = mBluetoothAdapter.bluetoothLeScanner

        Log.i(TAG, " initBluetooth mGattServices: " + mGattServices.size)

        when (mGattServices.size) {
            0 -> {
                initAdvertiser()
            }

            else -> {
                mServiceIterator = mGattServices.iterator()
                mCurrentService = mServiceIterator.next().getGattService()
                mBlueGattServer!!.addService(mCurrentService)
            }
        }

        mCurrentConnectedClientDevice = null
        stopDataQueue()
        startDataQueue()
    }

    private fun stopDataQueue() {
        Log.d(TAG, " stopDataQueue")

        try {
            mNotifyThread!!.interrupt()
            mNotifyThread = null
        } catch (exception: Exception) {
            Log.e(TAG, exception.message, exception)
        }
    }

    @Synchronized
    private fun innerDestroy() {
        Log.d(TAG, " innerDestroy")

        if (mIsInit) {
            stopDataQueue()
            closeGattServer()
            stopAdvertising()
            mCommunicateProxy.destroy()
            mCurrentConnectedClientDevice = null
            mBleConnectListeners.clear()
            mDataQueue.clear()
            mGattServices.clear()
        }

        mIsInit = false
    }

    fun stopAdvertising() {
        Log.d(TAG, " stopAdvertising")

        try {
            mAdvertiser!!.stopAdvertising(mAdvCallback)
            mAdvertiser = null
            mAdvCallback = null
        } catch (exception: Exception) {
            Log.e(TAG, exception.message, exception)
        }
    }

    fun closeGattServer() {
        Log.d(TAG, " closeGattServer")

        try {
            mBlueGattServer!!.close()
            mBlueGattServer = null
        } catch (exception: Exception) {
            Log.e(TAG, exception.message, exception)
        }
    }

    private fun startDataQueue() {
        Log.d(TAG, " startDataQueue")
        mDataQueue.clear()
        mNotifyThread = BleNotifyThread()
        mNotifyThread!!.start()
    }

    fun setServices(services: MutableList<IBleGattService>) {
        mGattServices = services
    }

    fun getServices(): MutableList<IBleGattService> {
        return mGattServices
    }

    fun reStart() {
        Log.d(TAG, " reStart")

        stopAdvertising()
        closeGattServer()

        val mBluetoothManager: BluetoothManager = mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBlueGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback)

        if (mGattServices.size == 0) {
            initAdvertiser()
        } else {
            mBlueGattServer!!.services.clear()
            mServiceIterator = mGattServices.iterator()
            mCurrentService = mServiceIterator.next().getGattService()
            mBlueGattServer!!.addService(mCurrentService)
        }
    }

    fun addBleStateListener(listener: IBleConnectListener) {
        Log.d(TAG, "addBleStateListener, listener: $listener")

        mBleConnectListeners.add(listener)

        Log.d(TAG, "addBleStateListener, all start")

        mBleConnectListeners.forEach() {
            Log.d(TAG, "addBleStateListener, bleConnectListener: $it")
        }

        Log.d(TAG, "addBleStateListener, all end")
    }

    fun removeBleStateListener(listener: IBleConnectListener) {
        Log.d(TAG, "removeBleStateListener, listener: $listener")

        mBleConnectListeners.remove(listener)

        Log.d(TAG, "addBleStateListener, all start")

        mBleConnectListeners.forEach() {
            Log.d(TAG, "addBleStateListener, bleConnectListener: $it")
        }

        Log.d(TAG, "addBleStateListener, all end")
    }

    fun clearAllListener() {
        mBleConnectListeners.clear()
    }


    @Synchronized
    fun sendDataFrame(dataFrames: List<DataToNotifyPhoneEntity?>) {
        Log.d(TAG, " sendDataFrame dataFrames: " + dataFrames.size)

        if (mNotifyThread == null || !mNotifyThread!!.isAlive) {
            Log.w(TAG, " mNotifyThread is restarting")
            startDataQueue()
        }

        if (mNotifyThread!!.isLock()) {
            Log.w(TAG, " mNotifyThread is lock, clear mQueueLock")
            mDataQueue.clear()
            mQueueLock.open()
        }

        if (dataFrames.isNotEmpty()) {
            dataFrames.forEach() {
                Log.d(TAG, " start send data, $it")
                mDataQueue.offer(it)
                Log.d(TAG, " end send data, $it")
            }
        }
    }

    @Synchronized
    fun sendDataToClient(messageSender: MessageSender): Boolean {
        if (mCurrentConnectedClientDevice != null && (messageSender.mCharacteristic != null || !TextUtils.isEmpty(messageSender.mCharacteristicUUID))) {
            Log.d(TAG, " sendDataToClient data: " + (if (messageSender.mData == null) "null " else messageSender.mData!!.size)
                    + ", mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + " mCharacteristic: " + if (messageSender.mCharacteristic == null) "null" else messageSender.mCharacteristic!!.uuid.toString())

            val message: BaseMessage = BaseMessage()
            message.mMessageBody = messageSender.mData
            message.mDevice = mCurrentConnectedClientDevice

            if (messageSender.mCharacteristic == null) {
                val characteristic: BluetoothGattCharacteristic? = mAllCharacteristicMap.get(messageSender.mCharacteristicUUID)

                if (characteristic == null) {
                    Log.e(TAG, " sendDataToClient fail, characteristic is null")
                    return false
                }

                messageSender.mCharacteristic = characteristic
            }

            message.mCharacteristic = messageSender.mCharacteristic
            return mCommunicateProxy.sendMessage(message)
        } else {
            Log.e(TAG, " sendDataToClient fail, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + ", messageSender.mCharacteristic: " + messageSender.mCharacteristic
                    + ", messageSender.mCharacteristicUUID: " + messageSender.mCharacteristicUUID)
            return false
        }
    }


    @Synchronized
    fun sendDataToServer(messageSender: MessageSender): Boolean {
        if (mServerBluetoothGatt != null && (messageSender.mCharacteristic != null || !TextUtils.isEmpty(messageSender.mCharacteristicUUID))) {
            Log.d(TAG, " sendDataToServer data: " + (if (messageSender.mData == null) "null " else messageSender.mData!!.size)
                    + ", mServerBluetoothGatt: " + mServerBluetoothGatt
                    + " mCharacteristic: " + if (messageSender.mCharacteristic == null) "null" else messageSender.mCharacteristic!!.uuid.toString())

            val message: BaseMessage = BaseMessage()
            message.mMessageBody = messageSender.mData
            message.mCharacteristic = messageSender.mCharacteristic
            return mCommunicateProxy.sendMessage(message)
        } else {
            Log.e(TAG, " sendDataToServer fail, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                    + ", messageSender.mCharacteristic: " + messageSender.mCharacteristic
                    + ", messageSender.mCharacteristicUUID: " + messageSender.mCharacteristicUUID)
            return false
        }
    }

    @Synchronized
    fun initAdvertiser() {
        Log.d(TAG, " initAdvertiser : $mBleNamePrefix-$mSerialNumber")

        if (mAdvertiser == null && BlueToothUtil.isOpenBle()) {
            mAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
            mBluetoothAdapter.name = "$mBleNamePrefix-$mSerialNumber"

            val mAdvSettings: AdvertiseSettings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                    .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
                    .build()
            val mAdvData: AdvertiseData = AdvertiseData.Builder()
                    .setIncludeDeviceName(true) //设置设备的名字是否要在广播的packet
                    .addManufacturerData(0x01AC, byteArrayOf(0x64, 0x12))//扫描得到的data和设备制造商的id
                    .build()

            var builder: AdvertiseData.Builder = AdvertiseData.Builder()

            mGattServices.forEach() {
                builder = builder.addServiceUuid(ParcelUuid.fromString(it.getGattServiceUUID()))
            }

            val scanResponse: AdvertiseData = builder
                    .addManufacturerData(2, byteArrayOf(0)) //设备厂商数据，自定义
                    .build()

            if (mAdvertiser != null) {
                if (mAdvCallback == null) {
                    mAdvCallback = object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                            super.onStartSuccess(settingsInEffect)

                            Log.d(TAG, " Advertising onStartSuccess: ")

                            mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_ADVERTISE_SUCCESS)
                        }

                        override fun onStartFailure(errorCode: Int) {
                            super.onStartFailure(errorCode)

                            Log.d(TAG, " Advertising onStartFailure: $errorCode")

                            mBlueGattServer = null
                        }
                    }
                }

                Log.d(TAG, " startAdvertising ")

                mAdvertiser!!.startAdvertising(mAdvSettings, mAdvData, scanResponse, mAdvCallback)
            } else {
                Log.e(TAG, " mAdvertiser is null")
            }
        }
    }

    private var mGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d(TAG, " server onConnectionStateChange, status: $status, newState: $newState, device: $device")

            when (status) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val message: Message = mMainHandler.obtainMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_SUCCESS, device)
                    mMainHandler.sendMessage(message)
                    mCurrentConnectedClientDevice = device
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_DISCONNECT)
                    mCurrentConnectedClientDevice = null
                }

                else -> {
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_FAILED)
                    mCurrentConnectedClientDevice = null
                }
            }

        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Log.d(TAG, " onCharacteristicReadRequest ")

            mBlueGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Log.d(TAG, " onNotificationSent")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, " onNotificationSent: clear mDataQueue")

                mDataQueue.clear()
            }

            mQueueLock.open()
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d(TAG, " onCharacteristicWriteRequest, value: " + value?.size ?: "0")

            characteristic!!.value = value

            var success: Boolean = receiveData(characteristic, device)

            var status = -1

            if (success) {
                status = BluetoothGatt.GATT_SUCCESS
            }

            mBlueGattServer!!.sendResponse(device, requestId, status, 0, null)
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            mBlueGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            mBlueGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d(TAG, " onServiceAdded: " + service!!.uuid.toString())

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mServiceTryAddTime = 0

                if (mServiceIterator.hasNext()) {
                    mBlueGattServer!!.addService(mServiceIterator.next().getGattService())
                } else {
                    initAdvertiser()
                }
            } else {
                mServiceTryAddTime++

                Log.e(TAG, " onServiceAdded failed, status:" + status + ", tryTime: "
                        + mServiceTryAddTime + ", mCurrentService: " + mCurrentService!!.uuid)

                if (mServiceTryAddTime > BlueToothConstants.BaseProp.SERVICE_ADD_TRY_TIME) {
                    Log.e(TAG, " onServiceAdded final failed")
                } else {
                    mBlueGattServer!!.addService(mCurrentService)
                }
            }
        }
    }


    @Synchronized
    private fun receiveData(characteristic: BluetoothGattCharacteristic, device: BluetoothDevice?): Boolean {
        Log.d(TAG, " receiveData data: " + (if (characteristic.value == null) "null " else characteristic.value.size)
                + ", device: " + device + " mCharacteristic: " + (characteristic?.uuid?.toString()
                ?: "null"))

        val message = BaseMessage()
        message.mDevice = device
        message.mCharacteristic = characteristic
        message.mMessageBody = characteristic.value
        return mCommunicateProxy.receiveMessage(message)
    }

    @Synchronized
    private fun receiveData(characteristic: BluetoothGattCharacteristic): Boolean {
        Log.d(TAG, " receiveData data: " + (if (characteristic.value == null) "null " else characteristic.value.size)
                + " mCharacteristic: " + (characteristic?.uuid?.toString() ?: "null"))

        val message: BaseMessage = BaseMessage()
        message.mCharacteristic = characteristic
        message.mMessageBody = characteristic.value
        return mCommunicateProxy.receiveMessage(message)
    }

    private var mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (!TextUtils.isEmpty(result!!.device!!.name)
                    && result.device!!.name.contains("xiaofang-")) {
                val message: Message = Message.obtain(mMainHandler, BlueToothConstants.BleConnectStateWhat.WHAT_SCAN_RESULT, result)
                mMainHandler.sendMessage(message)
            }
        }
    }

    fun disConnectServer() {
        Log.d(TAG, "disConnectServer start, mServerBluetoothGatt: $mServerBluetoothGatt")

        mServerBluetoothGatt!!.disconnect()

        Log.d(TAG, "disConnectServer end")
    }

    fun disConnectClient() {
        Log.d(TAG, "disConnectClient start, mCurrentConnectedClientDevice: " + mCurrentConnectedClientDevice
                + ", mBlueGattServer : " + mBlueGattServer)

        mBlueGattServer!!.cancelConnection(mCurrentConnectedClientDevice!!)

        Log.d(TAG, "disConnectClient start")
    }

    fun connectServer(device: BluetoothDevice) {
        Log.d(TAG, "connectServer start")
        device.connectGatt(mContext, false, mBluetoothGattCallback)
        Log.d(TAG, "connectServer end")
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            Log.d(TAG, " my onConnectionStateChange, status: " + status
                    + ", newState: " + newState
                    + ", service: " + if (gatt!!.services == null) "null" else gatt!!.services.size)

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    mServerBluetoothGatt = gatt
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_SUCCESS)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_DISCONNECT)
                    mServerBluetoothGatt = null
                }

                else -> {
                    mMainHandler.sendEmptyMessage(BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_FAILED)
                    mServerBluetoothGatt = null
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
        }
    }

    fun getServerBluetoothGatt(): BluetoothGatt? {
        return mServerBluetoothGatt
    }

    fun startScan() {
        Log.d(TAG, "startScan start")
        mScanner.startScan(mScanCallback)
        Log.d(TAG, "startScan end")
    }

    fun stopScan() {
        Log.d(TAG, "stopScan start")
        mScanner.stopScan(mScanCallback)
        Log.d(TAG, "stopScan end")
    }

    private inner class MyMainHandler(mainLooper: Looper) : Handler(mainLooper) {
        var mBleConnectListenersWeakReference: WeakReference<List<IBleConnectListener>>? = null

        constructor(mainLooper: Looper, list: List<IBleConnectListener>) : this(mainLooper) {
            mBleConnectListenersWeakReference = WeakReference(list)
        }

        override fun dispatchMessage(msg: Message) {
            var bleConnectListeners: List<IBleConnectListener>? = mBleConnectListenersWeakReference!!.get()

            bleConnectListeners!!.forEach {
                when (msg.what) {
                    BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_SUCCESS -> {
                        it.onClientConnect(msg.obj as BluetoothDevice)
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_FAILED -> {
                        it.onClientFailed()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_CLIENT_CONN_DISCONNECT -> {
                        it.onClientDisconnect()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_SUCCESS -> {
                        it.onServerConnect()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_DISCONNECT -> {
                        it.onServerDisconnect()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_CONN_SERVER_FAILED -> {
                        it.onServerFailed()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_ADVERTISE_SUCCESS -> {
                        it.advertiseSuccess()
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_SCAN_RESULT -> {
                        it.scanResult(msg.obj as ScanResult)
                    }

                    BlueToothConstants.BleConnectStateWhat.WHAT_DISCOVER_SERVICE -> {
                        it.discoverService(msg.obj as List<BluetoothGattService>)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    /**
     * 开启一个线程不断的读取要向手机发送的数据
     */
    private inner class BleNotifyThread() : Thread() {
        var mTryTime: Int = 0
        var mIsLock: Boolean = false

        fun isLock(): Boolean {
            return mIsLock
        }

        @Override
        override fun run() {
            Log.d(TAG, " BleNotifyThread: start run")
            do {
                try {
                    val dataObject = mDataQueue.take()

                    Log.d(TAG, " BleNotifyThread, dataObject.mData: " + dataObject.mData
                            + ", dataObject.getCharacteristic: " + dataObject.mCharacteristic
                            + ", mBlueGattServer: " + mBlueGattServer
                            + ", mServerBluetoothGatt: " + mServerBluetoothGatt
                            + ", dataObject.getDevice: " + dataObject.mDevice)

                    dataObject.mCharacteristic.value = dataObject.mData
                    mQueueLock.close()

                    var success = false

                    while (mTryTime < 3) {
                        when {
                            mBlueGattServer != null -> {
                                success = mBlueGattServer!!.notifyCharacteristicChanged(dataObject.mDevice
                                        , dataObject.mCharacteristic, true)
                            }

                            mServerBluetoothGatt != null -> {
                                Log.d(TAG, " start write")
                                success = mServerBluetoothGatt!!.writeCharacteristic(dataObject.mCharacteristic)
                                Log.d(TAG, " end write")
                            }

                            else -> {
                                success = true
                            }

                        }

                        if (success) {
                            mTryTime = 0
                            break
                        }

                        when (mTryTime) {
                            0 -> {
                                mIsLock = true
                                Log.d(TAG, " start block")
                                mQueueLock.block()
                                Log.d(TAG, " end block")
                                mIsLock = false
                            }
                            else -> {
                                Log.e(TAG, " BleNotifyThread try failed")

                                mDataQueue.clear()
                            }
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, exception.message, exception)
                }
            } while (!interrupted())
        }
    }
}