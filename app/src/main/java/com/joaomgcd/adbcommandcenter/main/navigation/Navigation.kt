package com.joaomgcd.adbcommandcenter.main.navigation

import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {


    @Serializable
    object Dashboard : Screen()


    @Serializable
    object Shell : Screen()
    @Serializable
    data class AppSelection(val mode: Mode) : Screen() {
        enum class Mode {
            Permissions { override fun next(pkg: String) = Permissions(pkg) },
            Freezing { override fun next(pkg: String) = Freezing(pkg) };

            abstract fun next(pkg: String): Screen
        }
    }
    @Serializable
    data class FileSelection(val mode: Mode) : Screen() {
        enum class Mode {
            InstallApk { override fun next(filePath: String) = InstallApk(filePath) };

            abstract fun next(filePath: String): Screen
        }
    }


    @Serializable
    data class Permissions(val packageName: String) : Screen()


    @Serializable
    data class Freezing(val packageName: String) : Screen()
    @Serializable
    data class InstallApk(val filePath: String) : Screen()
}
