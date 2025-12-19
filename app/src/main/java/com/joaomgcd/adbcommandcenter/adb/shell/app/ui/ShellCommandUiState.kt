package com.joaomgcd.adbcommandcenter.adb.shell.app.ui



data class ShellUiState(
    val isVisible: Boolean = false,
    val connectedHost: String? = null,
    val commandInput: String = "",
    val logs: List<CommandLog> = emptyList(),
    val isLoading: Boolean = false
)
data class CommandLog(
    val timestamp: String,
    val command: String,
    val output: String,
    val isSuccess: Boolean
)