package c.example.dell.ktlionble.inter

import c.example.dell.ktlionble.bean.BaseMessage

interface IReceiveMessage {
    fun destroy()
    fun resetData()
    fun receiveMessage(message: BaseMessage)
}