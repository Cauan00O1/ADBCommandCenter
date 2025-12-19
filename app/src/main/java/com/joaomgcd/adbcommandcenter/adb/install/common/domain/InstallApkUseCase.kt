package com.joaomgcd.adbcommandcenter.adb.install.common.domain

import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import javax.inject.Inject

class InstallApkUseCase @Inject constructor(private val adbRepository: AdbRepository) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        apkFilePath: String
    ) = adbRepository.installApk(ip, port, apkFilePath)
}