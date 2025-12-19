package com.joaomgcd.adbcommandcenter.adb.settings.domain

import android.util.Log
import com.joaomgcd.adbcommandcenter.common.di.ApplicationScope
import com.joaomgcd.adbcommandcenter.connectivity.domain.WifiStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WirelessDebuggingMonitor"
private fun String.debug() = Log.d(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

@Singleton
class WirelessDebuggingEnforcer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wifiStateRepository: WifiStateRepository,
    private val ensureWirelessDebuggingEnabled: EnsureWirelessDebuggingEnabledUseCase,
    @param:ApplicationScope private val scope: CoroutineScope
) {

    private var monitoringJob: Job? = null

    fun enableMonitoring(enable: Boolean) {
        if (enable) {
            start()
        } else {
            stop()
        }
    }

    private fun start() {
        if (monitoringJob?.isActive == true) return

        "Starting Wireless Debugging monitoring...".debug()
        monitoringJob = combine(
            wifiStateRepository.wifiConnectedFlow,
            settingsRepository.observeInt(SettingsNamespace.GLOBAL, "adb_wifi_enabled", 0)
        ) { isWifiConnected, adbEnabled ->
            isWifiConnected to adbEnabled
        }
            .onEach { (isWifiConnected, adbEnabled) ->
                if (isWifiConnected && adbEnabled == 0) {
                    "Wifi is connected but ADB Wifi is disabled. Re-enabling...".debug()

                    val result = ensureWirelessDebuggingEnabled()


                    if (!result.isSuccess) {
                        "Failed to enable ADB Wifi. Missing WRITE_SECURE_SETTINGS?".error(result.exceptionOrNull())
                    }
                }
            }
            .launchIn(scope)
    }

    private fun stop() {
        if (monitoringJob?.isActive == true) {
            "Stopping Wireless Debugging monitoring.".debug()
            monitoringJob?.cancel()
            monitoringJob = null
        }
    }
}