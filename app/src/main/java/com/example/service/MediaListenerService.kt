package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.manager.LockScreenLaunchManager
import com.example.manager.LockScreenStateManager
import com.example.model.MediaInfo
import com.example.receiver.ScreenStateReceiver
import com.example.repository.MediaRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android NotificationListenerService implementation that leverages MediaSessionManager
 * to automatically detect, monitor, and control media sessions (e.g. Spotify, YouTube Music,
 * Apple Music, VLC, etc.) across the system.
 *
 * Hosts the dynamic ScreenStateReceiver for lock events and coordinates automatic
 * lock-screen activation when phone locking or media playback occurs.
 */
class MediaListenerService : NotificationListenerService(), MediaRepository.MediaCommandDelegate {

    companion object {
        private const val TAG = "MediaListenerService"
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private var componentName: ComponentName? = null
    private var currentController: MediaController? = null
    private var compatController: MediaControllerCompat? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var isReceiverRegistered = false

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active sessions changed. Found ${controllers?.size ?: 0} sessions")
        mainHandler.post {
            handleSessionsChanged(controllers)
        }
    }

    private val compatCallback = object : MediaControllerCompat.Callback() {
        override fun onShuffleModeChanged(shuffleMode: Int) {
            Log.d(TAG, "Compat shuffle mode changed: $shuffleMode")
            mainHandler.post {
                updateMediaFromCurrentController()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.d(TAG, "Compat repeat mode changed: $repeatMode")
            mainHandler.post {
                updateMediaFromCurrentController()
            }
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "Media metadata changed")
            mainHandler.post {
                updateMediaFromCurrentController()
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed: ${state?.state}")
            mainHandler.post {
                val stateCode = state?.state
                if (stateCode == PlaybackState.STATE_STOPPED || stateCode == PlaybackState.STATE_NONE) {
                    refreshSessions()
                } else {
                    updateMediaFromCurrentController()
                    // If media just began playing while device is already locked/screen off, trigger auto-activation
                    if (stateCode == PlaybackState.STATE_PLAYING) {
                        if (LockScreenStateManager.isDeviceLocked(this@MediaListenerService) ||
                            !LockScreenStateManager.isScreenInteractive(this@MediaListenerService)
                        ) {
                            LockScreenLaunchManager.evaluateAutoLaunch(
                                this@MediaListenerService,
                                reason = "PLAYBACK_STATE_PLAYING_WHILE_LOCKED"
                            )
                        }
                    }
                }
            }
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "Current media session was destroyed")
            mainHandler.post {
                currentController = null
                refreshSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaListenerService created")
        componentName = ComponentName(this, MediaListenerService::class.java)
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        MediaRepository.setCommandDelegate(this)

        // Register dynamic screen state receiver
        registerScreenStateReceiver()

        // Sync settings from DataStore
        serviceScope.launch {
            val settingsRepo = SettingsRepository.getInstance(applicationContext)
            settingsRepo.isAutoLockScreenEnabled.collect { enabled ->
                Log.d(TAG, "Auto LockScreen mode preference updated: $enabled")
                MediaRepository.setAutoModeEnabled(enabled)
            }
        }
    }

    private fun registerScreenStateReceiver() {
        if (isReceiverRegistered) return
        try {
            screenStateReceiver = ScreenStateReceiver()
            val filter = ScreenStateReceiver.createIntentFilter()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this,
                    screenStateReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(screenStateReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "ScreenStateReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register ScreenStateReceiver", e)
        }
    }

    private fun unregisterScreenStateReceiver() {
        if (!isReceiverRegistered) return
        try {
            screenStateReceiver?.let { unregisterReceiver(it) }
            screenStateReceiver = null
            isReceiverRegistered = false
            Log.d(TAG, "ScreenStateReceiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister ScreenStateReceiver", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
        MediaRepository.setServiceConnected(true)
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
            refreshSessions()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering active sessions listener", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing active sessions", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
        cleanupController()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove active sessions listener", e)
        }
        MediaRepository.setServiceConnected(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MediaListenerService destroyed")
        MediaRepository.setCommandDelegate(null)
        cleanupController()
        unregisterScreenStateReceiver()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove active sessions listener on destroy", e)
        }
        MediaRepository.setServiceConnected(false)
        serviceScope.cancel()
    }

    private fun refreshSessions() {
        try {
            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            handleSessionsChanged(sessions)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting active sessions", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active sessions", e)
        }
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        val sessionList = controllers ?: emptyList()
        MediaRepository.setActiveSessionCount(sessionList.size)

        if (sessionList.isEmpty()) {
            cleanupController()
            MediaRepository.clearMediaInfo()
            return
        }

        val bestController = selectBestController(sessionList)

        if (bestController == null) {
            cleanupController()
            MediaRepository.clearMediaInfo()
            return
        }

        val isDifferentController = currentController?.sessionToken != bestController.sessionToken

        if (isDifferentController) {
            cleanupController()
            currentController = bestController
            try {
                bestController.registerCallback(controllerCallback, mainHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register controller callback", e)
            }
            try {
                val tokenCompat = MediaSessionCompat.Token.fromToken(bestController.sessionToken)
                compatController = MediaControllerCompat(this, tokenCompat).apply {
                    registerCallback(compatCallback, mainHandler)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register compatController", e)
            }
        }

        updateMediaFromCurrentController()
    }

    private fun selectBestController(controllers: List<MediaController>): MediaController? {
        if (controllers.isEmpty()) return null

        // 1. Is there a controller currently playing?
        val playingController = controllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING
        }
        if (playingController != null) return playingController

        // 2. Is there a buffering/connecting session?
        val bufferingController = controllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_BUFFERING ||
                    state == PlaybackState.STATE_CONNECTING ||
                    state == PlaybackState.STATE_FAST_FORWARDING ||
                    state == PlaybackState.STATE_REWINDING
        }
        if (bufferingController != null) return bufferingController

        // 3. Keep current paused controller to prevent flicker
        if (currentController != null) {
            val matching = controllers.firstOrNull { it.sessionToken == currentController?.sessionToken }
            if (matching != null && matching.playbackState?.state == PlaybackState.STATE_PAUSED) {
                return matching
            }
        }

        // 4. Any paused session with metadata
        val pausedController = controllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            val hasMeta = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotBlank() == true
            state == PlaybackState.STATE_PAUSED && hasMeta
        }
        if (pausedController != null) return pausedController

        // 5. Any session with a title
        val anyWithTitle = controllers.firstOrNull { controller ->
            controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotBlank() == true
        }

        return anyWithTitle ?: controllers.firstOrNull()
    }

    private fun updateMediaFromCurrentController() {
        val controller = currentController
        if (controller == null) {
            MediaRepository.clearMediaInfo()
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_COMPOSER)
            ?: ""

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L

        var artworkBitmap: Bitmap? = null
        var artworkUri: String? = null

        try {
            artworkBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

            if (artworkBitmap == null) {
                artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting artwork from metadata", e)
        }

        val stateCode = playbackState?.state ?: PlaybackState.STATE_NONE
        val isPlaying = stateCode == PlaybackState.STATE_PLAYING
        val isBuffering = stateCode == PlaybackState.STATE_BUFFERING || stateCode == PlaybackState.STATE_CONNECTING

        val actions = playbackState?.actions ?: 0L
        val hasPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) ||
                (actions and PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM != 0L) ||
                actions == 0L

        val hasNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) ||
                (actions and PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM != 0L) ||
                actions == 0L

        val canSeek = (actions and PlaybackState.ACTION_SEEK_TO != 0L) || duration > 0

        // Extract Shuffle state and support via MediaControllerCompat
        val rawShuffleMode = try {
            compatController?.shuffleMode ?: PlaybackStateCompat.SHUFFLE_MODE_NONE
        } catch (e: Exception) {
            PlaybackStateCompat.SHUFFLE_MODE_NONE
        }
        val isShuffleEnabled = rawShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                rawShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP
        val canShuffle = compatController != null &&
                rawShuffleMode != PlaybackStateCompat.SHUFFLE_MODE_INVALID

        // Extract Repeat state and support via MediaControllerCompat
        val rawRepeatMode = try {
            compatController?.repeatMode ?: PlaybackStateCompat.REPEAT_MODE_NONE
        } catch (e: Exception) {
            PlaybackStateCompat.REPEAT_MODE_NONE
        }
        val normalizedRepeatMode = when (rawRepeatMode) {
            PlaybackStateCompat.REPEAT_MODE_ONE -> 1
            PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_GROUP -> 2
            else -> 0
        }
        val canRepeat = compatController != null &&
                rawRepeatMode != PlaybackStateCompat.REPEAT_MODE_INVALID

        val pkgName = controller.packageName ?: ""
        val appName = getAppLabel(pkgName)

        val mediaInfo = MediaInfo(
            title = title,
            artist = artist,
            album = album,
            durationMs = if (duration > 0) duration else 0L,
            currentPositionMs = if (position > 0) position else 0L,
            artworkBitmap = artworkBitmap,
            artworkUri = artworkUri,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            playbackState = stateCode,
            packageName = pkgName,
            appName = appName,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            canSeek = canSeek,
            isShuffleEnabled = isShuffleEnabled,
            canShuffle = canShuffle,
            repeatMode = normalizedRepeatMode,
            canRepeat = canRepeat,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        MediaRepository.updateMediaInfo(mediaInfo)
    }

    private fun getAppLabel(packageName: String): String {
        if (packageName.isBlank()) return ""
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        } catch (e: Exception) {
            ""
        }
    }

