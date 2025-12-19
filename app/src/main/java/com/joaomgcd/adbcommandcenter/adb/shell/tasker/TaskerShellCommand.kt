package com.joaomgcd.adbcommandcenter.adb.shell.tasker

import android.content.Context
import com.joaomgcd.adbcommandcenter.adb.connection.domain.RunActiveAdbShellCommandUseCase
import com.joaomgcd.adbcommandcenter.adb.tasker.runner.TaskerPluginRunnerAdbCommandCenter
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


/***
 * IO
 */
@TaskerInputRoot
class TaskerShellCommandInput @JvmOverloads constructor(
    @field:TaskerInputField("command", labelResIdName = "command")
    var command: String = ""
)

@TaskerOutputObject
class TaskerShellCommandOutput(
    @get:TaskerOutputVariable("result", labelResIdName = "result", htmlLabelResIdName = "shell_output_result_description")
    var result: String?
)


/***
 * Helper
 */
class TaskerShellCommandHelper(config: TaskerPluginConfig<TaskerShellCommandInput>) :
    TaskerPluginConfigHelper<TaskerShellCommandInput, TaskerShellCommandOutput, TaskerShellCommandRunner>(config) {
    override val runnerClass = TaskerShellCommandRunner::class.java
    override val inputClass = TaskerShellCommandInput::class.java
    override val outputClass = TaskerShellCommandOutput::class.java
}


/***
 * Runner
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShellCommandEntryPoint {
    val runShellCommand: RunActiveAdbShellCommandUseCase
}
class TaskerShellCommandRunner : TaskerPluginRunnerAdbCommandCenter<TaskerShellCommandInput, TaskerShellCommandOutput, ShellCommandEntryPoint>() {

    override suspend fun runSuspend(context: Context, entryPoint: ShellCommandEntryPoint, input: TaskerInput<TaskerShellCommandInput>): Result<TaskerShellCommandOutput> {
        return entryPoint.runShellCommand(input.regular.command).map { TaskerShellCommandOutput(it) }
    }
}
