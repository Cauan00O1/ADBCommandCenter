package com.joaomgcd.adbcommandcenter.main.ui

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joaomgcd.adbcommandcenter.adb.connection.domain.GetPairingStateUseCase
import com.joaomgcd.adbcommandcenter.common.domain.CheckNormalPermissionViaAndroidApiUseCase
import com.joaomgcd.adbcommandcenter.discovery.PairingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getPairingStateUseCase: GetPairingStateUseCase,
    private val checkPermissionUseCase: CheckNormalPermissionViaAndroidApiUseCase
) : ViewModel() {


    private val _hasNotificationPermission = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionUseCase(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    )
    val hasNotificationPermission = _hasNotificationPermission.asStateFlow()

    val isPaired: StateFlow<Boolean> = getPairingStateUseCase()
        .map { it is PairingState.Paired }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onPermissionResult(isGranted: Boolean) {
        _hasNotificationPermission.value = isGranted
    }
}