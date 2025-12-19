package com.joaomgcd.adbcommandcenter.adb.install.tasker.ui

import com.joaomgcd.adbcommandcenter.adb.install.tasker.TaskerInstallApkInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TaskerInstallApkViewModel @Inject constructor() : TaskerPluginConfigViewModel<TaskerInstallApkInput>() {

    fun onPathChanged(newPath: String) {
        updateInput(TaskerInstallApkInput(newPath))
    }

    override val defaultInput = TaskerInstallApkInput()
    override val defaultTitle = "Install APK"
}