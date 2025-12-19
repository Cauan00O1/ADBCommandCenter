package com.joaomgcd.adbcommandcenter.adb.permissions.tasker.ui

import androidx.lifecycle.viewModelScope
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.permissions.tasker.TogglePermissionFromTaskerInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigViewModel
import com.joaomgcd.adbcommandcenter.apps.domain.GetAppPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class TogglePermissionFromTaskerViewModel @Inject  constructor(
    private val getAppPermissions: GetAppPermissionsUseCase
) : TaskerPluginConfigViewModel<TogglePermissionFromTaskerInput>() {

    private val _availablePermissions = MutableStateFlow<List<AdbPermission>>(emptyList())
    val availablePermissions = _availablePermissions.asStateFlow()

    fun onCommandChanged(newCommand: String) {
        updateInput(TogglePermissionFromTaskerInput(newCommand))
    }

    override val defaultInput = TogglePermissionFromTaskerInput()
    override val defaultTitle = "ADB Shell Command"
    init {

        val savedPackage = uiState.value.input.appPackageName
        if (savedPackage.isNotEmpty()) {
            loadPermissionsForPackage(savedPackage)
        }
    }
    fun onAppPackageSelected(packageName: String) {

        updateInput(
            uiState.value.input.copy(
                appPackageName = packageName,
                permission = ""
            )
        )
        loadPermissionsForPackage(packageName)
    }
    private fun loadPermissionsForPackage(packageName: String) {
        viewModelScope.launch {

            val permissions = getAppPermissions(packageName).getOrDefault(emptyList())
            _availablePermissions.value = permissions
        }
    }

    fun onPermissionSelected(permission: AdbPermission) {


        val valueToStore = (permission as? AdbPermission.Single)?.permissionName ?: permission.displayName

        updateInput(uiState.value.input.copy(permission = valueToStore))
    }


    fun onPermissionTextChanged(text: String) {
        updateInput(uiState.value.input.copy(permission = text))
    }

    fun onStateChanged(newState: Boolean?) {
        updateInput(uiState.value.input.copy(state = newState))
    }
}