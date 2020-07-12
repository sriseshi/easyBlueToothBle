package c.example.dell.ktlionble.impl

import android.util.Log
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.inter.IReceiveDataEngine
import c.example.dell.ktlionble.inter.IReceiveMessage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReceiveMessageBusiness : IReceiveMessage {
    companion object {
        const val TAG = "ReceiveMessageBusiness"
    }

    private var mReceiveDataEngine: IReceiveDataEngine = BleReceiveDataEngine()
    private var mExecutorService: ExecutorService = Executors.newFixedThreadPool(1)

    override fun destroy() {
        Log.d(TAG, " destroy")
        mExecutorService.shutdownNow()
    }

    override fun resetData() {
        Log.d(TAG, " resetData")
        mReceiveDataEngine.resetData()
    }

    override fun receiveMessage(message: BaseMessage) {
        Log.d(TAG, " receiveMessage")
        mExecutorService.submit(ReceiveTask(message))
    }

    inner class ReceiveTask() : Runnable { //TODO
        private lateinit var message: BaseMessage

        constructor(message: BaseMessage) : this() {
            this.message = message
        }

        override fun run() {
            Log.d(TAG, " ReceiveTask run");

            mReceiveDataEngine.recreateDataFrame(message);
        }
    }
}