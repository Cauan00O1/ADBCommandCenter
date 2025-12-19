package com.joaomgcd.adb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

private fun String.debug() = {}//Log.d(TAG, this)
private fun String.info() = {}// Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

fun commandToString(cmd: Int): String {
    return when (cmd) {
        AdbProtocol.A_SYNC -> "SYNC"
        AdbProtocol.A_CNXN -> "CNXN"
        AdbProtocol.A_AUTH -> "AUTH"
        AdbProtocol.A_OPEN -> "OPEN"
        AdbProtocol.A_OKAY -> "OKAY"
        AdbProtocol.A_CLSE -> "CLSE"
        AdbProtocol.A_WRTE -> "WRTE"
        AdbProtocol.A_STLS -> "STLS"
        else -> "0x" + cmd.toUInt().toString(16)
    }
}

class AdbClient(private val host: String, private val port: Int, private val key: AdbKey) : Closeable {
    private lateinit var socket: Socket
    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream

    val isConnected: Boolean
        get() = ::socket.isInitialized && !socket.isClosed && socket.isConnected

    suspend fun connect() = withContext(Dispatchers.IO) {
        "Connecting to $host:$port...".debug()
        socket = Socket(host, port).apply {
            tcpNoDelay = true
            soTimeout = 10000
        }
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())

        write(AdbProtocol.A_CNXN, AdbProtocol.A_VERSION, AdbProtocol.A_MAXDATA, "host::")

        var msg = read()
        if (msg.command == AdbProtocol.A_STLS) {
            write(AdbProtocol.A_STLS, AdbProtocol.A_STLS_VERSION, 0, null)
            val sslSocket = key.getSslContext().socketFactory.createSocket(socket, host, port, true) as SSLSocket
            "Starting SSL Handshake for $host:$port...".debug()
            sslSocket.startHandshake()
            "SSL Handshake Complete for $host:$port.".debug()
            input = DataInputStream(sslSocket.inputStream)
            output = DataOutputStream(sslSocket.outputStream)
            msg = read()
        }
        if (msg.command == AdbProtocol.A_AUTH) {
            write(AdbProtocol.A_AUTH, AdbProtocol.ADB_AUTH_SIGNATURE, 0, key.sign(msg.data))
            msg = read()
            if (msg.command != AdbProtocol.A_CNXN) {
                write(AdbProtocol.A_AUTH, AdbProtocol.ADB_AUTH_RSAPUBLICKEY, 0, key.getAdbPublicKey())
                msg = read()
            }
        }
        if (msg.command != AdbProtocol.A_CNXN) throw Exception("Connection failed for $host:$port: Not CNXN. Msg: ${commandToString(msg.command)}")
        "Connection successful for $host:$port.".debug()
    }

    suspend fun shellCommand(command: String): String = withContext(Dispatchers.IO) {
        val localId = (Math.random() * Int.MAX_VALUE).toInt() + 1
        var remoteId = 0
        "--- Starting shell command '$command' with localId=$localId ---".debug()

        write(AdbProtocol.A_OPEN, localId, 0, "shell:$command\u0000".toByteArray())

        val sb = StringBuilder()

        while (true) {
            val msg = read()

            if (remoteId == 0) {
                if (msg.command == AdbProtocol.A_OKAY && msg.arg1 == localId) {
                    remoteId = msg.arg0
                    "Stream opened. localId=$localId, remoteId=$remoteId".debug()
                    continue
                } else {
                    "Ignoring stray message while waiting for initial OKAY: ${commandToString(msg.command)}".warn()
                    continue
                }
            }

            if (msg.arg1 != localId || msg.arg0 != remoteId) {
                "Ignoring stray message for another stream: ${commandToString(msg.command)}(arg0=${msg.arg0}, arg1=${msg.arg1})".warn()
                continue
            }

            when (msg.command) {
                AdbProtocol.A_WRTE -> {
                    if (msg.data != null) sb.append(String(msg.data, Charsets.UTF_8))
                    write(AdbProtocol.A_OKAY, localId, remoteId, null)
                }
                AdbProtocol.A_CLSE -> {
                    "--- Shell command finished for localId=$localId ---".debug()
                    return@withContext sb.toString()
                }
                AdbProtocol.A_OKAY -> {
                    continue
                }
                else -> {
                    close()
                    throw Exception("Unexpected command ${commandToString(msg.command)} during shell execution.")
                }
            }
        }

        return@withContext sb.toString()
    }

    private fun logMessage(direction: String, msg: AdbMessage) {
//        val cmdStr = commandToString(msg.command)
//        val dataStr = msg.data?.let { data ->
//            val s = String(data, Charsets.UTF_8).replace("\u0000", "").replace("\n", "")
//            if (s.length > 100) s.substring(0, 100) + "..." else s
//        } ?: "no data"
//        "$direction: $cmdStr(arg0=${msg.arg0}, arg1=${msg.arg1}) - data[${msg.data?.size ?: 0}]='$dataStr'".debug()
    }

    private fun write(cmd: Int, arg0: Int, arg1: Int, data: String) = write(cmd, arg0, arg1, "$data\u0000".toByteArray())

    private fun write(cmd: Int, arg0: Int, arg1: Int, data: ByteArray?) {
        val message = AdbMessage(cmd, arg0, arg1, data)
        logMessage("SEND", message)
        try {
            output.write(message.toByteArray())
            output.flush()
        } catch (e: Exception) {
            "Write failed!".error(e)
            close()
            throw e
        }
    }

    private fun read(): AdbMessage {
        val buf = ByteArray(24)
        input.readFully(buf)
        val b = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = b.int; val arg0 = b.int; val arg1 = b.int; val len = b.int; val crc = b.int; val magic = b.int
        val data = if (len > 0) ByteArray(len).also { input.readFully(it) } else null
        val message = AdbMessage(cmd, arg0, arg1, len, crc, magic, data)
        logMessage("RECV", message)
        return message
    }

    override fun close() {
        "Closing connection.".debug()
        try { socket.close() } catch (e: Exception) {}
    }
}