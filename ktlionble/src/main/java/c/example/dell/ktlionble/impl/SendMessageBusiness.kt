package c.example.dell.ktlionble.impl

import android.util.Log
import c.example.dell.ktlionble.bean.BaseMessage
import c.example.dell.ktlionble.inter.ISendDataEngine
import c.example.dell.ktlionble.inter.ISendMessage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors;

class SendMessageBusiness : ISendMessage {
    companion object {
        const val TAG = "SendMessageBusiness"
    }

    private val mSendEngine: ISendDataEngine? = BleSendDataEngine()
    private val mExecutorService: ExecutorService? = Executors.newSingleThreadExecutor()

    override fun sendMessage(message: BaseMessage) {
        Log.d(TAG, " sendMessage")
        mExecutorService!!.submit(SendTask(message))
    }

    override fun destroy() {
        Log.d(TAG, " destroy");
        mExecutorService!!.shutdownNow();
    }

    inner class SendTask() : Runnable { //TODO
        private var mMessage: BaseMessage? = null

        constructor(message: BaseMessage) : this() {
            mMessage = message
        }

        override fun run() {
            Log.d(TAG, " SendTask run")
            mSendEngine!!.sendData(mMessage!!)
        }
    }
}