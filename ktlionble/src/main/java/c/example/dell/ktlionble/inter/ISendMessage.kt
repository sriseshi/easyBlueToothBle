package c.example.dell.ktlionble.inter

import c.example.dell.ktlionble.bean.BaseMessage

interface ISendMessage {
    fun sendMessage(message: BaseMessage)
    fun destroy()
}