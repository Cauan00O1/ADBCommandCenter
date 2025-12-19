package com.joaomgcd.adbcommandcenter.adb.connection.domain

import com.joaomgcd.adbcommandcenter.discovery.PairingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetActiveConnectionUseCase @Inject constructor(
    private val repository: AdbActiveConnectionRepository
) {
    operator fun invoke(): StateFlow<AdbConnection?> = repository.connectionState
}