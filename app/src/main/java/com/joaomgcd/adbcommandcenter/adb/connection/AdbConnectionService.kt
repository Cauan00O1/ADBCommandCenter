package com.joaomgcd.adbcommandcenter.adb.connection


import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.joaomgcd.adbcommandcenter.adb.common.domain.RunShellCommandUseCase
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnectionNotificationRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.adb.lifecycle.AdbLifecycleManager
import com.joaomgcd.adbcommandcenter.adb.notification.AdbNotificationHelper
import com.joaomgcd.adbcommandcenter.common.di.ServiceCoroutineScope
import com.joaomgcd.adbcommandcenter.common.domain.WaitForPermissionToBeGrantedUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AdbDiscoveryService"

private fun String.debug() = Log.d(TAG, this)
private fun String.info() = Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)


@AndroidEntryPoint
class AdbConnectionService : Service() {

    @Inject
    lateinit var pairingManager: AdbPairingRepository

    @Inject
    @ServiceCoroutineScope
    lateinit var serviceScope: CoroutineScope

    @Inject
    lateinit var runShell: RunShellCommandUseCase

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var notificationHelper: AdbNotificationHelper

    @Inject
    lateinit var adbLifecycleManager: AdbLifecycleManager

    @Inject
    lateinit var waitForPermission: WaitForPermissionToBeGrantedUseCase
    @Inject
    lateinit var adbConnectionNotificationRepository: AdbConnectionNotificationRepository

    private var monitoringJob: Job? = null

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 101
    }

    private fun Notification.notify() {
        startForeground(FOREGROUND_NOTIFICATION_ID, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationHelper.buildIdleNotification())

        if (adbConnectionNotificationRepository.isPairingIntent(intent)) {
            serviceScope.launch { adbConnectionNotificationRepository.handlePairingIntent(intent!!) }
            return START_NOT_STICKY
        }

        serviceScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                waitForPermission(Manifest.permission.POST_NOTIFICATIONS)
            }
            "Notification permission confirmed. Starting ADB monitoring.".debug()
            startAdbLogic()
        }

        return START_STICKY
    }

    private fun startAdbLogic() {
        if (monitoringJob?.isActive == true) return

        adbLifecycleManager.start()
        notifyPairingManagerState()
        pairingManager.startMonitoring()
    }

    @SuppressLint("MissingPermission")
    private fun notifyPairingManagerState() {
        monitoringJob = adbConnectionNotificationRepository.notificationFlow
            .onEach { it.notify() }
            .launchIn(serviceScope)
    }




    override fun onDestroy() {
        super.onDestroy()
        pairingManager.stopMonitoring()
        adbLifecycleManager.stop()
        serviceScope.cancel()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}