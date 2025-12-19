package com.joaomgcd.adbcommandcenter.adb.connection.data

import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbActiveConnectionRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnection
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbConnectionRepositoryImpl @Inject constructor(
    pairingRepository: AdbPairingRepository
) : AdbActiveConnectionRepository {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override val connectionState: StateFlow<AdbConnection?> = pairingRepository.state
        .map { it.connection }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = pairingRepository.state.value.connection
        )

    private val PairingState.connection
        get() :AdbConnection? {
            if (this !is PairingState.Paired) return null
            val ip = connectionService.host.hostAddress ?: return null
            val port = connectionService.port
            return AdbConnection(ip, port)
        }
    override suspend fun waitForConnection(timeoutMs: Long): AdbConnection? {

        connectionState.value?.let { return it }


        return withTimeoutOrNull(timeoutMs) {
            connectionState.filterNotNull().first()
        }
    }
}