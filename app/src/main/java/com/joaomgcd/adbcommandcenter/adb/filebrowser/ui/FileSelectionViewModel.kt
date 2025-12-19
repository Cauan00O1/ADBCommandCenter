package com.joaomgcd.adbcommandcenter.adb.filebrowser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnection
import com.joaomgcd.adbcommandcenter.adb.connection.domain.GetActiveConnectionUseCase
import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.ListRemoteFilesUseCase
import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.RemoteFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROOT_PATH = "/"
private const val INITIAL_PATH = "/storage/emulated/0"
private val String.isRootPath: Boolean get() = this == ROOT_PATH

@HiltViewModel
class FileSelectionViewModel @Inject constructor(
    private val listRemoteFiles: ListRemoteFilesUseCase,
    private val getActiveConnection: GetActiveConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileSelectionUiState())
    val uiState = _uiState.asStateFlow()

    private var activeConnection: AdbConnection? = null

    init {
        getActiveConnection()
            .onEach { connection ->
                activeConnection = connection
                if (connection != null) {
                    if (uiState.value.error != null || uiState.value.files.isEmpty()) {
                        loadDirectory(INITIAL_PATH)
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Device not connected.")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onFileClicked(file: RemoteFile) {
        if (file.isDirectory) {
            loadDirectory(file.path)
        } else {


        }
    }

    fun onUpClicked() {
        val currentPath = _uiState.value.currentPath
        if (currentPath != ROOT_PATH) {
            val parentPath = currentPath.substringBeforeLast('/', "/")
            loadDirectory(parentPath.ifEmpty { ROOT_PATH })
        }
    }

    private fun loadDirectory(path: String) {
        val connection = activeConnection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            listRemoteFiles(connection.ip, connection.port, path)
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentPath = path,
                            isRootPath = path.isRootPath,
                            files = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message)
                    }
                }
        }
    }
}