package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.manager.LockScreenLaunchManager
import com.example.repository.MediaRepository
import com.example.ui.screens.LockscreenScreen
import com.example.ui.theme.FullScreenLockPlayerTheme
import com.example.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Immersive, lock-screen-compatible Full-Screen Media Player Activity.
 * Enables Android lock screen overlay display using setShowWhenLocked and setTurnScreenOn.
 * Seamlessly integrates with LockScreenLaunchManager for lifecycle awareness, duplicate
 * prevention, and instant dismissal on device unlock (ACTION_USER_PRESENT).
 */
class LockscreenActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LockscreenActivity"

        fun createIntent(context: Context): Intent {
            return Intent(context, LockscreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }

    private val mediaViewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Configure Lock Screen Display compatibility
        setupLockScreenFlags()

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Observe dismiss signals from USER_PRESENT or session termination
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                MediaRepository.dismissLockscreenEvent.collect {
                    Log.d(TAG, "Received dismissLockscreenEvent. Finishing LockscreenActivity...")
                    finishAndFade()
                }
            }
        }

        // Auto-dismiss if media session completely disappears while locked (Scenario 6)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                MediaRepository.mediaInfo.collect { info ->
                    if (!info.hasActiveMedia) {
                        Log.d(TAG, "Active media session completely disappeared. Graceful exit in 2.5s...")
                        delay(2500L)
                        if (!MediaRepository.mediaInfo.value.hasActiveMedia) {
                            finishAndFade()
                        }
                    }
                }
            }
        }

        setContent {
            FullScreenLockPlayerTheme {
                val mediaInfo = mediaViewModel.mediaInfo.collectAsStateWithLifecycle().value
                val positionMs = mediaViewModel.interpolatedPositionMs.collectAsStateWithLifecycle().value

                LockscreenScreen(
                    mediaInfo = mediaInfo,
                    currentPositionMs = positionMs,
                    onPlayPause = { mediaViewModel.togglePlayPause() },
                    onSkipNext = { mediaViewModel.skipToNext() },
                    onSkipPrevious = { mediaViewModel.skipToPrevious() },
                    onSeek = { mediaViewModel.seekTo(it) },
                    onToggleShuffle = { mediaViewModel.toggleShuffle() },
                    onCycleRepeat = { mediaViewModel.cycleRepeatMode() },
                    onDismissScreen = {
                        finishAndFade()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: UI visible")
        LockScreenLaunchManager.onActivityResumed(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        LockScreenLaunchManager.onActivityFinished()
    }

    private fun finishAndFade() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Keep screen on while interacting with player if desired
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
