package com.joaomgcd.adbcommandcenter.adb.permissions.tasker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joaomgcd.adb.AdbPermission
import com.joaomgcd.adbcommandcenter.adb.permissions.tasker.TogglePermissionFromTaskerHelper
import com.joaomgcd.adbcommandcenter.adb.permissions.tasker.TogglePermissionFromTaskerInput
import com.joaomgcd.adbcommandcenter.adb.tasker.ui.TaskerPluginConfigActivity
import com.joaomgcd.adbcommandcenter.apps.ui.AppSelectionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TogglePermissionFromTaskerActivity : TaskerPluginConfigActivity<TogglePermissionFromTaskerInput, TogglePermissionFromTaskerViewModel>() {
    override val taskerHelper by lazy { TogglePermissionFromTaskerHelper(this) }
    override val viewModel: TogglePermissionFromTaskerViewModel by viewModels()

    @Composable
    override fun ColumnScope.Content(viewModel: TogglePermissionFromTaskerViewModel) {
        TogglePermissionFromTaskerScreen(viewModel = viewModel)
    }

}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.TogglePermissionFromTaskerScreen(
    viewModel: TogglePermissionFromTaskerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availablePermissions by viewModel.availablePermissions.collectAsStateWithLifecycle()


    var isSelectingApp by remember { mutableStateOf(false) }


    BackHandler(enabled = isSelectingApp) {
        isSelectingApp = false
    }

    Crossfade(targetState = isSelectingApp, label = "ScreenNavigation") { showAppSelector ->
        if (showAppSelector) {
            AppSelectionScreen(
                onAppSelected = { appInfo ->
                    viewModel.onAppPackageSelected(appInfo.packageName)
                    isSelectingApp = false
                },
                onBackPressed = { isSelectingApp = false },
                screenTitle = { Text("Select App") }
            )
        } else {

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {


                AppPackageInput(
                    packageName = uiState.input.appPackageName,
                    onClick = { isSelectingApp = true }
                )


                if (uiState.input.appPackageName.isNotEmpty()) {
                    PermissionDropdown(
                        currentValue = uiState.input.permission,
                        options = availablePermissions,
                        onPermissionSelected = viewModel::onPermissionSelected,
                        onTextChange = viewModel::onPermissionTextChanged
                    )
                }


                Text(
                    text = "Action",
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentState = uiState.input.state


                    FilterChip(
                        selected = currentState == null,
                        onClick = { viewModel.onStateChanged(null) },
                        label = { Text("Toggle") },
                        leadingIcon = {
                            if (currentState == null) Icon(Icons.Default.SwapHoriz, null)
                        }
                    )


                    FilterChip(
                        selected = currentState == true,
                        onClick = { viewModel.onStateChanged(true) },
                        label = { Text("Grant") },
                        leadingIcon = {
                            if (currentState == true) Icon(Icons.Default.Check, null)
                        }
                    )


                    FilterChip(
                        selected = currentState == false,
                        onClick = { viewModel.onStateChanged(false) },
                        label = { Text("Revoke") },
                        leadingIcon = {
                            if (currentState == false) Icon(Icons.Default.Close, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppPackageInput(
    packageName: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = packageName,
            onValueChange = {},
            label = { Text("App Package") },
            placeholder = { Text("Tap to select app...") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.AppRegistration, contentDescription = "Select App")
                }
            }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDropdown(
    currentValue: String,
    options: List<AdbPermission>,
    onPermissionSelected: (AdbPermission) -> Unit,
    onTextChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current



    val filteredOptions by remember(currentValue, options) {
        derivedStateOf {
            if (currentValue.startsWith("%")) {
                emptyList()
            } else if (currentValue.isBlank()) {
                options
            } else {
                options.filter {
                    it.displayName.contains(currentValue, ignoreCase = true) ||
                            (it is AdbPermission.Single && it.permissionName.contains(currentValue, ignoreCase = true))
                }
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                onTextChange(newValue)

                expanded = !newValue.startsWith("%")
            },
            label = { Text("Permission") },
            placeholder = { Text("Select or type variable...") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = false,
            singleLine = true
        )


        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { permission ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = permission.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (permission is AdbPermission.Single) {
                                    Text(
                                        text = permission.permissionName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onPermissionSelected(permission)
                            expanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}