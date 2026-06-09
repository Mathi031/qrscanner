package dev.mathi031.qrscanner.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val SAVE_HISTORY = booleanPreferencesKey("save_history_enabled")
        val EXCLUDE_WIFI = booleanPreferencesKey("exclude_wifi_from_history")
    }

    val saveHistoryEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SAVE_HISTORY] ?: true }

    // Default true: las contraseñas Wi-Fi son sensibles, no se guardan salvo opt-in.
    val excludeWifiFromHistory: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.EXCLUDE_WIFI] ?: true }

    suspend fun setSaveHistoryEnabled(v: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_HISTORY] = v }
    }

    suspend fun setExcludeWifi(v: Boolean) {
        context.dataStore.edit { it[Keys.EXCLUDE_WIFI] = v }
    }
}
