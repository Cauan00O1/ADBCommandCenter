package com.joaomgcd.adbcommandcenter.adb.common.domain

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "StartForegroundService"
private fun String.debug() = Log.d(TAG, this)
private fun String.info() = Log.i(TAG, this)
private fun String.warn() = Log.w(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)

@Singleton
class StartForegroundServiceUseCase @Inject constructor(
    private val adbServiceController: AdbServiceController
) {
    operator fun invoke(source: String) = adbServiceController.start(source)
}