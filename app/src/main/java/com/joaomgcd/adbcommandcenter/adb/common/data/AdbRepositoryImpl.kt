package com.joaomgcd.adbcommandcenter.adb.common.data

import android.util.Log
import com.joaomgcd.adb.AdbConnectionManager
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adb.AdbPermissionStatuses
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.RemoteFile
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdbRepository"
private fun String.debug() = Log.d(TAG, this)

@Singleton
class AdbRepositoryImpl @Inject constructor(
    private val adbConnectionManager: AdbConnectionManager
) : AdbRepository {

    override suspend fun isPaired(ip: String, port: Int) = adbConnectionManager.isPaired(ip, port)
    override suspend fun pairDevice(ip: String, port: Int, code: String) = adbConnectionManager.pairDevice(ip, port, code)
    override suspend fun runShellCommand(ip: String, port: Int, command: String) = adbConnectionManager.runWithConnection(ip, port) {
        "Running shell command \"$command\" on $ip:$port".debug()
        it.shellCommand(command)
    }

    override suspend fun isGranted(
        ip: String,
        port: Int,
        appPackageName: String,
        vararg permissions: AdbPermission
    ): Result<AdbPermissionStatuses> {
        val deviceApp = adbConnectionManager.device(ip, port).forApp(appPackageName)
        return deviceApp.isGranted(*permissions)
    }

    override suspend fun grant(ip: String, port: Int, permission: AdbPermission, appPackageName: String) = adbConnectionManager.device(ip, port).forApp(appPackageName).grant(permission)

    override suspend fun revoke(ip: String, port: Int, permission: AdbPermission, appPackageName: String) = adbConnectionManager.device(ip, port).forApp(appPackageName).revoke(permission)
    override suspend fun getDeviceApiLevel(ip: String, port: Int) = runShellCommand(ip, port, "getprop ro.build.version.sdk").mapCatching { output ->
        output.trim().toInt()
    }

    override suspend fun installApk(ip: String, port: Int, apkFilePath: String): Result<Unit> {
        val apiLevelResult = getDeviceApiLevel(ip, port)
        if (apiLevelResult.isFailure) {
            return Result.failure(
                Exception("Failed to get device API level.", apiLevelResult.exceptionOrNull())
            )
        }

        val fileSizeResult = getRemoteFileSize(ip, port, apkFilePath)
        if (fileSizeResult.isFailure) return fileSizeResult.map { Unit }

        val apiLevel = apiLevelResult.getOrThrow()
        val fileSize = fileSizeResult.getOrThrow()
        val installCommandBuilder = StringBuilder("cat \"$apkFilePath\" | pm install -S $fileSize")
        if (apiLevel >= 34) {
            installCommandBuilder.append(" --bypass-low-target-sdk-block")
        }
        val installCommand = installCommandBuilder.toString()

        return runShellCommand(ip, port, installCommand).mapCatching { output ->
            if (output.contains("Success", ignoreCase = true)) {
                Unit
            } else {
                throw Exception("APK installation failed: $output")
            }
        }
    }

    override suspend fun listFiles(ip: String, port: Int, path: String): Result<List<RemoteFile>> {
        val command = "ls -lA \"$path\""

        return runShellCommand(ip, port, command).mapCatching { output ->
            output.lines()
                .filter { it.isNotBlank() && !it.startsWith("total") }
                .mapNotNull { line -> parseLsLine(line, path) }
        }
    }

    private fun parseLsLine(line: String, parentPath: String): RemoteFile? {




        try {
            val isDirectory = line.firstOrNull() == 'd'
            val namePart = line.substringAfterLast(" ").substringBefore(" ->")
            if (namePart.isBlank()) return null

            val fullPath = if (parentPath == "/") "/$namePart" else "$parentPath/$namePart"
            return RemoteFile(
                name = namePart,
                path = fullPath,
                isDirectory = isDirectory
            )
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun getRemoteFileSize(ip: String, port: Int, filePath: String): Result<Long> {
        val fileSizeResult = runShellCommand(ip, port, "stat -c %s \"$filePath\"")
        if (fileSizeResult.isFailure) {
            return Result.failure(
                Exception("Failed to get file size at '$filePath'.", fileSizeResult.exceptionOrNull())
            )
        }
        return fileSizeResult.getOrThrow().trim().toLongOrNull()?.let {
            Result.success(it)
        } ?: Result.failure(Exception("Could not parse file size from command output."))
    }
}