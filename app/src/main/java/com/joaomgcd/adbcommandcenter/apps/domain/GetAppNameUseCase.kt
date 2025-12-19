package com.joaomgcd.adbcommandcenter.apps.domain

import javax.inject.Inject

class GetAppNameUseCase @Inject constructor(
    private val appsRepository: AppsRepository
) {
    suspend operator fun invoke(packageName: String): String {
        return appsRepository.getAppName(packageName)
    }
}