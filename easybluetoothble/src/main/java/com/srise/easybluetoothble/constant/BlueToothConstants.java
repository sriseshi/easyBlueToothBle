package com.srise.easybluetoothble.constant;

public class BlueToothConstants {
    /**
     * 蓝牙基本信息属性
     */
    public static class BaseProp {
        public static final byte TLV_HEAD = 0x3f;
        public static final byte NEW_PROTOCOL_HEAD = 0x4f;
        public static final int SERVICE_ADD_TRY_TIME = 3;
    }


    /**
     * 蓝牙连接状态码
     */
    public static class BleConnectStateWhat {
        public static final int WHAT_CLIENT_CONN_SUCCESS = 0x001;
        public static final int WHAT_CLIENT_CONN_FAILED = 0x002;
        public static final int WHAT_CLIENT_CONN_DISCONNECT = 0x003;

        public static final int WHAT_CONN_SERVER_SUCCESS = 0x004;
        public static final int WHAT_CONN_SERVER_FAILED = 0x005;
        public static final int WHAT_CONN_SERVER_DISCONNECT = 0x006;

        public static final int WHAT_ADVERTISE_SUCCESS = 0x007;
        public static final int WHAT_SCAN_RESULT = 0x008;
        public static final int WHAT_DISCOVER_SERVICE = 0x009;
    }

    /**
     * 蓝牙连接状态码
     */
    public static class BleMathConstant {
        public static final int TwoByteNum = 65536;
        public static final int OneByteNum = 256;
    }

    /**
     * 正在接受消息类型
     */
    public static class BleReceiveState {
        public static final int IDLE = -1;
        public static final int TLV = 1;
        public static final int NEW_PROTOCOL = 2;
    }
}