    private fun cleanupController() {
        try {
            currentController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering controller callback", e)
        }
        try {
            compatController?.unregisterCallback(compatCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering compat callback", e)
        }
        currentController = null
        compatController = null
    }

    // --- MediaCommandDelegate implementations ---

    override fun play() {
        mainHandler.post {
            try {
                currentController?.transportControls?.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send play command", e)
            }
        }
    }

    override fun pause() {
        mainHandler.post {
            try {
                currentController?.transportControls?.pause()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send pause command", e)
            }
        }
    }

    override fun skipToNext() {
        mainHandler.post {
            try {
                currentController?.transportControls?.skipToNext()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send skipToNext command", e)
            }
        }
    }

    override fun skipToPrevious() {
        mainHandler.post {
            try {
                currentController?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send skipToPrevious command", e)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        mainHandler.post {
            try {
                currentController?.transportControls?.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send seekTo command", e)
            }
        }
    }

    override fun toggleShuffle() {
        mainHandler.post {
            try {
                val compat = compatController ?: return@post
                val currentShuffle = try {
                    compat.shuffleMode
                } catch (e: Exception) {
                    PlaybackStateCompat.SHUFFLE_MODE_NONE
                }
                val targetMode = if (currentShuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL ||
                    currentShuffle == PlaybackStateCompat.SHUFFLE_MODE_GROUP
                ) {
                    PlaybackStateCompat.SHUFFLE_MODE_NONE
                } else {
                    PlaybackStateCompat.SHUFFLE_MODE_ALL
                }
                compat.transportControls?.setShuffleMode(targetMode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle shuffle mode", e)
            }
        }
    }

    override fun cycleRepeatMode() {
        mainHandler.post {
            try {
                val compat = compatController ?: return@post
                val currentRepeat = try {
                    compat.repeatMode
                } catch (e: Exception) {
                    PlaybackStateCompat.REPEAT_MODE_NONE
                }
                val targetMode = when (currentRepeat) {
                    PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
                    PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_GROUP -> PlaybackStateCompat.REPEAT_MODE_ONE
                    PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_NONE
                    else -> PlaybackStateCompat.REPEAT_MODE_NONE
                }
                compat.transportControls?.setRepeatMode(targetMode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cycle repeat mode", e)
            }
        }
    }
}
