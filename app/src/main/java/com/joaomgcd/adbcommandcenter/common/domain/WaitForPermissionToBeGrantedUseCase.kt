package com.joaomgcd.adbcommandcenter.common.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class WaitForPermissionToBeGrantedUseCase @Inject constructor(
    private val checkPermissionUseCase: CheckNormalPermissionViaAndroidApiUseCase
) {

    suspend operator fun invoke(permission: String) {

        if (checkPermissionUseCase(permission)) {
            return
        }


        while (coroutineContext.isActive) {
            if (checkPermissionUseCase(permission)) {
                return
            }
            delay(1000)
        }
    }
}