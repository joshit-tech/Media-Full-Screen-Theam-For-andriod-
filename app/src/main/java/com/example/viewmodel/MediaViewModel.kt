package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.MediaInfo
import com.example.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel exposing the active media session state, live track position progress,
 * and user transport actions for Compose UI.
 */
class MediaViewModel : ViewModel() {

    val mediaInfo: StateFlow<MediaInfo> = MediaRepository.mediaInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MediaInfo()
        )

    private val _interpolatedPositionMs = MutableStateFlow(0L)
    val interpolatedPositionMs: StateFlow<Long> = _interpolatedPositionMs.asStateFlow()

    init {
        // Track live position ticker when playing
        viewModelScope.launch {
            while (isActive) {
                val current = MediaRepository.mediaInfo.value
                if (current.isPlaying && current.durationMs > 0) {
                    val elapsed = System.currentTimeMillis() - current.lastUpdatedTimestamp
                    val estimatedPos = (current.currentPositionMs + elapsed).coerceAtMost(current.durationMs)
                    _interpolatedPositionMs.value = estimatedPos
                } else {
                    _interpolatedPositionMs.value = current.currentPositionMs
                }
                delay(500L)
            }
        }
    }

    fun togglePlayPause() {
        MediaRepository.togglePlayPause()
    }

    fun play() {
        MediaRepository.play()
    }

    fun pause() {
        MediaRepository.pause()
    }

    fun skipToNext() {
        MediaRepository.skipToNext()
    }

    fun skipToPrevious() {
        MediaRepository.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        _interpolatedPositionMs.value = positionMs
        MediaRepository.seekTo(positionMs)
    }

    fun toggleShuffle() {
        MediaRepository.toggleShuffle()
    }

    fun cycleRepeatMode() {
        MediaRepository.cycleRepeatMode()
    }

    companion object {
        fun formatTime(millis: Long): String {
            if (millis <= 0) return "0:00"
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
