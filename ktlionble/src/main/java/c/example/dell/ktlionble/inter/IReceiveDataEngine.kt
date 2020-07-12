package c.example.dell.ktlionble.inter

import c.example.dell.ktlionble.bean.BaseMessage

interface IReceiveDataEngine {
    fun recreateDataFrame(message: BaseMessage?)

    fun resetData()
}