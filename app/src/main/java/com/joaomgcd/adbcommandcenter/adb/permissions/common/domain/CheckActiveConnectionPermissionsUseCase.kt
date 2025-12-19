package com.joaomgcd.adbcommandcenter.adb.permissions.common.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.connection.domain.DoWithActiveConnectionOperationUseCase
import javax.inject.Inject

class CheckActiveConnectionPermissionsUseCase @Inject constructor(
    private val doWithConnection: DoWithActiveConnectionOperationUseCase,
    private val checkPermissions: CheckPermissionsViaAdbUseCase
) {
    suspend operator fun invoke(
        appPackageName: String,
        vararg permissions: AdbPermission
    ) = doWithConnection { checkPermissions(ip, port, appPackageName, *permissions) }
}