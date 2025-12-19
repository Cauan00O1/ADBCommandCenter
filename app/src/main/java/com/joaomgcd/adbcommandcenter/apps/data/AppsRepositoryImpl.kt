package com.joaomgcd.adbcommandcenter.apps.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.apps.domain.AppsRepository
import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppsRepository {

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedInfos = packageManager.queryIntentActivities(mainIntent, 0)

        resolvedInfos.map { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            AppInfo(
                name = appInfo.loadLabel(packageManager).toString(),
                packageName = appInfo.packageName,
                icon = appInfo.loadIcon(packageManager)
            )
        }.distinctBy { it.packageName }
    }
    override suspend fun getAppPermissions(packageName: String): List<AdbPermission> = withContext(Dispatchers.IO) {
        AdbPermission.fromPackage(context, packageName)
    }
    override suspend fun getAppName(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            info.loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}