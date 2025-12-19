package com.joaomgcd.adbcommandcenter.adb.connection.domain

import kotlinx.coroutines.flow.StateFlow


interface AdbActiveConnectionRepository {

    val connectionState: StateFlow<AdbConnection?>

    suspend fun waitForConnection(timeoutMs: Long = 5000): AdbConnection?
}