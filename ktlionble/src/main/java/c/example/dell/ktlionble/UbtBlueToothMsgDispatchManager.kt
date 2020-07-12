package c.example.dell.ktlionble

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import c.example.dell.ktlionble.annotation.IDispatcherAnnotation
import c.example.dell.ktlionble.annotation.IDispatcherMethodAnnotation
import c.example.dell.ktlionble.inter.IBleGattService

class UbtBlueToothMsgDispatchManager private constructor() {
    companion object {
        const val TAG = "UbtBlueToothMsgDispatchManager";

        val sInstance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            UbtBlueToothMsgDispatchManager()
        }

        fun destroy() {
            Log.d(TAG, " destroy");

            sInstance.innerDestroy();
        }
    }

    private val mDispatchClassMap: MutableMap<String, Class<*>> by lazy() {
        HashMap<String, Class<*>>()
    }

    val mAllCharacteristicMap: MutableMap<String, BluetoothGattCharacteristic> by lazy() {
        HashMap<String, BluetoothGattCharacteristic>()
    }

    fun init(list: List<IBleGattService>?) {
        mAllCharacteristicMap.clear()

        list!!.forEach {
            mAllCharacteristicMap.putAll(it.getCharacteristicMap());

            parseAnnotation(it.getDispatcher());
        }
    }


    private fun innerDestroy() {
        mDispatchClassMap.clear()
    }

    fun dispatch(characteristic: BluetoothGattCharacteristic, requestId: Int, data: ByteArray?) {
        Log.d(TAG, " dispatch, characteristic: " + characteristic.uuid.toString()
                + "requestId : " + requestId + ", data: " + (data?.size ?: "0"))

        val UUIDFromCharacteristic = characteristic.uuid.toString()
        val clazz = mDispatchClassMap[UUIDFromCharacteristic]

        if (clazz != null) {
            val methods = clazz.declaredMethods

            methods.forEach {
                val disPatchMethodInfo: IDispatcherMethodAnnotation? = it.getAnnotation(IDispatcherMethodAnnotation::class.java)
                val UUIDs: Array<String> = disPatchMethodInfo!!.CharacteristicUUID
                val requestIdPara: Int = disPatchMethodInfo.requestId

                for (UUID in UUIDs) {
                    if (UUID == UUIDFromCharacteristic && requestIdPara == requestId) {
                        try {
                            it.invoke(clazz.newInstance(), characteristic, data)
                        } catch (e: Exception) {
                            Log.e(TAG, e.message, e)
                        }

                        break
                    }
                }
            }
        }
    }

    private fun parseAnnotation(clazz: Class<*>) {
        val disPatchInfo: IDispatcherAnnotation? = clazz.getAnnotation(IDispatcherAnnotation::class.java)
        if (disPatchInfo != null) {
            val UUIDList: Array<String> = disPatchInfo.CharacteristicUUID

            if (UUIDList.isNotEmpty()) {
                UUIDList.forEach {
                    mDispatchClassMap[it] = clazz
                }
            }
        }
    }
}

