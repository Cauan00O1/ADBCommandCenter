package com.joaomgcd.adbcommandcenter.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joaomgcd.adbcommandcenter.apps.domain.GetInstalledAppsUseCase
import com.joaomgcd.adbcommandcenter.apps.domain.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.filter

data class AppSelectionUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val error: Throwable? = null
)

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterApps(query)
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getInstalledAppsUseCase()
                .onSuccess { apps ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allApps = apps,
                            filteredApps = apps
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
        }
    }

    private fun filterApps(query: String) {
        viewModelScope.launch {
            val filteredList = withContext(Dispatchers.Default) {
                if (query.isBlank()) {
                    _uiState.value.allApps
                } else {
                    _uiState.value.allApps.filter { app ->
                        app.name.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true)
                    }
                }
            }
            _uiState.update { it.copy(filteredApps = filteredList) }
        }
    }
}