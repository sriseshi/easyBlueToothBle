package c.example.dell.ktlionble.inter

interface IMessageTool {
    fun encodeData(newProtocolType: Int, requestId: Int, data: Int): ByteArray?
    fun encodeData(newProtocolType: Int, requestId: Int, data: String?): ByteArray?
    fun encodeData(newProtocolType: Int, requestId: Int, data: ByteArray?): ByteArray?
}