package com.joaomgcd.adbcommandcenter.adb.install.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbActiveConnectionRepository
import com.joaomgcd.adbcommandcenter.adb.install.app.domain.GetInstallableFileInfoUseCase
import com.joaomgcd.adbcommandcenter.adb.install.common.domain.InstallApkUseCase
import com.joaomgcd.adbcommandcenter.main.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstallApkViewModel @Inject constructor(
    private val getInstallableFileInfoUseCase: GetInstallableFileInfoUseCase,
    private val installApkUseCase: InstallApkUseCase,
    private val activeConnectionRepository: AdbActiveConnectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val apkFilePath: String = savedStateHandle.toRoute<Screen.InstallApk>().filePath

    private val _uiState = MutableStateFlow<InstallApkUiState>(InstallApkUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val connection = activeConnectionRepository.connectionState.filterNotNull().first()
            getInstallableFileInfoUseCase(connection.ip, connection.port, apkFilePath)
                .onSuccess { fileInfo ->
                    _uiState.value = InstallApkUiState.ReadyToInstall(fileInfo)
                }
                .onFailure { error ->
                    _uiState.value = InstallApkUiState.Error(error.message ?: "Failed to get file info.")
                }
        }
    }


    fun onInstallConfirmed() {
        val currentState = _uiState.value
        if (currentState !is InstallApkUiState.ReadyToInstall) return

        _uiState.value = InstallApkUiState.Installing(currentState.fileInfo)

        viewModelScope.launch {
            val connection = activeConnectionRepository.connectionState.value ?: return@launch
            installApkUseCase(connection.ip, connection.port, apkFilePath)
                .onSuccess {
                    _uiState.value = InstallApkUiState.Success(currentState.fileInfo)
                }
                .onFailure { error ->
                    _uiState.value = InstallApkUiState.Error(error.message ?: "Installation failed.")
                }
        }
    }
}