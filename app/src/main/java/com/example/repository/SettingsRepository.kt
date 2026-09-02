package com.example.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

/**
 * Repository for managing persistent user preferences via Jetpack DataStore.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_AUTO_LOCKSCREEN = booleanPreferencesKey("auto_lockscreen_enabled")
        private val KEY_SHOW_WHEN_PAUSED = booleanPreferencesKey("show_when_paused")
        private val KEY_FSI_FALLBACK = booleanPreferencesKey("use_fullscreen_intent_fallback")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Whether the app automatically activates the lock screen media player
     * when media is active and the device is locked.
     * Default: false (user must explicitly opt-in).
     */
    val isAutoLockScreenEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCKSCREEN] ?: false
    }

    /**
     * Whether the lock screen media player should also activate or stay visible
     * when playback is paused with valid metadata. Default: true.
     */
    val isShowWhenPausedEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_WHEN_PAUSED] ?: true
    }

    suspend fun setAutoLockScreenEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCKSCREEN] = enabled
        }
    }

    suspend fun setShowWhenPausedEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_WHEN_PAUSED] = enabled
        }
    }
}
