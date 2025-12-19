package com.joaomgcd.adbcommandcenter.adb.common.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adb.AdbPermissionStatuses
import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.RemoteFile

interface AdbRepository {
    suspend fun isPaired(ip: String, port: Int): Boolean
    suspend fun pairDevice(ip: String, port: Int, code: String): Result<Unit>
    suspend fun runShellCommand(ip: String, port: Int, command: String): Result<String>
    suspend fun isGranted(ip: String, port: Int, appPackageName: String, vararg permissions: AdbPermission): Result<AdbPermissionStatuses>
    suspend fun grant(ip: String, port: Int, permission: AdbPermission, appPackageName: String): Result<Unit>
    suspend fun revoke(ip: String, port: Int, permission: AdbPermission, appPackageName: String): Result<Unit>
    suspend fun getDeviceApiLevel(ip: String, port: Int): Result<Int>
    suspend fun installApk(ip: String, port: Int, apkFilePath: String): Result<Unit>

    suspend fun listFiles(ip: String, port: Int, path: String): Result<List<RemoteFile>>
    suspend fun getRemoteFileSize(ip: String, port: Int, filePath: String): Result<Long>
}