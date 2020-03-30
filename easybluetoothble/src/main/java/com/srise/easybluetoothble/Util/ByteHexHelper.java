package com.srise.easybluetoothble.Util;

import java.nio.charset.StandardCharsets;

public class ByteHexHelper {
    /**
     * 十进制数字转换十六进制数组
     *
     * @param dec
     * @return
     */
    public static byte[] Dec2Hex(int dec, String... prefix) {
        if (prefix != null && prefix.length > 0) {
            return hexString2Bytes(prefix[0] + Integer.toHexString(dec));
        } else {
            return hexString2Bytes(Integer.toHexString(dec));
        }
    }

    /**
     * 十六进制数组转换十进制数字
     *
     * @param hex
     * @return
     */
    public static int Hex2DEC(byte[] hex) {
        return Integer.parseInt(bytes2HexString(hex), 16);
    }

    /**
     * 字符串转换UTF-8的十六进制数组
     *
     * @param string
     * @return
     */
    public static byte[] String2Hex(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 十六进制数组转换UTF-8字符串
     *
     * @param hex
     * @return
     */
    public static String Hex2String(byte[] hex) {
        return new String(hex, StandardCharsets.UTF_8);
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 16进制字符串转字节数组
     *
     * @param hex
     * @return
     */
    public static byte[] hexString2Bytes(String hex) {

        if ((hex == null) || (hex.equals(""))) {
            return null;
        } else if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }

        hex = hex.toUpperCase();
        int len = hex.length() / 2;
        byte[] b = new byte[len];
        char[] hc = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int p = 2 * i;
            b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
        }
        return b;
    }

    /**
     * 字节数组转16进制字符串
     *
     * @param b
     * @return
     */
    public static String bytes2HexString(byte[] b) {
        String r = "";

        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }

    public static byte[] byteMergerAll(byte[]... values) {
        int length_byte = 0;
        for (int i = 0; i < values.length; i++) {
            length_byte += values[i].length;
        }
        byte[] all_byte = new byte[length_byte];
        int countLength = 0;
        for (int i = 0; i < values.length; i++) {
            byte[] b = values[i];
            System.arraycopy(b, 0, all_byte, countLength, b.length);
            countLength += b.length;
        }
        return all_byte;
    }
}
