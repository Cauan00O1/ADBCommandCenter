package com.joaomgcd.adbcommandcenter.adb.lifecycle

import android.util.Log
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.adb.settings.domain.GrantSelfWriteSecureSettingsUseCase
import com.joaomgcd.adbcommandcenter.common.di.ApplicationScope
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdbLifecycleManager"

private fun String.debug() = Log.d(TAG, this)
private fun String.info() = Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

@Singleton
class AdbLifecycleManager @Inject constructor(
    private val adbPairingRepository: AdbPairingRepository,
//    private val wirelessDebuggingEnforcer: WirelessDebuggingEnforcer,
    private val grantSelfWriteSecureSettings: GrantSelfWriteSecureSettingsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope
) {

    private var lifecycleJob: Job? = null

    fun start() {
        if (lifecycleJob?.isActive == true) return
        "Starting ADB Lifecycle Management".debug()

        lifecycleJob = scope.launch {
            adbPairingRepository.state
                .collectLatest { state ->
                    handleState(state)
                }
        }
    }

    fun stop() {
        "Stopping ADB Lifecycle Management".debug()
        lifecycleJob?.cancel()
        lifecycleJob = null

//        wirelessDebuggingEnforcer.enableMonitoring(false)
    }

    private suspend fun handleState(state: PairingState) {
        when (state) {
            is PairingState.Paired -> {
//                wirelessDebuggingEnforcer.enableMonitoring(false)

                try {
                    val ip = state.connectionService.host.hostAddress
                    if (ip != null) {
                        "Paired. Grant WRITE_SECURE_SETTINGS permission if needed...".debug()
                        grantSelfWriteSecureSettings(ip, state.connectionService.port)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during permission grant phase", e)
                }
            }

            else -> {

//                wirelessDebuggingEnforcer.enableMonitoring(true)
            }
        }
    }
}