fun main() {
    val b: Byte = 0xA0.toByte()
    println(String.format("%02x", b))
    val arr = byteArrayOf(0x04, 0xA0.toByte(), 0x14)
    println(arr.joinToString("") { "%02x".format(it) })
}
