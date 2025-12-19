package com.joaomgcd.adbcommandcenter.adb.shell.tasker.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joaomgcd.adbcommandcenter.adb.shell.tasker.TaskerShellCommandHelper
import com.joaomgcd.adbcommandcenter.adb.shell.tasker.TaskerShellCommandInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskerShellCommandActivity : TaskerPluginConfigActivity<TaskerShellCommandInput, TaskerShellCommandViewModel>() {
    override val taskerHelper by lazy { TaskerShellCommandHelper(this) }
    override val viewModel: TaskerShellCommandViewModel by viewModels()

    @Composable
    override fun ColumnScope.Content(viewModel: TaskerShellCommandViewModel) {
        TaskerShellCommandScreen(viewModel = viewModel)
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.TaskerShellCommandScreen(
    viewModel: TaskerShellCommandViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = uiState.input.command))
    }

    LaunchedEffect(Unit) {
        textFieldValue = textFieldValue.copy(
            selection = TextRange(0, textFieldValue.text.length)
        )
        focusRequester.requestFocus()
    }


    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            viewModel.onCommandChanged(it.text)
        },
        label = { Text("ADB Shell Command") },
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
    )
}