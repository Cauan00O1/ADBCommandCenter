package com.joaomgcd.adb

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AdbProtocol {
    const val A_SYNC = 0x434e5953
    const val A_CNXN = 0x4e584e43
    const val A_AUTH = 0x48545541
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257
    const val A_STLS = 0x534C5453
    const val A_VERSION = 0x01000000
    const val A_MAXDATA = 1024 * 1024
    const val A_STLS_VERSION = 0x01000000
    const val ADB_AUTH_TOKEN = 1
    const val ADB_AUTH_SIGNATURE = 2
    const val ADB_AUTH_RSAPUBLICKEY = 3
}

class AdbMessage(
    val command: Int, val arg0: Int, val arg1: Int,
    val data_length: Int, val data_crc32: Int, val magic: Int, val data: ByteArray?
) {
    constructor(command: Int, arg0: Int, arg1: Int, data: ByteArray?) : this(
        command, arg0, arg1, data?.size ?: 0, crc32(data), (command.toLong() xor 0xFFFFFFFF).toInt(), data
    )

    fun toByteArray(): ByteArray {
        val length = 24 + (data?.size ?: 0)
        return ByteBuffer.allocate(length).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(command); putInt(arg0); putInt(arg1); putInt(data_length); putInt(data_crc32); putInt(magic)
            if (data != null) put(data)
        }.array()
    }

    companion object {
        fun crc32(data: ByteArray?): Int {
            if (data == null) return 0
            var res = 0
            for (b in data) {
                res += (b.toInt() and 0xFF)
            }
            return res
        }
    }
}