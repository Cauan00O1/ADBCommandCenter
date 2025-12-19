package com.joaomgcd.adbcommandcenter.adb.tasker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.joaomgcd.adbcommandcenter.ui.theme.ADBCommandCenterTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class TaskerPluginConfigActivity<TInput : Any, TViewModel : TaskerPluginConfigViewModel<TInput>> : ComponentActivity(), TaskerPluginConfig<TInput> {
    override val context get() = this
    override val inputForTasker get() = TaskerInput(viewModel.inputFromState)
    protected abstract val taskerHelper: TaskerPluginConfigHelper<TInput, *, *>
    protected abstract val viewModel: TViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()

        setContent {
            ADBCommandCenterTheme {
                TaskerPluginConfigScreen(viewModel) {
                    Content(viewModel)
                }
            }
        }
        taskerHelper.onCreate()
    }

    @Composable
    abstract fun ColumnScope.Content(viewModel: TViewModel)
    private fun observeViewModelEvents() {
        viewModel.events.onEach { event ->
            when (event) {
                is TaskerPluginConfigEvent.SaveAndFinish -> taskerHelper.finishForTasker()
                TaskerPluginConfigEvent.CancelAndFinish -> finish()
            }
        }.launchIn(lifecycleScope)
    }

    override fun assignFromInput(input: TaskerInput<TInput>) {
        viewModel.updateInput(input.regular)
    }
}