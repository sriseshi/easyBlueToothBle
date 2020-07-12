package c.example.dell.ktlionble

import android.bluetooth.BluetoothGattCharacteristic
import c.example.dell.ktlionble.inter.IMessageTool
import c.example.dell.ktlionble.util.BlueToothUtil
import c.example.dell.ktlionble.util.ByteHexHelper

class MessageSender {

    companion object {
        const val TLV_TYPE = 1 //新协议中的的消息体为TLV
        const val PROTOBUF_TYPE = 2 //新协议中的的消息体为PB格式
    }

    var mData: ByteArray? = null
    var mCharacteristic: BluetoothGattCharacteristic? = null
    var mCharacteristicUUID: String? = null

    class MessageSenderBuilder {
        private var mStringData: String? = null
        private var mIntData = 0
        private var mData: ByteArray? = null
        private var mRequestId = 0
        private var mNewProtocolBodyType = -1 //新协议消息体的类型
        private lateinit var mMessageTool: IMessageTool
        private var mCharacteristicUUID: String? = null
        private var mCharacteristic: BluetoothGattCharacteristic? = null

        fun data(data: String): MessageSenderBuilder {
            mStringData = data;
            return this;
        }

        fun data(data: Int): MessageSenderBuilder? {
            mIntData = data
            return this
        }

        fun data(data: ByteArray): MessageSenderBuilder? {
            mData = data
            return this
        }

        fun Characteristic(characteristic: BluetoothGattCharacteristic): MessageSenderBuilder? {
            mCharacteristic = characteristic
            return this
        }

        fun Characteristic(characteristicUUID: String): MessageSenderBuilder? {
            mCharacteristicUUID = characteristicUUID
            return this
        }

        fun requestId(requestId: Int): MessageSenderBuilder? {
            mRequestId = requestId
            return this
        }

        fun IMessageTool(IMessageTool: IMessageTool): MessageSenderBuilder? {
            mMessageTool = IMessageTool
            return this
        }

        fun newProtocolBodyType(newProtocolBodyType: Int): MessageSenderBuilder? {
            mNewProtocolBodyType = newProtocolBodyType
            return this
        }

        fun createMessageSender(): MessageSender? {
            val sender: MessageSender = MessageSender()
            var data: ByteArray? = null

            if (mMessageTool == null) {
                mMessageTool = DefaultMessageTool()
            }

            data = when {
                mData != null -> {
                    mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mData)
                }
                mStringData == null -> {
                    mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mIntData)
                }
                else -> {
                    mMessageTool.encodeData(mNewProtocolBodyType, mRequestId, mStringData)
                }
            }

            sender.mData = data
            sender.mCharacteristic = mCharacteristic
            sender.mCharacteristicUUID = mCharacteristicUUID
            return sender
        }
    }

    class DefaultMessageTool : IMessageTool {
        override fun encodeData(newProtocolType: Int, requestId: Int, data: Int): ByteArray? {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody(requestId.toByte(), ByteHexHelper.Dec2Hex(data)));
        }

        override fun encodeData(newProtocolType: Int, requestId: Int, data: String?): ByteArray? {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody(requestId.toByte(), ByteHexHelper.String2Hex(data!!)));
        }

        override fun encodeData(newProtocolType: Int, requestId: Int, data: ByteArray?): ByteArray? {
            return BlueToothUtil.packTLVFrameData(BlueToothUtil.CombineTLVMessageBody(requestId.toByte(), data!!));
        }
    }
}