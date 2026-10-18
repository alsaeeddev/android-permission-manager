package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "permora_settings")

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_SCAN_FREQ = intPreferencesKey("scan_frequency_hours")
    }

    override val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode
        }
    }

    override val scanFrequencyHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCAN_FREQ] ?: 24
    }

    override suspend fun setScanFrequencyHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCAN_FREQ] = hours
        }
    }
}
