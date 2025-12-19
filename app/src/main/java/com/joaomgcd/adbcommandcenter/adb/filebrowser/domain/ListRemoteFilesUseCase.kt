package com.joaomgcd.adbcommandcenter.adb.filebrowser.domain

import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import javax.inject.Inject

class ListRemoteFilesUseCase @Inject constructor(private val adbRepository: AdbRepository) {
    suspend operator fun invoke(
        ip: String,
        port: Int,
        path: String
    ) = adbRepository.listFiles(ip, port, path)
}