package com.joaomgcd.adbcommandcenter.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.joaomgcd.adbcommandcenter.adb.filebrowser.ui.FileSelectionScreen
import com.joaomgcd.adbcommandcenter.adb.install.app.ui.InstallApkScreen
import com.joaomgcd.adbcommandcenter.adb.permissions.app.ui.AppPermissionsScreen
import com.joaomgcd.adbcommandcenter.apps.ui.AppSelectionScreen
import com.joaomgcd.adbcommandcenter.main.navigation.Screen
import com.joaomgcd.adbcommandcenter.main.ui.MainScreen
import com.joaomgcd.adbcommandcenter.shell.presentation.ShellCommandScreen



@Composable
fun AdbControlCenterApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard) {

        composable<Screen.Dashboard> { backStackEntry ->

            MainScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        val onBackPressed: () -> Unit = { navController.navigateUp() }
        composable<Screen.AppSelection> { backStackEntry ->
            val mode = backStackEntry.toRoute<Screen.AppSelection>().mode
            AppSelectionScreen(
                screenTitle = { Text("Select an App") },
                onAppSelected = { appInfo ->
                    navController.navigate(mode.next(appInfo.packageName))
                },
                onBackPressed = onBackPressed
            )
        }
        composable<Screen.FileSelection> { backStackEntry ->
            val mode = backStackEntry.toRoute<Screen.FileSelection>().mode
            FileSelectionScreen(
                onFileSelected = { filePath ->
                    navController.navigate(mode.next(filePath))
                },
                onBackPressed = onBackPressed
            )
        }

        composable<Screen.Shell> {
            ShellCommandScreen(
                onBackPressed = onBackPressed
            )
        }

        composable<Screen.Permissions> { backStackEntry ->
            AppPermissionsScreen(onBackPressed = onBackPressed)
        }

        composable<Screen.Freezing> { backStackEntry ->
            ShellCommandScreen(onBackPressed = onBackPressed)
        }
        composable<Screen.InstallApk> { backStackEntry ->
            InstallApkScreen(onBackPressed = onBackPressed)
        }
    }
}