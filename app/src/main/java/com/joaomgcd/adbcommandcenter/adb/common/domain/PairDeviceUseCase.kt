package com.joaomgcd.adbcommandcenter.adb.common.domain

import javax.inject.Inject

class PairDeviceUseCase @Inject constructor(
    private val adbRepository: AdbRepository
) {
    suspend operator fun invoke(ip: String, port: Int, code: String): Result<Unit> {
        return adbRepository.pairDevice(ip, port, code)
    }
}