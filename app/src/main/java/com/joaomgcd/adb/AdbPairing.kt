
package com.joaomgcd.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

class PairingContext private constructor(private val nativePtr: Long) {
    val msg: ByteArray = nativeMsg(nativePtr)
    fun initCipher(theirMsg: ByteArray) = nativeInitCipher(nativePtr, theirMsg)
    fun encrypt(input: ByteArray) = nativeEncrypt(nativePtr, input)
    fun decrypt(input: ByteArray) = nativeDecrypt(nativePtr, input)
    fun destroy() = nativeDestroy(nativePtr)

    private external fun nativeMsg(ptr: Long): ByteArray
    private external fun nativeInitCipher(ptr: Long, msg: ByteArray): Boolean
    private external fun nativeEncrypt(ptr: Long, data: ByteArray): ByteArray?
    private external fun nativeDecrypt(ptr: Long, data: ByteArray): ByteArray?
    private external fun nativeDestroy(ptr: Long)

    companion object {
        init { System.loadLibrary("adb") }
        @JvmStatic private external fun nativeConstructor(isClient: Boolean, password: ByteArray): Long

        fun create(password: ByteArray): PairingContext? {
            val ptr = nativeConstructor(true, password)
            return if (ptr != 0L) PairingContext(ptr) else null
        }
    }
}

class AdbPairingClient(private val host: String, private val port: Int, private val pairCode: String, private val key: AdbKey) {
    suspend fun start() : Boolean= withContext(Dispatchers.IO) {
        val socket = Socket(host, port).apply { tcpNoDelay = true }

        val sslContext = key.getSslContext()
        val sslSocket = sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
        sslSocket.startHandshake()

        val input = DataInputStream(sslSocket.inputStream)
        val output = DataOutputStream(sslSocket.outputStream)

        val keyMaterial = Conscrypt.exportKeyingMaterial(sslSocket, "adb-label\u0000", null, 64)
            ?: throw Exception("Failed to export keying material")

        val combined = ByteArray(pairCode.length + keyMaterial.size)
        System.arraycopy(pairCode.toByteArray(), 0, combined, 0, pairCode.length)
        System.arraycopy(keyMaterial, 0, combined, pairCode.length, keyMaterial.size)

        val ctx = PairingContext.create(combined) ?: return@withContext false

        try {
            val myHeader = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN).put(1).put(0).putInt(ctx.msg.size).array()
            output.write(myHeader); output.write(ctx.msg)

            val theirHeaderBytes = ByteArray(6).also { input.readFully(it) }
            val theirHeader = ByteBuffer.wrap(theirHeaderBytes).order(ByteOrder.BIG_ENDIAN)
            theirHeader.get();
            if (theirHeader.get().toInt() != 0) return@withContext false
            val theirPayload = ByteArray(theirHeader.int).also { input.readFully(it) }

            if (!ctx.initCipher(theirPayload)) return@withContext false

            val peerInfoBuffer = ByteBuffer.allocate(8192).order(ByteOrder.BIG_ENDIAN)
            peerInfoBuffer.put(0)
            peerInfoBuffer.put(key.getAdbPublicKey())

            val encryptedPeerInfo = ctx.encrypt(peerInfoBuffer.array()) ?: return@withContext false

            val myInfoHeader = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN).put(1).put(1).putInt(encryptedPeerInfo.size).array()
            output.write(myInfoHeader); output.write(encryptedPeerInfo)

            val theirInfoHeadBytes = ByteArray(6).also { input.readFully(it) }
            val theirInfoHead = ByteBuffer.wrap(theirInfoHeadBytes).order(ByteOrder.BIG_ENDIAN)
            theirInfoHead.get()
            if (theirInfoHead.get().toInt() != 1) return@withContext false
            val theirInfoPayload = ByteArray(theirInfoHead.int).also { input.readFully(it) }

            val decrypted = ctx.decrypt(theirInfoPayload)

            return@withContext decrypted != null
        } finally {
            ctx.destroy()
            socket.close()
        }
    }
}