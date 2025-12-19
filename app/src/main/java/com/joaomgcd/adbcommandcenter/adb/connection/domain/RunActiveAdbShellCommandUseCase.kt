package com.joaomgcd.adbcommandcenter.adb.connection.domain

import com.joaomgcd.adbcommandcenter.adb.common.domain.RunShellCommandUseCase
import javax.inject.Inject


class RunActiveAdbShellCommandUseCase @Inject constructor(

    private val doWithConnection: DoWithActiveConnectionOperationUseCase,
    private val runShell: RunShellCommandUseCase
) {
    suspend operator fun invoke(command: String): Result<String> {
        return doWithConnection { runShell(ip, port, command) }
    }
}