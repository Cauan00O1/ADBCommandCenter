package com.joaomgcd.adbcommandcenter.apps.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.sorted

class GetAppPermissionsUseCase @Inject constructor(
    private val appsRepository: AppsRepository
) {

    suspend operator fun invoke(packageName: String): Result<List<AdbPermission>> {
        return runCatching {
            withContext(Dispatchers.Default) {
                appsRepository.getAppPermissions(packageName)
                    .sortedBy { it.displayName }
            }
        }
    }
}