package com.joaomgcd.adbcommandcenter.apps.domain

import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo


interface AppsRepository {

    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getAppPermissions(packageName: String): List<AdbPermission>
    suspend fun getAppName(packageName: String): String
}