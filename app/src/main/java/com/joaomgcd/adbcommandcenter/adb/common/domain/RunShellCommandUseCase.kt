package com.joaomgcd.adbcommandcenter.adb.common.domain

import javax.inject.Inject


class RunShellCommandUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {
    suspend operator fun invoke(ip: String, port: Int, command: String) = adbRepository.runShellCommand(ip, port, command)
}