package com.joaomgcd.adbcommandcenter.adb.connection.domain

import com.joaomgcd.adbcommandcenter.discovery.PairingState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPairingStateUseCase @Inject constructor(
    private val repository: AdbPairingRepository
) {
    operator fun invoke(): Flow<PairingState> = repository.state
}