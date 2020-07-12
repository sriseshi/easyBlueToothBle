package c.example.dell.ktlionble.impl

import android.util.Log
import c.example.dell.ktlionble.MessageSender
import c.example.dell.ktlionble.UbtBlueToothMsgDispatchManager
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.constants.BlueToothConstants
import c.example.dell.ktlionble.inter.IReceiveDataEngine
import c.example.dell.ktlionble.util.BlueToothUtil

class BleReceiveDataEngine : IReceiveDataEngine {
    companion object {
        const val TAG = "BleReceiveDataEngine"
    }

    private val mReceiveList: MutableList<ByteArray>? = ArrayList()
    private var mTotalLength = 0
    private var mReceiveLength = 0
    private var mNowProcessingMessageType: Int = BlueToothConstants.BleReceiveState.IDLE

    override fun recreateDataFrame(message: BaseMessage?) {
        Log.d(TAG, " recreateDataFrame")

        if (mTotalLength == 0 && mNowProcessingMessageType == BlueToothConstants.BleReceiveState.IDLE) {
            val headFrame: ByteArray? = message!!.mMessageBody

            when (headFrame!![0]) {
                BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD -> mNowProcessingMessageType = BlueToothConstants.BleReceiveState.NEW_PROTOCOL

                BlueToothConstants.BaseProp.TLV_HEAD -> mNowProcessingMessageType = BlueToothConstants.BleReceiveState.TLV

                else -> {
                }
            }
        }

        mReceiveLength += message!!.mMessageBody!!.size

        if (mTotalLength == 0) {
            mTotalLength = BlueToothUtil.getDataLength(message.mMessageBody!!)
        }

        mReceiveList!!.add(message.mMessageBody!!)

        if (mTotalLength == mReceiveLength) {
            val innerBytes = ByteArray(mTotalLength)

            for (index in mReceiveList.indices) {
                System.arraycopy(mReceiveList[index], 0, innerBytes, index * 20, mReceiveList[index].size)
            }

            var messageBody: ByteArray? = null
            var requestId = 0

            when (mNowProcessingMessageType) {
                BlueToothConstants.BleReceiveState.NEW_PROTOCOL -> {
                    val encodeType: Int = BlueToothUtil.getContentTypeFromNewProtocol(innerBytes)

                    val contentByte: ByteArray? = BlueToothUtil.getContentFromNewProtocal(innerBytes)

                    if (encodeType == MessageSender.PROTOBUF_TYPE) {
                        messageBody = contentByte
                        requestId = BlueToothUtil.getRequestIdFromNewProtocol(innerBytes)
                    } else if (encodeType == MessageSender.TLV_TYPE) {
                        messageBody = BlueToothUtil.getMessageData(contentByte!!)
                        requestId = BlueToothUtil.getMessageRequestId(messageBody!!)
                    }
                }

                BlueToothConstants.BleReceiveState.TLV -> {
                    messageBody = BlueToothUtil.getMessageData(innerBytes)
                    requestId = BlueToothUtil.getMessageRequestId(messageBody!!)
                }

                else -> {
                }
            }

            message.mFinishCallBack!!.finishReceiving()

            UbtBlueToothMsgDispatchManager.sInstance
                    .dispatch(message.mCharacteristic!!,
                            requestId,
                            messageBody)
            resetData()
        }
    }

    override fun resetData() {
        Log.d(TAG, " resetData")

        mReceiveList!!.clear()
        mTotalLength = 0
        mReceiveLength = 0
        mNowProcessingMessageType = BlueToothConstants.BleReceiveState.IDLE
    }
}