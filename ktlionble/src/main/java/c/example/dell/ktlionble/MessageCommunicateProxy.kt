package c.example.dell.ktlionble

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.bean.DataToNotifyPhoneEntity
import c.example.dell.ktlionble.constants.BlueToothConstants
import c.example.dell.ktlionble.inter.IMessageDataProcessFinish
import c.example.dell.ktlionble.inter.IReceiveMessage
import c.example.dell.ktlionble.inter.ISendMessage

class MessageCommunicateProxy() : IMessageDataProcessFinish {
    private var mSendingProcessing = false
    private var mReceivingProcessing = false
    private lateinit var mReceiveBusiness: IReceiveMessage
    private lateinit var mSendBusiness: ISendMessage
    private var mCurrentCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        const val TAG = "MessageCommunicateProxy"
    }

    constructor(receiveBusiness: IReceiveMessage, sendBusiness: ISendMessage) : this() {
        mSendBusiness = sendBusiness
        mReceiveBusiness = receiveBusiness
    }

    fun sendMessage(message: BaseMessage): Boolean {
        Log.d(TAG, " sendMessage")
        return if (!mSendingProcessing) {
            mSendingProcessing = true
            message.mFinishCallBack = this
            mSendBusiness.sendMessage(message)
            return true
        } else {
            Log.d(TAG, " mSendBusiness: $mSendBusiness, mSendingProcessing: $mSendingProcessing")
            return false
        }
    }


    fun receiveMessage(message: BaseMessage): Boolean {
        Log.d(TAG, " receiveMessage");

        if (mReceiveBusiness != null) {
            if (!mReceivingProcessing) {
                mReceivingProcessing = true;
                mCurrentCharacteristic = message.mCharacteristic
                val headFrame: ByteArray? = message.mMessageBody

                if (headFrame!![0] != BlueToothConstants.BaseProp.TLV_HEAD
                        && headFrame[0] != BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD) {
                    Log.e(TAG, "not the right data frame: " + headFrame[0]);
                    mReceivingProcessing = false;
                    mReceiveBusiness.resetData();
                    return false;
                }
            } else if (mCurrentCharacteristic == null
                    || mCurrentCharacteristic!!.uuid!!.toString() != message.mCharacteristic!!.uuid!!.toString()) {
                Log.e(TAG, " mReceiveBusiness , mCurrentCharacteristic: "
                        + (if (mCurrentCharacteristic == null) "null" else mCurrentCharacteristic!!.uuid.toString())
                        + "messageCharacteristic: " + if (message.mCharacteristic == null) "null" else message.mCharacteristic!!.uuid!!.toString())

                mReceivingProcessing = false;
                mReceiveBusiness.resetData();
                return false;
            }

            message.mFinishCallBack = this
            mReceiveBusiness.receiveMessage(message);
            return true;
        } else {
            Log.e(TAG, " mReceiveBusiness is null");

            return false;
        }
    }

    fun destroy() {
        Log.d(TAG, " destroy")

        mReceiveBusiness.destroy()
        mSendBusiness.destroy()
        mCurrentCharacteristic = null
    }

    fun isSendingProcessing(): Boolean? {
        return mSendingProcessing
    }

    fun isReceivingProcessing(): Boolean? {
        return mReceivingProcessing
    }


    fun setSendingProcessing(sendingProcessing: Boolean) {
        Log.d(TAG, " setSendingProcessing, $mSendingProcessing ==> $sendingProcessing")
        mSendingProcessing = sendingProcessing
    }

    fun setReceivingProcessing(receivingProcessing: Boolean) {
        Log.d(TAG, " setSendingProcessing, $mReceivingProcessing ==> $receivingProcessing")
        mReceivingProcessing = receivingProcessing
    }


    override fun readyToSend(list: MutableList<DataToNotifyPhoneEntity?>) {
        Log.d(TAG, " readyToSend, list: " + (list?.size ?: "null"))

        UbtBlueToothConnManager.sInstance.sendDataFrame(list);
    }

    override fun finishReceiving() {
        Log.d(TAG, " finishReceiving")

        mReceivingProcessing = false
        mCurrentCharacteristic = null
    }
}