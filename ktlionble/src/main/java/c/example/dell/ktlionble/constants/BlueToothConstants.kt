package c.example.dell.ktlionble.constants

object BlueToothConstants {
    /**
     * 蓝牙基本信息属性
     */
    object BaseProp {
        const val TLV_HEAD: Byte = 0x3f
        const val NEW_PROTOCOL_HEAD: Byte = 0x4f
        const val SERVICE_ADD_TRY_TIME = 3
    }


    /**
     * 蓝牙连接状态码
     */
    object BleConnectStateWhat {
        const val WHAT_CLIENT_CONN_SUCCESS = 0x001
        const val WHAT_CLIENT_CONN_FAILED = 0x002
        const val WHAT_CLIENT_CONN_DISCONNECT = 0x003
        const val WHAT_CONN_SERVER_SUCCESS = 0x004
        const val WHAT_CONN_SERVER_FAILED = 0x005
        const val WHAT_CONN_SERVER_DISCONNECT = 0x006
        const val WHAT_ADVERTISE_SUCCESS = 0x007
        const val WHAT_SCAN_RESULT = 0x008
        const val WHAT_DISCOVER_SERVICE = 0x009
    }

    /**
     * 蓝牙连接状态码
     */
    object BleMathConstant {
        const val TwoByteNum = 65536
        const val OneByteNum = 256
    }

    /**
     * 正在接受消息类型
     */
    object BleReceiveState {
        const val IDLE = -1
        const val TLV = 1
        const val NEW_PROTOCOL = 2
    }
}