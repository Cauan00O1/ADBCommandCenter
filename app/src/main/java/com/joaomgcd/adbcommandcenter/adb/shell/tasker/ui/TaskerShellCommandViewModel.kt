package com.joaomgcd.adbcommandcenter.adb.shell.tasker.ui

import com.joaomgcd.adbcommandcenter.adb.shell.tasker.TaskerShellCommandInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class TaskerShellCommandViewModel @Inject constructor() : TaskerPluginConfigViewModel<TaskerShellCommandInput>() {


    fun onCommandChanged(newCommand: String) {
        updateInput(TaskerShellCommandInput(newCommand))
    }

    override val defaultInput = TaskerShellCommandInput()
    override val defaultTitle = "ADB Shell Command"


}