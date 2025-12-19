package com.joaomgcd.adbcommandcenter.adb.connection.domain

import com.joaomgcd.adb.AdbService
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import kotlinx.coroutines.flow.StateFlow

interface AdbPairingRepository {
    val state: StateFlow<PairingState>
    fun startMonitoring()
    fun stopMonitoring()
    suspend fun submitPairingCode(ip: String, port: Int, code: String): Result<Unit>
    val connectionServiceAfterPairing: StateFlow<AdbService?>
}