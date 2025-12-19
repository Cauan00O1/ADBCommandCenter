package com.joaomgcd.adbcommandcenter.adb.common.domain

import javax.inject.Inject

class IsPairedUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {
    suspend operator fun invoke(ip: String, connectPort: Int): Boolean {
        return adbRepository.isPaired(ip, connectPort)
    }

}