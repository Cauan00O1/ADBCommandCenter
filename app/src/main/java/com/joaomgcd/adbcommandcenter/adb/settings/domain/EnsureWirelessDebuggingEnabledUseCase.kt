package com.joaomgcd.adbcommandcenter.adb.settings.domain

import javax.inject.Inject

class EnsureWirelessDebuggingEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val KEY_ADB_WIFI_ENABLED = "adb_wifi_enabled"
        private const val VALUE_ENABLED = 1
    }

    operator fun invoke(): Result<Unit> {
        try {
            val currentValue = settingsRepository.getInt(
                SettingsNamespace.GLOBAL,
                KEY_ADB_WIFI_ENABLED,
                0
            )

            if (currentValue == VALUE_ENABLED) return Result.success(Unit)

            val success = settingsRepository.putInt(
                SettingsNamespace.GLOBAL,
                KEY_ADB_WIFI_ENABLED,
                VALUE_ENABLED
            )
            if (success) return Result.success(Unit)

            return Result.failure(Exception("Failed to write to Global Settings. Ensure WRITE_SECURE_SETTINGS is granted."))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}