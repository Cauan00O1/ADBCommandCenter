package com.joaomgcd.adbcommandcenter.adb.connection.domain

import javax.inject.Inject

class WaitForActiveConnectionUseCase @Inject constructor(
    private val repository: AdbActiveConnectionRepository
) {
    suspend operator fun invoke() = repository.waitForConnection(5000)
}