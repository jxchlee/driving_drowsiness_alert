package com.example.dpa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

// 설정 항목들을 담을 데이터 클래스 (EAR 임계값 및 졸음 지속 시간 임계값 추가)
data class AppSettings(
    val isSoundEnabled: Boolean = true,
    val volumeLevel: Float = 0.7f,
    val isFlashingEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val vibrationStrength: Int = 128,
    val earThreshold: Float = 0.2f, // 새로 추가: EAR 임계값 (기본값 0.2f)
    val drowsinessDurationThreshold: Long = 2000L // 새로 추가: 졸음 지속 시간 임계값 (기본값 2000ms = 2초)
)

class SettingsViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.appSettingsFlow.collect { newSettings ->
                Log.d("SettingsViewModel", "Settings updated: $newSettings")
                _settings.value = newSettings
            }
        }
    }

    fun setSoundEnabled(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveSoundEnabled(isEnabled) }
    }

    fun setVolumeLevel(volume: Float) {
        viewModelScope.launch { settingsDataStore.saveVolumeLevel(volume) }
    }

    fun setFlashingEnabled(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveFlashingEnabled(isEnabled) }
    }

    fun setVibrationEnabled(isEnabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveVibrationEnabled(isEnabled) }
    }

    fun setVibrationStrength(strength: Int) {
        viewModelScope.launch { settingsDataStore.saveVibrationStrength(strength) }
    }

    // 새로 추가된 설정 업데이트 함수들
    fun setEarThreshold(threshold: Float) {
        viewModelScope.launch { settingsDataStore.saveEarThreshold(threshold) }
    }

    fun setDrowsinessDurationThreshold(duration: Long) {
        viewModelScope.launch { settingsDataStore.saveDrowsinessDurationThreshold(duration) }
    }
}