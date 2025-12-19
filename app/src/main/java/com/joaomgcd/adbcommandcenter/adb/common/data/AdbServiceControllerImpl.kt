package com.joaomgcd.adbcommandcenter.adb.common.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.joaomgcd.adbcommandcenter.adb.connection.AdbConnectionService
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbServiceController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdbServiceController"
private fun String.debug() = Log.d(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

@Singleton
class AdbServiceControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AdbServiceController {

    override fun start(source: String) {
        val intent = Intent(context, AdbConnectionService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
            "Successfully requested service start. Source: $source".debug()
        } catch (e: Exception) {
            "Failed to request service start. Source: $source".error(e)
        }
    }
}