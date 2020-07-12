package c.example.dell.ktlionble.inter

import c.example.dell.ktlionble.bean.DataToNotifyPhoneEntity

interface IMessageDataProcessFinish {
    fun readyToSend(list: MutableList<DataToNotifyPhoneEntity?>)
    fun finishReceiving()
}