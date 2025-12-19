package com.joaomgcd.adbcommandcenter

import android.app.Application
import com.joaomgcd.adbcommandcenter.adb.common.domain.StartForegroundServiceUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "ApplicationAdbCommandCenter"

@HiltAndroidApp
class ApplicationAdbCommandCenter : Application() {
    @Inject
    lateinit var startForegroundService: StartForegroundServiceUseCase
    override fun onCreate() {
        super.onCreate()
        startForegroundService("App Creation")
    }

}