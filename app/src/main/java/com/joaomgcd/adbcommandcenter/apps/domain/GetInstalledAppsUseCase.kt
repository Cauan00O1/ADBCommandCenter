package com.joaomgcd.adbcommandcenter.apps.domain

import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val appsRepository: AppsRepository
) {

    suspend operator fun invoke(): Result<List<AppInfo>> {
        return runCatching {
            withContext(Dispatchers.Default) {
                appsRepository.getInstalledApps()
                    .sortedBy { it.name.lowercase() }
            }
        }
    }
}