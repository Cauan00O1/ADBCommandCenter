package com.joaomgcd.adbcommandcenter.adb.connection.domain

import javax.inject.Inject

class DoWithActiveConnectionOperationUseCase @Inject constructor(
    private val getActiveConnection: WaitForActiveConnectionUseCase
) {
    suspend operator fun <T> invoke(operation: suspend AdbConnection.() -> Result<T>): Result<T> {

        val connection = getActiveConnection()
            ?: return Result.failure(IllegalStateException("App not connected to ADB"))


        return connection.operation()
    }
}