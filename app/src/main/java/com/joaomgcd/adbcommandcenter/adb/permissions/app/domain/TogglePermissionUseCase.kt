package com.joaomgcd.adbcommandcenter.adb.permissions.app.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import javax.inject.Inject

class TogglePermissionUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {

    suspend operator fun invoke(
        ip: String,
        port: Int,
        permission: AdbPermission,
        appPackageName: String,
        isCurrentlyGranted: Boolean
    ): Result<Unit> {
        return if (isCurrentlyGranted) {
            adbRepository.revoke(ip, port, permission, appPackageName)
        } else {
            adbRepository.grant(ip, port, permission, appPackageName)
        }
    }
}