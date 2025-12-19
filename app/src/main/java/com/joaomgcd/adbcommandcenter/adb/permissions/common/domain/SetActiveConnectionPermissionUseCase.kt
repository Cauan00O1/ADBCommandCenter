package com.joaomgcd.adbcommandcenter.adb.permissions.common.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.connection.domain.DoWithActiveConnectionOperationUseCase
import com.joaomgcd.adbcommandcenter.adb.permissions.app.domain.TogglePermissionUseCase
import javax.inject.Inject

class SetActiveConnectionPermissionUseCase @Inject constructor(
    private val doWithConnection: DoWithActiveConnectionOperationUseCase,
    private val togglePermission: TogglePermissionUseCase,
) {

    suspend operator fun invoke(
        permission: AdbPermission,
        appPackageName: String,
        grant: Boolean
    ): Result<Unit> = doWithConnection { togglePermission(ip, port, permission, appPackageName, isCurrentlyGranted = !grant) }
}