package com.joaomgcd.adb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "AdbConnectionManager"
private fun String.debug() = Log.d(TAG, this)


class AdbConnectionManager constructor(
    private val context: Context,
    private val keyAlias: String
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private var activeClient: AdbClient? = null
    private var cachedIp: String? = null
    private var cachedPort: Int? = null
    private var activeUsers = 0
    private var disconnectJob: Job? = null

    private val adbKey by lazy {
        scope.async(start = CoroutineStart.LAZY) {
            AdbKey.create(context, keyAlias)
        }
    }

    fun device(ip: String, port: Int): AdbDevice {
        return AdbDevice(this, ip, port)
    }

    internal suspend fun <T> runWithConnection(
        ip: String,
        port: Int,
        block: suspend (AdbClient) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        val client = try {
            getConnectedClient(ip, port)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        try {
            Result.success(block(client))
        } catch (e: Exception) {

            mutex.withLock {
                activeClient = null
                cachedIp = null
                cachedPort = null
            }
            client.close()
            Result.failure(e)
        } finally {
            releaseClient()
        }
    }

    private suspend fun getConnectedClient(ip: String, port: Int): AdbClient {
        mutex.withLock {
            disconnectJob?.cancel()
            activeUsers++

            val isSameTarget = cachedIp == ip && cachedPort == port
            val isConnected = activeClient?.isConnected == true

            if (isSameTarget && isConnected) {
                return activeClient!!
            }

            activeClient?.close()

            "Establishing new ADB connection to $ip:$port".debug()
            val newClient = AdbClient(ip, port, adbKey.await())
            try {
                newClient.connect()
                activeClient = newClient
                cachedIp = ip
                cachedPort = port
                return newClient
            } catch (connectException: Exception) {
                activeUsers--
                throw IllegalStateException("Could not connect to $ip:$port. ${connectException.message}", connectException)
            }
        }
    }

    private suspend fun releaseClient() {
        mutex.withLock {
            activeUsers--
            if (activeUsers <= 0) {
                activeUsers = 0
                scheduleIdleDisconnect()
            }
        }
    }

    private fun scheduleIdleDisconnect() {
        disconnectJob?.cancel()
        disconnectJob = scope.launch {
            delay(DISCONNECT_TIMEOUT_MS)
            mutex.withLock {
                if (activeUsers > 0) return@withLock

                try {
                    activeClient?.close()
                    activeClient = null
                    cachedIp = null
                    cachedPort = null
                    "ADB connection closed after ${DISCONNECT_TIMEOUT_MS}ms idle time".debug()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing idle ADB connection", e)
                }
            }
        }
    }

    suspend fun pairDevice(ip: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pairingClient = AdbPairingClient(ip, port, code, adbKey.await())
            val success = pairingClient.start()
            if (success) Result.success(Unit)
            else Result.failure(Exception("Pairing rejected by the device."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPaired(ip: String, port:Int): Boolean {
        return runWithConnection(ip, port) { true }.isSuccess
    }
    companion object {
        private const val DISCONNECT_TIMEOUT_MS = 60000L
    }
}