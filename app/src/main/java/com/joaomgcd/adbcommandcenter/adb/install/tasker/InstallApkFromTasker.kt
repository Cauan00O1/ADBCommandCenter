package com.joaomgcd.adbcommandcenter.adb.install.tasker

import android.content.Context
import com.joaomgcd.adbcommandcenter.adb.install.common.domain.InstallApkOnActiveConnectionUseCase
import com.joaomgcd.adbcommandcenter.adb.tasker.runner.TaskerPluginRunnerAdbCommandCenter
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@TaskerInputRoot
class TaskerInstallApkInput @JvmOverloads constructor(
    @field:TaskerInputField("apkFilePath", labelResIdName = "apk_file_path")
    var apkFilePath: String = ""
)

@TaskerOutputObject
class TaskerInstallApkOutput

class TaskerInstallApkHelper(config: TaskerPluginConfig<TaskerInstallApkInput>) :
    TaskerPluginConfigHelper<TaskerInstallApkInput, TaskerInstallApkOutput, TaskerInstallApkRunner>(config) {
    override val runnerClass = TaskerInstallApkRunner::class.java
    override val inputClass = TaskerInstallApkInput::class.java
    override val outputClass = TaskerInstallApkOutput::class.java
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InstallApkEntryPoint {
    val installApk: InstallApkOnActiveConnectionUseCase
}

class TaskerInstallApkRunner : TaskerPluginRunnerAdbCommandCenter<TaskerInstallApkInput, TaskerInstallApkOutput, InstallApkEntryPoint>() {
    override suspend fun runSuspend(
        context: Context,
        entryPoint: InstallApkEntryPoint,
        input: TaskerInput<TaskerInstallApkInput>
    ): Result<TaskerInstallApkOutput> {
        return entryPoint.installApk(input.regular.apkFilePath).map { TaskerInstallApkOutput() }
    }
}