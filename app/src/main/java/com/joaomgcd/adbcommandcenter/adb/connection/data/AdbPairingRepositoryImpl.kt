package com.joaomgcd.adbcommandcenter.discovery

import android.util.Log
import com.joaomgcd.adb.AdbDiscoveryManager
import com.joaomgcd.adb.AdbService
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class PairingState {
    object Idle : PairingState()
    object Scanning : PairingState()
    data class PairingServiceFound(val service: AdbService) : PairingState()
    data class Paired(val connectionService: AdbService) : PairingState()
}

private const val TAG = "AdbPairingRepository"
private fun String.debug() = Log.d(TAG, this)
private fun String.info() = Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)
@Singleton
class AdbPairingRepositoryImpl @Inject constructor(
    private val adbDiscoveryManager: AdbDiscoveryManager,
    private val adbRepository: AdbRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope
) : AdbPairingRepository {
    private val _verifiedConnectionService = MutableStateFlow<AdbService?>(null)
    override val connectionServiceAfterPairing: StateFlow<AdbService?> get() = _verifiedConnectionService.asStateFlow()

    override val state: StateFlow<PairingState> = combine(
        adbDiscoveryManager.isSearching,
        adbDiscoveryManager.discoveredPairingService,
        _verifiedConnectionService
    ) { isSearching, pairingService, connectionService ->
        "State update: isSearching: $isSearching, pairingService: $pairingService, connectionService: $connectionService".debug()
        when {
            connectionService != null -> PairingState.Paired(connectionService)
            pairingService != null -> PairingState.PairingServiceFound(pairingService)
            isSearching -> PairingState.Scanning
            else -> PairingState.Idle
        }
    }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), PairingState.Idle)

    private var checkPairedJob: Job? = null
    private var disconnectJob: Job? = null

    private var latestDiscoveredService: AdbService? = null
    override fun startMonitoring() {
        _verifiedConnectionService.value = null
        adbDiscoveryManager.startDiscovery()

        checkPairedJob?.cancel()
        checkPairedJob = externalScope.launch {

            adbDiscoveryManager.connectServiceUpdates
                .collect { service ->
                    "discoveredConnectService update: $service".debug()
                    latestDiscoveredService = service

                    if (service == null) {
                        "Service lost signal received. Waiting 1s before disconnect...".debug()
                        disconnectJob?.cancel()
                        disconnectJob = launch {
                            delay(1000)
                            "Service lost confirmed. Disconnecting.".debug()
                            _verifiedConnectionService.value = null
                        }
                        return@collect
                    }
                    disconnectJob?.cancel()
                    val hostAddress = service.host.hostAddress
                    if (hostAddress == null) {
                        "setting verified service to null cause host address is null".debug()

                        _verifiedConnectionService.value = null
                        return@collect
                    }

                    if (adbRepository.isPaired(hostAddress, service.port)) {
                        "It's paired! Setting verified service to $service".debug()
                        _verifiedConnectionService.value = service
                    } else {
                        "$service is not paired!".debug()
                        val currentVerified = _verifiedConnectionService.value
                        val isSameHost = currentVerified?.host?.hostAddress == hostAddress
                        if (!isSameHost) {
                            "$service is not same host as $currentVerified, so setting verified to null".debug()
                            _verifiedConnectionService.value = null
                        } else {
                            "Ignoring $service because we are already verified on same host: $currentVerified".debug()
                        }
                    }
                }
        }
    }

    override suspend fun submitPairingCode(ip: String, port: Int, code: String): Result<Unit> {
        val result = adbRepository.pairDevice(ip, port, code)
        if (!result.isSuccess) return result

        latestDiscoveredService?.let {
            if (it.host.hostAddress != ip) return@let

            _verifiedConnectionService.value = it
        }
        return result
    }

    override fun stopMonitoring() {
        checkPairedJob?.cancel()
        disconnectJob?.cancel()
        _verifiedConnectionService.value = null
        adbDiscoveryManager.stopDiscovery()
    }

}