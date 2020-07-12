package c.example.dell.ktlionble.util

import java.nio.charset.StandardCharsets

class ByteHexHelper {
     companion object{
         fun Dec2Hex(dec: Int, vararg prefix: String): ByteArray? {
             return if (prefix.isNotEmpty()) {
                 ByteHexHelper.hexString2Bytes(prefix[0] + Integer.toHexString(dec))
             } else {
                 ByteHexHelper.hexString2Bytes(Integer.toHexString(dec))
             }
         }

         fun Hex2DEC(hex: ByteArray?): Int {
             return ByteHexHelper.bytes2HexString(hex)!!.toInt(16)
         }

         fun String2Hex(string: String): ByteArray? {
             return string.toByteArray(StandardCharsets.UTF_8)
         }

         fun Hex2String(hex: ByteArray?): String? {
             return String(hex!!, StandardCharsets.UTF_8)
         }

         private fun charToByte(c: Char): Byte {
             return "0123456789ABCDEF".indexOf(c).toByte()
         }

         fun hexString2Bytes(hex: String?): ByteArray? {
             var hex = hex

             if (hex == null || hex == "") {
                 return null
             } else if (hex.length % 2 != 0) {
                 hex = "0$hex"
             }

             hex = hex.toUpperCase()
             val len = hex.length / 2
             val b = ByteArray(len)
             val hc = hex.toCharArray()

             for (i in 0 until len) {
                 val p = 2 * i
                 b[i] = (charToByte(hc[p]).toInt() shl 4 or charToByte(hc[p+1]).toInt()) as Byte
             }

             return b
         }


         fun bytes2HexString(b: ByteArray?): String? {
             var r = ""
             for (i in b!!.indices) {
                 var hex = Integer.toHexString(b[i].toInt() and 0xFF)

                 if (hex.length == 1) {
                     hex = "0$hex"
                 }

                 r += hex.toUpperCase()
             }
             return r
         }

         fun byteMergerAll(vararg values: ByteArray): ByteArray? {
             var lengthByte = 0

             for (i in values.indices) {
                 lengthByte += values[i].size
             }

             val allByte = ByteArray(lengthByte)
             var countLength = 0

             for (i in values.indices) {
                 val b = values[i]
                 System.arraycopy(b, 0, allByte, countLength, b.size)
                 countLength += b.size
             }

             return allByte
         }

     }
 }