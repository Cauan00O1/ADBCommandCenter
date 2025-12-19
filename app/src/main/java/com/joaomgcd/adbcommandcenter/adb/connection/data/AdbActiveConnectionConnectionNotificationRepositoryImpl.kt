package com.joaomgcd.adbcommandcenter.adb.connection.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.app.RemoteInput
import com.joaomgcd.adb.AdbService
import com.joaomgcd.adbcommandcenter.adb.connection.AdbConnectionService
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnectionNotificationRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.adb.notification.AdbNotificationHelper
import com.joaomgcd.adbcommandcenter.adb.notification.AdbNotificationHelper.Companion.KEY_PAIRING_CODE
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Parcelize
internal data class PairingInfo(val ip: String, val port: Int) : Parcelable

@Singleton
class AdbActiveConnectionConnectionNotificationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pairingRepository: AdbPairingRepository,
    private val notificationHelper: AdbNotificationHelper
) : AdbConnectionNotificationRepository {

    private val transientNotifications = MutableSharedFlow<Notification>()

    private companion object {
        const val ACTION_PAIRING_CODE_SUBMIT = "com.joaomgcd.adbcommandcenter.ACTION_PAIRING_CODE_SUBMIT"
        const val KEY_PAIRING_INFO = "KEY_PAIRING_INFO"
    }

    override val notificationFlow: Flow<Notification> = combine(
        pairingRepository.state,
        transientNotifications.onStart { emit(notificationHelper.buildIdleNotification()) }
    ) { state, transient ->
        // Prioritize transient (error) notifications when Idle, otherwise map current state
        if (state is PairingState.Idle && transient != notificationHelper.buildIdleNotification()) {
            transient
        } else {
            mapStateToNotification(state)
        }
    }

    override fun isPairingIntent(intent: Intent?): Boolean = intent?.action == ACTION_PAIRING_CODE_SUBMIT

    override suspend fun handlePairingIntent(intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val code = results.getCharSequence(KEY_PAIRING_CODE)?.toString() ?: return
        val info = intent.getParcelableExtra<PairingInfo?>(KEY_PAIRING_INFO) ?: return

        val pairingResult = pairingRepository.submitPairingCode(info.ip, info.port, code)
        if (pairingResult.isFailure) {
            val state = pairingRepository.state.value as? PairingState.PairingServiceFound ?: return
            val errorMsg = pairingResult.exceptionOrNull()?.message?.let { "$it\n" }
            transientNotifications.emit(state.toNotification(errorMsg))
        }
    }

    private fun mapStateToNotification(state: PairingState): Notification = when (state) {
        is PairingState.Scanning -> notificationHelper.buildScanningNotification()
        is PairingState.PairingServiceFound -> state.toNotification()
        PairingState.Idle -> notificationHelper.buildIdleNotification()
        is PairingState.Paired -> notificationHelper.buildPairedNotification()
    }

    private fun PairingState.PairingServiceFound.toNotification(prefix: String? = null) =
        notificationHelper.buildPairingNotification(
            service,
            createPairingPendingIntent(service),
            prefix ?: ""
        )

    private fun createPairingPendingIntent(service: AdbService): PendingIntent {
        val ip = service.host.hostAddress ?: ""
        val intent = Intent(context, AdbConnectionService::class.java).apply {
            action = ACTION_PAIRING_CODE_SUBMIT
            putExtra(KEY_PAIRING_INFO, PairingInfo(ip, service.port))
        }
        return PendingIntent.getService(
            context,
            service.port,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}