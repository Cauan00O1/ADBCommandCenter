package com.joaomgcd.adbcommandcenter.adb.install.tasker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.joaomgcd.adbcommandcenter.adb.filebrowser.ui.FileSelectionScreen
import com.joaomgcd.adbcommandcenter.adb.install.tasker.TaskerInstallApkHelper
import com.joaomgcd.adbcommandcenter.adb.install.tasker.TaskerInstallApkInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskerInstallApkActivity : TaskerPluginConfigActivity<TaskerInstallApkInput, TaskerInstallApkViewModel>() {
    override val taskerHelper by lazy { TaskerInstallApkHelper(this) }
    override val viewModel: TaskerInstallApkViewModel by viewModels()

    @Composable
    override fun ColumnScope.Content(viewModel: TaskerInstallApkViewModel) {
        var showFileBrowser by remember { mutableStateOf(false) }

        BackHandler(enabled = showFileBrowser) {
            showFileBrowser = false
        }

        if (showFileBrowser) {
            FileSelectionScreen(
                onFileSelected = { path ->
                    viewModel.onPathChanged(path)
                    showFileBrowser = false
                },
                onBackPressed = {
                    showFileBrowser = false
                }
            )
        } else {
            TaskerInstallApkInputScreen(
                viewModel = viewModel,
                onBrowseClick = { showFileBrowser = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskerInstallApkInputScreen(
    viewModel: TaskerInstallApkViewModel,
    onBrowseClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = uiState.input.apkFilePath)) }

    LaunchedEffect(uiState.input.apkFilePath) {
        if (textFieldValue.text != uiState.input.apkFilePath) {
            textFieldValue = textFieldValue.copy(
                text = uiState.input.apkFilePath,
                selection = TextRange(uiState.input.apkFilePath.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            viewModel.onPathChanged(it.text)
        },
        label = { Text("APK File Path") },
        trailingIcon = {
            IconButton(onClick = onBrowseClick) {
                Icon(Icons.Default.Folder, contentDescription = "Select File")
            }
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
    )
}