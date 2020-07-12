package c.example.dell.ktlionble.inter

import c.example.dell.ktlionble.bean.BaseMessage

interface ISendDataEngine {
    /**
     * 根据情况分包
     *
     * @param data
     * @return
     */
    fun splitDataPacket(data: ByteArray?): List<ByteArray?>?


    /**
     * 发送数据
     *
     * @param bleSendMessage
     * @return
     */
    fun sendData(bleSendMessage: BaseMessage)
}