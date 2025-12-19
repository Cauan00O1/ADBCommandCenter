package com.joaomgcd.adbcommandcenter.adb.install.common.domain

import com.joaomgcd.adbcommandcenter.adb.connection.domain.DoWithActiveConnectionOperationUseCase
import javax.inject.Inject

class InstallApkOnActiveConnectionUseCase @Inject constructor(
    private val doWithConnection: DoWithActiveConnectionOperationUseCase,
    private val installApk: InstallApkUseCase,
) {

    suspend operator fun invoke(
        apkFilePath: String
    ): Result<Unit> = doWithConnection { installApk(ip, port, apkFilePath) }
}