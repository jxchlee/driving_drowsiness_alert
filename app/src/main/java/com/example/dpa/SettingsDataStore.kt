package com.example.dpa

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore 인스턴스를 위한 확장 속성
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    // Preferences Keys 정의
    private object PreferencesKeys {
        val IS_SOUND_ENABLED = booleanPreferencesKey("is_sound_enabled")
        val VOLUME_LEVEL = floatPreferencesKey("volume_level")
        val IS_FLASHING_ENABLED = booleanPreferencesKey("is_flashing_enabled")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val VIBRATION_STRENGTH = intPreferencesKey("vibration_strength")
        val EAR_THRESHOLD = floatPreferencesKey("ear_threshold") // EAR 임계값
        val DROWSINESS_DURATION_THRESHOLD = longPreferencesKey("drowsiness_duration_threshold") // 졸음 지속 시간 임계값 (ms)
    }

    // AppSettings Flow
    val appSettingsFlow: Flow<AppSettings> = dataStore.data
        .map { preferences ->
            AppSettings(
                isSoundEnabled = preferences[PreferencesKeys.IS_SOUND_ENABLED] ?: true,
                volumeLevel = preferences[PreferencesKeys.VOLUME_LEVEL] ?: 0.7f,
                isFlashingEnabled = preferences[PreferencesKeys.IS_FLASHING_ENABLED] ?: true,
                isVibrationEnabled = preferences[PreferencesKeys.IS_VIBRATION_ENABLED] ?: true,
                vibrationStrength = preferences[PreferencesKeys.VIBRATION_STRENGTH] ?: 128,
                earThreshold = preferences[PreferencesKeys.EAR_THRESHOLD] ?: 0.2f, // 기본 EAR 임계값
                drowsinessDurationThreshold = preferences[PreferencesKeys.DROWSINESS_DURATION_THRESHOLD] ?: 2000L // 기본 졸음 지속 시간 (2초)
            )
        }

    // 저장 함수들
    suspend fun saveSoundEnabled(isEnabled: Boolean) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.IS_SOUND_ENABLED] = isEnabled
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update sound enabled setting: $e")
        }
    }

    suspend fun saveVolumeLevel(volume: Float) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.VOLUME_LEVEL] = volume
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update volume level setting: $e")
        }
    }

    suspend fun saveFlashingEnabled(isEnabled: Boolean) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.IS_FLASHING_ENABLED] = isEnabled
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update flashing enabled setting: $e")
        }
    }

    suspend fun saveVibrationEnabled(isEnabled: Boolean) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.IS_VIBRATION_ENABLED] = isEnabled
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update vibration enabled setting: $e")
        }
    }

    suspend fun saveVibrationStrength(strength: Int) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.VIBRATION_STRENGTH] = strength
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update vibration strength setting: $e")
        }
    }

    suspend fun saveEarThreshold(threshold: Float) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.EAR_THRESHOLD] = threshold
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update EAR threshold setting: $e")
        }
    }

    suspend fun saveDrowsinessDurationThreshold(duration: Long) {
        try {
            dataStore.updateData { settings ->
                settings.toMutablePreferences().apply {
                    this[PreferencesKeys.DROWSINESS_DURATION_THRESHOLD] = duration
                }
            }
        } catch (e: IOException) {
            Log.e("SettingsDataStore", "Failed to update drowsiness duration threshold setting: $e")
        }
    }
}