package com.joaomgcd.adbcommandcenter.adb.filebrowser.ui

import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.RemoteFile

data class FileSelectionUiState(
    val isLoading: Boolean = true,
    val currentPath: String = "/",
    val isRootPath: Boolean = true,
    val files: List<RemoteFile> = emptyList(),
    val error: String? = null
)