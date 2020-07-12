package c.example.dell.ktlionble.util

import android.bluetooth.BluetoothAdapter
import android.util.Log
import c.example.dell.ktlionble.constants.BlueToothConstants

class BlueToothUtil {
    companion object {
        const val TAG = "BlueToothUtil"

        fun isOpenBle(): Boolean {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val success = bluetoothAdapter.enable()
            Log.d(TAG, "openBle, success: $success")

            return success
        }

        fun openBle(): Boolean {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val success = bluetoothAdapter.enable()
            Log.d(TAG, "openBle, success: $success")

            return success
        }

        fun closeBle() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val token = bluetoothAdapter.isEnabled
            Log.d(TAG, "closeBle, token: $token")

            if (token) {
                bluetoothAdapter.disable()
            }
        }

        fun CombineTLVMessageBody(requestId: Byte, data: ByteArray?): ByteArray? {
            if (data!!.size > BlueToothConstants.BleMathConstant.TwoByteNum - 1) {
                throw RuntimeException("data is too long")
            }

            val finalByteArray = ByteArray(data.size + 3)
            finalByteArray[0] = requestId

            val lengthArray: ByteArray? = when{
                data.size > BlueToothConstants.BleMathConstant.OneByteNum -> {
                    ByteHexHelper.Dec2Hex(data.size)
                }

                else ->{
                    ByteHexHelper.Dec2Hex(data.size, "00")
                }
            }

            System.arraycopy(lengthArray!!, 0, finalByteArray, 1, lengthArray!!.size)
            System.arraycopy(data, 0, finalByteArray, 3, data.size)
            return finalByteArray
        }


        fun packTLVFrameData(data: ByteArray?): ByteArray? {
            val check = byteArrayOf(0x04, 0x02, 0x00, 0x00)
            val frameHead = byteArrayOf(0x3F, 0x00)
            val extent: ByteArray

            if (data!!.size + check.size > BlueToothConstants.BleMathConstant.TwoByteNum - 1) {
                throw java.lang.RuntimeException("data is too long")
            }

            extent = if (data.size + check.size > 255 && data.size + check.size < BlueToothConstants.BleMathConstant.TwoByteNum) {
                val lengthByte: ByteArray? = ByteHexHelper.Dec2Hex(data.size + check.size)
                byteArrayOf(0x02, 0x02, lengthByte!![0], lengthByte[1])
            } else {
                val lengthByte: ByteArray? = ByteHexHelper.Dec2Hex(data.size + check.size)
                byteArrayOf(0x02, 0x01, lengthByte!![0])
            }

            val finalDataPack = ByteArray(frameHead.size + extent.size + data.size + check.size)
            System.arraycopy(frameHead, 0, finalDataPack, 0, frameHead.size)
            System.arraycopy(extent, 0, finalDataPack, frameHead.size, extent.size)
            System.arraycopy(data, 0, finalDataPack, frameHead.size + extent.size, data.size)
            System.arraycopy(check, 0, finalDataPack, frameHead.size + extent.size + data.size, check.size)
            return finalDataPack
        }


        fun packNewProtocolFrameData(newProtocolType: Byte, requestId: Byte, data: ByteArray): ByteArray? {
            val frameHead = byteArrayOf(0x4F, 0x00)
            val contentLength: ByteArray? = ByteHexHelper.Dec2Hex(data.size, "00")
            val check = byteArrayOf(0x01, 0x00, 0x00)
            val finalDataPack = ByteArray(frameHead.size + 1 + contentLength!!.size + 1 + check.size + data.size)
            System.arraycopy(frameHead, 0, finalDataPack, 0, frameHead.size)
            finalDataPack[frameHead.size] = newProtocolType
            System.arraycopy(contentLength, 0, finalDataPack, frameHead.size + 1, contentLength.size)
            finalDataPack[frameHead.size + 1 + contentLength.size] = requestId
            System.arraycopy(check, 0, finalDataPack, frameHead.size + 1 + contentLength.size + 1, check.size)
            System.arraycopy(data, 0, finalDataPack, frameHead.size + 1 + contentLength.size + 1 + check.size, data.size)
            return finalDataPack
        }


        fun getContentFromNewProtocal(data: ByteArray?): ByteArray? {
            val contentLengthArray = ByteArray(2)
            System.arraycopy(data!!, 3, contentLengthArray, 0, 2)
            val contentLength: Int = ByteHexHelper.Hex2DEC(contentLengthArray)
            val content = ByteArray(contentLength)
            System.arraycopy(data, 9, content, 0, content.size)
            return content
        }

        fun getRequestIdFromNewProtocol(data: ByteArray): Int {
            return data[5].toInt()
        }

        fun getContentTypeFromNewProtocol(data: ByteArray): Int {
            return data[2].toInt()
        }


        fun getMessageData(data: ByteArray): ByteArray? {
            var messageLengthStartPosition = 6
            var messageDataStartPosition = 5

            if (data[3].toInt() == 0x02) {
                messageLengthStartPosition++
                messageDataStartPosition++
            }

            val dataLength = ByteArray(2)
            System.arraycopy(data, messageLengthStartPosition, dataLength, 0, dataLength.size)
            val length: Int = ByteHexHelper.Hex2DEC(dataLength)
            val dataValue = ByteArray(length + 3)
            System.arraycopy(data, messageDataStartPosition, dataValue, 0, dataValue.size)
            return dataValue
        }

        fun getMessageRequestId(data: ByteArray): Int {
            return data[0].toInt()
        }


        fun getMessageTrueData(data: ByteArray?): ByteArray? {
            val messageLengthStartPosition = 1
            val messageDataStartPosition = 3
            val dataLength = ByteArray(2)
            System.arraycopy(data!!, messageLengthStartPosition, dataLength, 0, dataLength.size)
            val length: Int = ByteHexHelper.Hex2DEC(dataLength)
            val dataValue = ByteArray(length)
            System.arraycopy(data, messageDataStartPosition, dataValue, 0, dataValue.size)
            return dataValue
        }

        fun getDataLength(headFrame: ByteArray): Int {
            return when (headFrame[0]) {
                BlueToothConstants.BaseProp.NEW_PROTOCOL_HEAD -> {
                    val contentLengthArray = ByteArray(2)
                    System.arraycopy(headFrame, 3, contentLengthArray, 0, 2)
                    ByteHexHelper.Hex2DEC(contentLengthArray) + 9
                }
                BlueToothConstants.BaseProp.TLV_HEAD -> {
                    val headContentLength = headFrame[1].toInt() // head部 内容长度占用字节数
                    val extentContentLength = headFrame[3 + headContentLength].toInt() // (message + check)长度占用的字节数
                    val totalLengthByte = ByteArray(extentContentLength) // 生成占用(message + check)长度占用的字节数组
                    val headLength = 2 + headContentLength // 头部占用字节数（0x3f + head内容长度 +  实际head内容所占字节数）
                    val extentLength = 2 + extentContentLength // extent部分占用的字节数(0x02 + (message + check)内容长度 + 实际(message + check)内容所占字节数)
                    System.arraycopy(headFrame, headLength + 2, totalLengthByte, 0, totalLengthByte.size)
                    val contentLength: Int = ByteHexHelper.Hex2DEC(totalLengthByte) // 获取(message + check)内容所占字节数
                    headLength + extentLength + contentLength //总长度
                }
                else -> -1
            }
        }
    }
}