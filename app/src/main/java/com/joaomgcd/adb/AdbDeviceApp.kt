package com.joaomgcd.adb

import kotlinx.coroutines.delay

class AdbDeviceApp internal constructor(
    private val adbConnectionManager: AdbConnectionManager,
    private val ip: String,
    private val port: Int,
    private val packageName: String
) {

    suspend fun grant(permission: AdbPermission): Result<Unit> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            permission.toggle(client, packageName, grant = true)
        }
    }


    suspend fun revoke(permission: AdbPermission): Result<Unit> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            permission.toggle(client, packageName, grant = false)
        }
    }


    suspend fun isGranted(permission: AdbPermission): Result<Boolean> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            permission.isGranted(client, packageName)
        }
    }


    suspend fun grantAndCheck(permission: AdbPermission): Result<Boolean> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            permission.toggle(client, packageName, grant = true)
            delay(VERIFICATION_DELAY_MS)
            permission.isGranted(client, packageName)
        }
    }


    suspend fun revokeAndCheck(permission: AdbPermission): Result<Boolean> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            permission.toggle(client, packageName, grant = false)
            delay(VERIFICATION_DELAY_MS)
            !permission.isGranted(client, packageName)
        }
    }

    suspend fun isGranted(vararg permissions: AdbPermission): Result<AdbPermissionStatuses> {
        return adbConnectionManager.runWithConnection(ip, port) { client ->
            val map = permissions.associateWith { permission ->
                permission.isGranted(client, packageName)
            }
            AdbPermissionStatuses(map)
        }
    }

    private suspend fun AdbPermission.toggle(client: AdbClient, packageName: String, grant: Boolean) {
        singlePermissions.forEach {
            client.shellCommand(it.getCommandToggle(packageName, grant))
        }
    }



    private suspend fun AdbPermission.isGranted(client: AdbClient, packageName: String): Boolean {
        return singlePermissions.all {
            val output = client.shellCommand(it.getCommandIsGranted(packageName))
            it.checkIfGranted(output)
        }
    }

    companion object {
        private const val VERIFICATION_DELAY_MS = 500L
    }
}