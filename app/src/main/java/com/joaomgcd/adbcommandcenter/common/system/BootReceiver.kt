package com.joaomgcd.adbcommandcenter.common.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joaomgcd.adbcommandcenter.adb.common.domain.StartForegroundServiceUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var startForegroundService: StartForegroundServiceUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        startForegroundService("Boot")
    }
}