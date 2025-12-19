package com.joaomgcd.adbcommandcenter.adb.common.di

import android.content.Context
import com.joaomgcd.adb.AdbConnectionManager
import com.joaomgcd.adbcommandcenter.adb.common.data.AdbRepositoryImpl
import com.joaomgcd.adbcommandcenter.adb.common.data.AdbServiceControllerImpl
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbRepository
import com.joaomgcd.adbcommandcenter.adb.common.domain.AdbServiceController
import com.joaomgcd.adbcommandcenter.adb.connection.data.AdbActiveConnectionConnectionNotificationRepositoryImpl
import com.joaomgcd.adbcommandcenter.adb.connection.data.AdbConnectionRepositoryImpl
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbActiveConnectionRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbConnectionNotificationRepository
import com.joaomgcd.adbcommandcenter.adb.connection.domain.AdbPairingRepository
import com.joaomgcd.adbcommandcenter.adb.settings.data.SettingsRepositoryImpl
import com.joaomgcd.adbcommandcenter.adb.settings.domain.SettingsRepository
import com.joaomgcd.adbcommandcenter.connectivity.data.WifiStateRepositoryImpl
import com.joaomgcd.adbcommandcenter.connectivity.domain.WifiStateRepository
import com.joaomgcd.adbcommandcenter.discovery.AdbPairingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val ADB_NAME = "AdbCommandCenter"

@Module
@InstallIn(SingletonComponent::class)
object AdbModule {

    @Provides
    @Singleton
    fun provideAdbConnectionManager(@ApplicationContext context: Context): AdbConnectionManager {
        return AdbConnectionManager(context, ADB_NAME)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdbBindingModule {

    @Binds
    @Singleton
    abstract fun bindAdbRepository(impl: AdbRepositoryImpl): AdbRepository

    @Binds
    @Singleton
    abstract fun bindAdbServiceController(impl: AdbServiceControllerImpl): AdbServiceController

    @Binds
    abstract fun bindAdbPairingRepository(adbPairingManager: AdbPairingRepositoryImpl): AdbPairingRepository

    @Binds
    abstract fun bindAdbActiveConnectionRepository(adbActiveConnectionRepository: AdbConnectionRepositoryImpl): AdbActiveConnectionRepository

    @Binds
    @Singleton
    abstract fun bindLocalSettingsRepository(repository: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWifiStateRepository(repository: WifiStateRepositoryImpl): WifiStateRepository

    @Binds
    @Singleton
    abstract fun bindAdbNotificationRepository(impl: AdbActiveConnectionConnectionNotificationRepositoryImpl): AdbConnectionNotificationRepository
}

