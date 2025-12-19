package com.joaomgcd.adbcommandcenter.adb.permissions.common.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import javax.inject.Inject

class CheckPermissionsViaAdbUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        appPackageName: String,
        vararg permissions: AdbPermission
    ) = adbRepository.isGranted(ip, port, appPackageName, *permissions)
}