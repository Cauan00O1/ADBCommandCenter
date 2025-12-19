package com.joaomgcd.adbcommandcenter.adb.tasker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskerPluginConfigState<TInput>(val input: TInput, val title: String)
abstract class TaskerPluginConfigViewModel<TInput>() : ViewModel() {

    abstract val defaultInput: TInput
    abstract val defaultTitle: String
    private val _uiState by lazy { MutableStateFlow(TaskerPluginConfigState(defaultInput, defaultTitle)) }
    val uiState by lazy { _uiState.asStateFlow() }
    val inputFromState get() = uiState.value.input

    private val _events = MutableSharedFlow<TaskerPluginConfigEvent>()
    val events = _events.asSharedFlow()

    fun updateInput(input: TInput) {
        _uiState.update { it.copy(input = input) }
    }


    fun onSaveClicked() {
        viewModelScope.launch {
            _events.emit(TaskerPluginConfigEvent.SaveAndFinish)
        }
    }

    fun onCancelClicked() {
        viewModelScope.launch {
            _events.emit(TaskerPluginConfigEvent.CancelAndFinish)
        }
    }
}