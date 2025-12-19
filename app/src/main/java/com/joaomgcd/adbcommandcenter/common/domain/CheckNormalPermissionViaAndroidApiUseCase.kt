package com.joaomgcd.adbcommandcenter.common.domain

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckNormalPermissionViaAndroidApiUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}