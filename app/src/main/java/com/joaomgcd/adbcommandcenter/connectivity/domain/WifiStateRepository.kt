package com.joaomgcd.adbcommandcenter.connectivity.domain

import kotlinx.coroutines.flow.Flow

interface WifiStateRepository {
    fun isWifiConnected(): Boolean
    val wifiConnectedFlow: Flow<Boolean>
}