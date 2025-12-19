package com.joaomgcd.adbcommandcenter.shell.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joaomgcd.adbcommandcenter.adb.common.domain.RunShellCommandUseCase
import com.joaomgcd.adbcommandcenter.adb.connection.domain.GetPairingStateUseCase
import com.joaomgcd.adbcommandcenter.adb.shell.app.ui.CommandLog
import com.joaomgcd.adbcommandcenter.adb.shell.app.ui.ShellUiState
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class ShellCommandViewModel @Inject constructor(
    private val runShellCommandUseCase: RunShellCommandUseCase,
    getPairingStateUseCase: GetPairingStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()


    private var activeConnection: Pair<String, Int>? = null

    init {
        getPairingStateUseCase()
            .onEach { pairingState ->
                _uiState.update { state ->
                    if (pairingState is PairingState.Paired) {
                        val ip = pairingState.connectionService.host.hostAddress ?: ""
                        val port = pairingState.connectionService.port
                        activeConnection = ip to port

                        state.copy(
                            isVisible = true,
                            connectedHost = "$ip:$port"
                        )
                    } else {
                        activeConnection = null
                        state.copy(isVisible = false, connectedHost = null)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCommandInputChange(newInput: String) {
        _uiState.update { it.copy(commandInput = newInput) }
    }

    fun executeCommand() {
        val currentState = _uiState.value
        val connection = activeConnection

        if (!currentState.isVisible || currentState.commandInput.isBlank() || connection == null) return

        _uiState.update { it.copy(isLoading = true) }
        val cmdToRun = currentState.commandInput

        viewModelScope.launch {
            val result = runShellCommandUseCase(
                ip = connection.first,
                port = connection.second,
                command = cmdToRun
            )

            val timestamp = System.currentTimeMillis()

            val newLog = result.fold(
                onSuccess = { output ->
                    CommandLog(timestamp.formatTimestamp(), cmdToRun, output, isSuccess = true)
                },
                onFailure = { error ->
                    CommandLog(timestamp.formatTimestamp(), cmdToRun, error.message ?: "Unknown Error", isSuccess = false)
                }
            )

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    commandInput = "",
                    logs = listOf(newLog) + state.logs
                )
            }
        }
    }

    private fun Long.formatTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(this))
    }
}