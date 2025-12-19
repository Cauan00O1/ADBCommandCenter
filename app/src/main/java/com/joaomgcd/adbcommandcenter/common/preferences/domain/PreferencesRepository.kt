package com.joaomgcd.adbcommandcenter.common.preferences.domain

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T>


    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T)
}