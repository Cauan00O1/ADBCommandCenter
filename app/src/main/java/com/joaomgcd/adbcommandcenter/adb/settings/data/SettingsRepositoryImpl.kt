package com.joaomgcd.adbcommandcenter.adb.settings.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.joaomgcd.adbcommandcenter.adb.settings.domain.SettingsNamespace
import com.joaomgcd.adbcommandcenter.adb.settings.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {

    private val resolver = context.contentResolver

    override fun putInt(namespace: SettingsNamespace, key: String, value: Int): Boolean {
        val resolver = context.contentResolver
        return try {
            when (namespace) {
                SettingsNamespace.GLOBAL -> Settings.Global.putInt(resolver, key, value)
                SettingsNamespace.SECURE -> Settings.Secure.putInt(resolver, key, value)
                SettingsNamespace.SYSTEM -> Settings.System.putInt(resolver, key, value)
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun getInt(namespace: SettingsNamespace, key: String, default: Int): Int {
        val resolver = context.contentResolver
        return try {
            when (namespace) {
                SettingsNamespace.GLOBAL -> Settings.Global.getInt(resolver, key, default)
                SettingsNamespace.SECURE -> Settings.Secure.getInt(resolver, key, default)
                SettingsNamespace.SYSTEM -> Settings.System.getInt(resolver, key, default)
            }
        } catch (e: Exception) {
            default
        }
    }
    override fun observeInt(namespace: SettingsNamespace, key: String, default: Int): Flow<Int> = callbackFlow {
        val uri = when (namespace) {
            SettingsNamespace.GLOBAL -> Settings.Global.getUriFor(key)
            SettingsNamespace.SECURE -> Settings.Secure.getUriFor(key)
            SettingsNamespace.SYSTEM -> Settings.System.getUriFor(key)
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(getInt(namespace, key, default))
            }
        }


        trySend(getInt(namespace, key, default))

        resolver.registerContentObserver(uri, false, observer)

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged().conflate()
}