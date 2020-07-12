package c.example.dell.ktlionble.impl

import android.util.Log
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.bean.DataToNotifyPhoneEntity
import c.example.dell.ktlionble.inter.ISendDataEngine
import java.util.*
import kotlin.math.ceil

class BleSendDataEngine : ISendDataEngine {
    companion object {
        const val TAG = "BleSendDataEngine"
    }


    override fun splitDataPacket(data: ByteArray?): List<ByteArray?>? {
        Log.d(TAG, " splitDataPacket")

        val bytesList: MutableList<ByteArray> = ArrayList()
        val splitSize = ceil(data!!.size / 20f.toDouble()).toInt()
        val remainder = data.size % 20

        for (index in 0 until splitSize) {
            var partData: ByteArray

            if (index == splitSize - 1) {
                partData = ByteArray(remainder)
                System.arraycopy(data, index * 20, partData, 0, remainder)
            } else {
                partData = ByteArray(20)
                System.arraycopy(data, index * 20, partData, 0, 20)
            }

            bytesList.add(partData)
        }

        return bytesList
    }

    override fun sendData(bleSendMessage: BaseMessage) {
        Log.d(TAG, " sendData")

        val frameData: ByteArray = bleSendMessage.mMessageBody!!
        val listBytes: MutableList<DataToNotifyPhoneEntity?> = ArrayList<DataToNotifyPhoneEntity?>()

        if (frameData.size > 20) {
            val bytesArray = splitDataPacket(frameData)

            for (bytes in bytesArray!!) {
                listBytes.add(createEntity(bleSendMessage, bytes!!))
            }
        } else {
            listBytes.add(createEntity(bleSendMessage, frameData))
        }

        bleSendMessage.mFinishCallBack!!.readyToSend(listBytes)
    }

    private fun createEntity(bleSendMessage: BaseMessage, bytes: ByteArray): DataToNotifyPhoneEntity? {
        val entity: DataToNotifyPhoneEntity = DataToNotifyPhoneEntity()
        entity.mData = bytes
        entity.mCharacteristic = bleSendMessage.mCharacteristic!!
        entity.mDevice = bleSendMessage.mDevice!!

        return entity
    }
}