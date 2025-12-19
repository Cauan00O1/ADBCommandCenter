package com.joaomgcd.adb


class AdbDevice internal constructor(
    private val adbConnectionManager: AdbConnectionManager,
    private val ip: String,
    private val port: Int
) {

    fun forApp(packageName: String): AdbDeviceApp {
        return AdbDeviceApp(adbConnectionManager, ip, port, packageName)
    }


    suspend fun executeCommand(command: String): Result<String> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            client.shellCommand(command)
        }
    }


    suspend fun pair(code: String): Result<Unit> {
        return adbConnectionManager.pairDevice(ip, port, code)
    }


    suspend fun isPaired(): Boolean {
        return adbConnectionManager.isPaired(ip, port)
    }
}


