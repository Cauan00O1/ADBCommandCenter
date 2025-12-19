package com.joaomgcd.adbcommandcenter.adb.settings.domain

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun putInt(namespace: SettingsNamespace, key: String, value: Int): Boolean
    fun getInt(namespace: SettingsNamespace, key: String, default: Int): Int
    fun observeInt(namespace: SettingsNamespace, key: String, default: Int): Flow<Int>
}