package com.joaomgcd.adbcommandcenter.common.preferences.domain

import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject

class SetPreferenceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun <T> invoke(key: Preferences.Key<T>, value: T) {
        return preferencesRepository.setPreference(key, value)
    }
}