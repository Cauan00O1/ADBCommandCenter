package com.joaomgcd.adbcommandcenter.adb.permissions.tasker

import android.content.Context
import com.joaomgcd.adbcommandcenter.adb.permissions.common.domain.ToggleActiveConnectionPermissionUseCase
import com.joaomgcd.adbcommandcenter.adb.tasker.runner.TaskerPluginRunnerAdbCommandCenter
import com.joaomgcd.adbcommandcenter.common.domain.ToggleState
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


/***
 * IO
 */
@TaskerInputRoot
data class TogglePermissionFromTaskerInput @JvmOverloads constructor(
    @field:TaskerInputField("app_package", labelResIdName = "app_package")
    var appPackageName: String = "",
    @field:TaskerInputField("permission", labelResIdName = "permission", descriptionResIdName = "tasker_permission_input_explained")
    var permission: String = "",
    @field:TaskerInputField("state", labelResIdName = "state")
    var state: Boolean? = null
)

@TaskerOutputObject
class TogglePermissionFromTaskerOutput constructor(
)


/***
 * Helper
 */

class TogglePermissionFromTaskerHelper(config: TaskerPluginConfig<TogglePermissionFromTaskerInput>) :
    TaskerPluginConfigHelper<TogglePermissionFromTaskerInput, TogglePermissionFromTaskerOutput, TogglePermissionFromTaskerRunnerAdbCommandCenter>(config) {
    override val runnerClass = TogglePermissionFromTaskerRunnerAdbCommandCenter::class.java
    override val inputClass = TogglePermissionFromTaskerInput::class.java
    override val outputClass = TogglePermissionFromTaskerOutput::class.java
    override fun addToStringBlurb(input: TaskerInput<TogglePermissionFromTaskerInput>, blurbBuilder: StringBuilder) {
        val args = input.regular
        val action = when (args.state) {
            true -> "Grant"
            false -> "Revoke"
            else -> "Toggle"
        }
        blurbBuilder.append("$action ${args.permission} for ${args.appPackageName}")
    }

    override val addDefaultStringBlurb = false
}

/***
 * Runner
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TogglePermissionEntryPoint {
    val togglePermissions: ToggleActiveConnectionPermissionUseCase
}
class TogglePermissionFromTaskerRunnerAdbCommandCenter : TaskerPluginRunnerAdbCommandCenter<TogglePermissionFromTaskerInput, TogglePermissionFromTaskerOutput, TogglePermissionEntryPoint>() {


    override suspend fun runSuspend(context: Context, entryPoint: TogglePermissionEntryPoint, input: TaskerInput<TogglePermissionFromTaskerInput>): Result<TogglePermissionFromTaskerOutput> {
        val taskerInput = input.regular
        val toggleState = when (taskerInput.state) {
            true -> ToggleState.Enable
            false -> ToggleState.Disable
            null -> ToggleState.Toggle
        }
        return entryPoint.togglePermissions(taskerInput.appPackageName, taskerInput.permission, toggleState).map { TogglePermissionFromTaskerOutput() }
    }
}
