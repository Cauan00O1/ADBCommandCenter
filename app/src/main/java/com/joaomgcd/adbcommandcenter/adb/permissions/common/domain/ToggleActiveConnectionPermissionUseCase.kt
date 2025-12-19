package com.joaomgcd.adbcommandcenter.adb.permissions.common.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.common.domain.ToggleState
import javax.inject.Inject

class ToggleActiveConnectionPermissionUseCase @Inject constructor(
    private val checkPermissions: CheckActiveConnectionPermissionsUseCase,
    private val setPermission: SetActiveConnectionPermissionUseCase,
) {

    suspend operator fun invoke(appPackageName: String, permissionString: String, state: ToggleState): Result<Unit> {
        val adbPermission = AdbPermission.fromString(permissionString)
            ?: return Result.failure(IllegalStateException("Invalid permission $permissionString"))
        val newState: Boolean = when (state) {
            ToggleState.Enable -> true
            ToggleState.Disable -> false
            ToggleState.Toggle -> {
                val checkPermissionsResult = checkPermissions(
                    appPackageName,
                    adbPermission
                )
                if (checkPermissionsResult.isFailure) return checkPermissionsResult.map { false }
                val currentState = checkPermissionsResult.getOrThrow()[adbPermission] ?: return Result.failure(IllegalStateException("Couldn't get status of permission to toggle $permissionString"))
                !currentState
            }
        }
        val resultToggle = setPermission(adbPermission, appPackageName, newState)
        return resultToggle.map { true }
    }
}