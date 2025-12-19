package com.joaomgcd.adbcommandcenter.adb.connection.domain

import android.app.Notification
import android.content.Intent
import kotlinx.coroutines.flow.Flow


interface AdbConnectionNotificationRepository {
    val notificationFlow: Flow<Notification>

    fun isPairingIntent(intent: Intent?): Boolean

    suspend fun handlePairingIntent(intent: Intent)
}