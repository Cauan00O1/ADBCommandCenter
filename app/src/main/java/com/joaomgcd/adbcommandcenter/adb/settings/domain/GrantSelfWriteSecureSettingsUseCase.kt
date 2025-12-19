package com.joaomgcd.adbcommandcenter.adb.settings.domain

import android.content.Context
import android.util.Log
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.permissions.app.domain.TogglePermissionUseCase
import com.joaomgcd.adbcommandcenter.common.domain.CheckNormalPermissionViaAndroidApiUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "GrantSelfWriteSecureSettings"

private fun String.debug() = Log.d(TAG, this)
private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)
class GrantSelfWriteSecureSettingsUseCase @Inject constructor(
    private val togglePermission: TogglePermissionUseCase,
    private val checkPermission: CheckNormalPermissionViaAndroidApiUseCase,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(ip: String, port: Int): Result<Unit> {
        val adbPermission = AdbPermission.SecureSettings
        val alreadyHasPermission = checkPermission(adbPermission.permissionName)
        if (alreadyHasPermission) {
            "${adbPermission.permissionName} already granted. No need to grant again".debug()
            return Result.success(Unit)
        }

        "Enabling permission ${adbPermission.permissionName}!".debug()
        return togglePermission(
            ip = ip,
            port = port,
            permission = adbPermission,
            appPackageName = context.packageName,
            isCurrentlyGranted = false
        )
            .onSuccess { "Permission granted successfully.".debug() }
            .onFailure { "Failed to grant permission.".error(it) }

    }
}