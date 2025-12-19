package com.joaomgcd.adbcommandcenter.common.preferences.domain

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferenceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun <T> invoke(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return preferencesRepository.getPreference(key, defaultValue)
    }
}