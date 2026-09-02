package com.example.repository

import com.example.model.DeviceLockState
import com.example.model.LaunchState
import com.example.model.MediaInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central repository serving as the single source of truth for:
 *  - Current Media State & metadata
 *  - Device Lock state (Unlocked, Screen Off, Screen On Locked)
 *  - Auto Lock Screen Mode enabled preference
 *  - Lockscreen UI Launch and Visibility lifecycle state
 *  - Transport control delegation
 */
object MediaRepository {

    // --- Media Info ---
    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo: StateFlow<MediaInfo> = _mediaInfo.asStateFlow()

    private val _activeSessionCount = MutableStateFlow(0)
    val activeSessionCount: StateFlow<Int> = _activeSessionCount.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    // --- Lock State & Launch State ---
    private val _lockState = MutableStateFlow(DeviceLockState.UNLOCKED)
    val lockState: StateFlow<DeviceLockState> = _lockState.asStateFlow()

    private val _launchState = MutableStateFlow(LaunchState.NOT_VISIBLE)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    private val _isAutoModeEnabled = MutableStateFlow(false)
    val isAutoModeEnabled: StateFlow<Boolean> = _isAutoModeEnabled.asStateFlow()

    // Event emitted when LockscreenActivity should immediately finish (e.g. user unlocked)
    private val _dismissLockscreenEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dismissLockscreenEvent: SharedFlow<Unit> = _dismissLockscreenEvent.asSharedFlow()

    // Command delegate supplied by the active MediaListenerService
    interface MediaCommandDelegate {
        fun play()
        fun pause()
        fun skipToNext()
        fun skipToPrevious()
        fun seekTo(positionMs: Long)
        fun toggleShuffle()
        fun cycleRepeatMode()
    }

    private var commandDelegate: MediaCommandDelegate? = null

    fun setCommandDelegate(delegate: MediaCommandDelegate?) {
        commandDelegate = delegate
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
        if (!connected) {
            _activeSessionCount.value = 0
            _mediaInfo.value = MediaInfo()
        }
    }

    fun setActiveSessionCount(count: Int) {
        _activeSessionCount.value = count
    }

    fun updateMediaInfo(newInfo: MediaInfo) {
        _mediaInfo.value = newInfo
    }

    fun clearMediaInfo() {
        _mediaInfo.value = MediaInfo()
    }

    fun updateLockState(state: DeviceLockState) {
        _lockState.value = state
    }

    fun updateLaunchState(state: LaunchState) {
        _launchState.value = state
    }

    fun setAutoModeEnabled(enabled: Boolean) {
        _isAutoModeEnabled.value = enabled
    }

    fun requestDismissLockscreen() {
        _launchState.value = LaunchState.DISMISSED
        _dismissLockscreenEvent.tryEmit(Unit)
    }

    fun play() {
        commandDelegate?.play()
    }

    fun pause() {
        commandDelegate?.pause()
    }

    fun togglePlayPause() {
        if (_mediaInfo.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        commandDelegate?.skipToNext()
    }

    fun skipToPrevious() {
        commandDelegate?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        commandDelegate?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        commandDelegate?.toggleShuffle()
    }

    fun cycleRepeatMode() {
        commandDelegate?.cycleRepeatMode()
    }
}
