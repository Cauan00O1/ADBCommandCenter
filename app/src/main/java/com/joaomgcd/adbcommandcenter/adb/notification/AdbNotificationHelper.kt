package com.joaomgcd.adbcommandcenter.adb.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.joaomgcd.adb.AdbService
import com.joaomgcd.adbcommandcenter.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat
) {

    companion object {
        const val FOREGROUND_SERVICE_CHANNEL_ID = "AdbDiscovery"
        const val PAIRING_REQUEST_CHANNEL_ID = "AdbPairingRequest"
        const val KEY_PAIRING_CODE = "KEY_PAIRING_CODE"
    }

    init {
        createChannels()
    }

    fun buildScanningNotification(): Notification {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle("Scanning for ADB Services")
            .setContentText("Tap to open Developer Settings. Enable 'Wireless Debugging'.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Scanning for ADB Services.\n\n" +
                            "1. Tap here to open Developer Settings.\n" +
                            "2. Enable 'Wireless Debugging'.\n" +
                            "3. Select 'Pair device with pairing code'."
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground_vector)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildIdleNotification(): Notification {
        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle("ADB Service Active")
            .setContentText("Managing ADB Connection")
            .setSmallIcon(R.drawable.ic_launcher_foreground_vector)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildPairingNotification(service: AdbService, replyPendingIntent: PendingIntent, prefix: String = ""): Notification {
        val remoteInput = RemoteInput.Builder(KEY_PAIRING_CODE)
            .setLabel("Pairing Code")
            .build()

        val action = NotificationCompat.Action.Builder(
            0, "Enter Code", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val text = "${prefix}Tap 'Enter Code' to enter pairing code for ${service.host.hostAddress}"
        val notification = NotificationCompat.Builder(context, PAIRING_REQUEST_CHANNEL_ID)
            .setContentTitle("ADB Device Found")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground_vector)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .addAction(action)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        return notification
    }

    fun buildPairedNotification(): Notification {
        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle("ADB Service Active")
            .setContentText("ADB Paired!")
            .setSmallIcon(R.drawable.ic_launcher_foreground_vector)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }


    private fun createChannels() {
        val foregroundChannel = NotificationChannel(
            FOREGROUND_SERVICE_CHANNEL_ID,
            "ADB Discovery Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val pairingChannel = NotificationChannel(
            PAIRING_REQUEST_CHANNEL_ID,
            "ADB Pairing Request",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows a notification to enter the ADB pairing code"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
        }

        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(pairingChannel)
    }
}