package com.joaomgcd.adbcommandcenter.adb.install.app.ui

import com.joaomgcd.adbcommandcenter.adb.install.app.domain.InstallableFileInfo

sealed interface InstallApkUiState {
    object Loading : InstallApkUiState
    data class Error(val message: String) : InstallApkUiState
    data class ReadyToInstall(val fileInfo: InstallableFileInfo) : InstallApkUiState
    data class Installing(val fileInfo: InstallableFileInfo) : InstallApkUiState
    data class Success(val fileInfo: InstallableFileInfo) : InstallApkUiState
}