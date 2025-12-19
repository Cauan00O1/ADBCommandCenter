package com.joaomgcd.adbcommandcenter.adb.install.app.domain

import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import javax.inject.Inject

class GetInstallableFileInfoUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        filePath: String
    ): Result<InstallableFileInfo> {
        return adbRepository.getRemoteFileSize(ip, port, filePath).map { size ->
            InstallableFileInfo(
                fileName = filePath.substringAfterLast('/'),
                filePath = filePath,
                fileSizeInBytes = size
            )
        }
    }
}