package com.joaomgcd.adbcommandcenter.adb.permissions.app.ui


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnection
import com.joaomgcd.adbcommandcenter.adb.connection.domain.GetActiveConnectionUseCase
import com.joaomgcd.adbcommandcenter.adb.permissions.app.domain.TogglePermissionUseCase
import com.joaomgcd.adbcommandcenter.adb.permissions.common.domain.CheckPermissionsViaAdbUseCase
import com.joaomgcd.adbcommandcenter.apps.domain.GetAppNameUseCase
import com.joaomgcd.adbcommandcenter.apps.domain.GetAppPermissionsUseCase
import com.joaomgcd.adbcommandcenter.main.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPermissionsUiState(
    val isLoading: Boolean = true,
    val appPackageName: String = "",
    val appName: String = "",
    val searchQuery: String = "",
    val permissions: Map<AdbPermission.Single, Boolean> = emptyMap(),
    val filteredPermissions: List<AdbPermission.Single> = emptyList(),
    val error: Throwable? = null
)

@HiltViewModel
class AppPermissionsViewModel @Inject constructor(
    private val getAppPermissions: GetAppPermissionsUseCase,
    private val checkPermissions: CheckPermissionsViaAdbUseCase,
    private val togglePermission: TogglePermissionUseCase,
    private val getActiveConnection: GetActiveConnectionUseCase,
    private val getAppName: GetAppNameUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPermissionsUiState())
    val uiState: StateFlow<AppPermissionsUiState> = _uiState.asStateFlow()

    private val permissionArgs = savedStateHandle.toRoute<Screen.Permissions>()
    val appPackageName get() = permissionArgs.packageName
    private var activeConnection: AdbConnection? = null

    init {
        viewModelScope.launch {
            val appName = getAppName(appPackageName)
            _uiState.update { it.copy(appPackageName = appPackageName, appName = appName) }
        }

        getActiveConnection()
            .onEach { connection ->
                activeConnection = connection
                if (connection != null) {
                    loadPermissions()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = IllegalStateException("Device not connected.")
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun grantPermission(permission: AdbPermission.Single) {
        togglePermission(permission, isCurrentlyGranted = false)
    }

    fun revokePermission(permission: AdbPermission.Single) {
        togglePermission(permission, isCurrentlyGranted = true)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredPermissions = current.permissions.filtered(query)
            )
        }
    }

    private fun Map<AdbPermission.Single, Boolean>.filtered(query: String) = filterPermissions(keys, query)
    private fun filterPermissions(
        permissions: Set<AdbPermission.Single>,
        query: String
    ): List<AdbPermission.Single> {
        return permissions
            .filter {
                it.displayName.contains(query, ignoreCase = true) ||
                        it.permissionName.contains(query, ignoreCase = true)
            }
            .sortedBy { it.displayName }
    }

    private fun loadPermissions() {
        val connection = activeConnection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getAppPermissions(appPackageName)
                .onSuccess { permissionGroups ->
                    val singlePermissions = permissionGroups.flatMap { it.singlePermissions }.distinct()
                    checkInitialPermissionStates(singlePermissions)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
        }
    }

    private fun checkInitialPermissionStates(permissions: List<AdbPermission.Single>) {
        val connection = activeConnection ?: return
        if (permissions.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, permissions = emptyMap(), filteredPermissions = emptyList()) }
            return
        }
        viewModelScope.launch {
            checkPermissions(connection.ip, connection.port, appPackageName, *permissions.toTypedArray())
                .onSuccess { statuses ->
                    val permissionMap = statuses.list.associate { (perm, isGranted) ->
                        (perm as AdbPermission.Single) to isGranted
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            permissions = permissionMap,
                            filteredPermissions = permissionMap.filtered(_uiState.value.searchQuery)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
        }
    }

    private fun togglePermission(permission: AdbPermission.Single, isCurrentlyGranted: Boolean) {
        val connection = activeConnection ?: return
        viewModelScope.launch {
            togglePermission(connection.ip, connection.port, permission, appPackageName, isCurrentlyGranted)
                .onSuccess {
                    _uiState.update { currentState ->
                        val updatedPermissions = currentState.permissions.toMutableMap()
                        updatedPermissions[permission] = !isCurrentlyGranted
                        currentState.copy(permissions = updatedPermissions)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error) }
                }
        }
    }
}