package com.joaomgcd.adbcommandcenter.common.preferences.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.joaomgcd.adbcommandcenter.common.preferences.data.DataStorePreferencesRepository
import com.joaomgcd.adbcommandcenter.common.preferences.domain.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindingModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: DataStorePreferencesRepository
    ): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}