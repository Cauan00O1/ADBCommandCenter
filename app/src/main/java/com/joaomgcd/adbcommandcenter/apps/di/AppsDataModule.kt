package com.joaomgcd.adbcommandcenter.apps.di

import com.joaomgcd.adbcommandcenter.apps.data.AppsRepositoryImpl
import com.joaomgcd.adbcommandcenter.apps.domain.AppsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsDataModule {

    @Binds
    @Singleton
    abstract fun bindAppsRepository(
        appsRepositoryImpl: AppsRepositoryImpl
    ): AppsRepository
}