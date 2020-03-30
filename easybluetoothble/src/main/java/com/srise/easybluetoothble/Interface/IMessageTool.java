package com.srise.easybluetoothble.Interface;


public interface IMessageTool {
    byte[] encodeData(int newProtocolType, int requestId, int data);
    byte[] encodeData(int newProtocolType, int requestId, String data);
    byte[] encodeData(int newProtocolType, int requestId, byte[] data);
}
