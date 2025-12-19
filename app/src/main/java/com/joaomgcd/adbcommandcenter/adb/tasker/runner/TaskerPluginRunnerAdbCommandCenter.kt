package com.joaomgcd.adbcommandcenter.adb.tasker.runner

import android.content.Context
import com.joaomgcd.adbcommandcenter.adb.permissions.common.domain.ToggleActiveConnectionPermissionUseCase
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultErrorWithOutput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.lang.reflect.ParameterizedType


abstract class TaskerPluginRunnerAdbCommandCenter<TInput : Any, TOutput : Any, TEntryPoint : Any>(
) : TaskerPluginRunnerAction<TInput, TOutput>() {
    @Suppress("UNCHECKED_CAST")
    private val entryPointClass: Class<TEntryPoint> by lazy {

        val superclass = javaClass.genericSuperclass as? ParameterizedType
            ?: throw IllegalStateException("Class must be parameterized")



        superclass.actualTypeArguments[2] as Class<TEntryPoint>
    }
    override fun run(context: Context, input: TaskerInput<TInput>): TaskerPluginResult<TOutput> {
        val entryPoint = EntryPointAccessors.fromApplication(context, entryPointClass)

        val result = runBlocking {
            runSuspend(context, entryPoint, input)
        }

        return if (result.isSuccess) {
            TaskerPluginResultSucess(result.getOrNull())
        } else {
            TaskerPluginResultErrorWithOutput(result.exceptionOrNull() ?: RuntimeException("Unknown error"))
        }
    }

    abstract suspend fun runSuspend(context: Context, entryPoint: TEntryPoint, input: TaskerInput<TInput>): Result<TOutput>
}