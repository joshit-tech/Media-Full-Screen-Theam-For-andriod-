package com.example.model

import android.graphics.Bitmap

/**
 * Immutable data model representing the currently active media playback state and metadata.
 */
data class MediaInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val artworkBitmap: Bitmap? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackState: Int = 0,
    val packageName: String = "",
    val appName: String = "",
    val hasPrevious: Boolean = true,
    val hasNext: Boolean = true,
    val canSeek: Boolean = true,
    val isShuffleEnabled: Boolean = false,
    val canShuffle: Boolean = false,
    val repeatMode: Int = 0, // 0: None, 1: One, 2: All
    val canRepeat: Boolean = false,
    val lastUpdatedTimestamp: Long = 0L
) {
    val hasActiveMedia: Boolean
        get() = title.isNotBlank() || artist.isNotBlank() || isPlaying || artworkBitmap != null || artworkUri != null
}
