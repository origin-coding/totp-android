package io.github.origincoding.totp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface AppSettingsRepository {
    val appLockEnabled: Flow<Boolean>

    suspend fun setAppLockEnabled(enabled: Boolean)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val appLockEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[APP_LOCK_ENABLED] ?: false }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
