package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.manager.LockScreenStateManager
import com.example.model.AutoActivationStatus
import com.example.model.DeviceLockState
import com.example.model.MediaInfo
import com.example.repository.MediaRepository
import com.example.repository.SettingsRepository
import com.example.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing dashboard diagnostics, user settings, permission flows,
 * and lock screen status monitoring for MainActivity.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)

    val mediaInfo: StateFlow<MediaInfo> = MediaRepository.mediaInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MediaInfo()
        )

    val activeSessionCount: StateFlow<Int> = MediaRepository.activeSessionCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val isServiceConnected: StateFlow<Boolean> = MediaRepository.isServiceConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val lockState: StateFlow<DeviceLockState> = MediaRepository.lockState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceLockState.UNLOCKED
        )

    val isAutoLockScreenEnabled: StateFlow<Boolean> = settingsRepository.isAutoLockScreenEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isShowWhenPausedEnabled: StateFlow<Boolean> = settingsRepository.isShowWhenPausedEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _isNotificationPermissionGranted = MutableStateFlow(false)
    val isNotificationPermissionGranted: StateFlow<Boolean> = _isNotificationPermissionGranted.asStateFlow()

    val activationStatus: StateFlow<AutoActivationStatus> = combine(
        isNotificationPermissionGranted,
        isAutoLockScreenEnabled,
        mediaInfo
    ) { hasPermission, isAuto, media ->
        when {
            !hasPermission -> AutoActivationStatus.PERMISSION_REQUIRED
            !isAuto -> AutoActivationStatus.AUTO_MODE_DISABLED
            !media.hasActiveMedia || media.title.isBlank() -> AutoActivationStatus.WAITING_FOR_MEDIA
            else -> AutoActivationStatus.READY
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutoActivationStatus.AUTO_MODE_DISABLED
    )

    init {
        refreshPermissionStatus()
        // Sync setting to repository
        viewModelScope.launch {
            isAutoLockScreenEnabled.collect { enabled ->
                MediaRepository.setAutoModeEnabled(enabled)
            }
        }
    }

    fun refreshPermissionStatus() {
        val granted = PermissionUtils.isNotificationListenerEnabled(getApplication())
        _isNotificationPermissionGranted.value = granted
    }

    fun setAutoLockScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoLockScreenEnabled(enabled)
            MediaRepository.setAutoModeEnabled(enabled)
        }
    }

    fun setShowWhenPausedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowWhenPausedEnabled(enabled)
        }
    }

    fun openNotificationSettings() {
        PermissionUtils.openNotificationListenerSettings(getApplication())
    }
}
