package com.example.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.LockscreenActivity
import com.example.R
import com.example.model.DeviceLockState
import com.example.model.LaunchState
import com.example.repository.MediaRepository

/**
 * Centralized launch manager coordinating the automatic activation of LockscreenActivity.
 * Enforces lifecycle guards, prevents infinite launch loops, handles Android background
 * activity restrictions via supported Full-Screen Intent notifications, and manages dismissal.
 */
object LockScreenLaunchManager {

    private const val TAG = "LockScreenLaunchManager"
    const val CHANNEL_ID = "fullscreen_lock_channel"
    const val NOTIFICATION_ID = 2026

    private var lastLaunchAttemptTime: Long = 0L
    private const val LAUNCH_COOLDOWN_MS = 2500L
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Evaluates whether LockscreenActivity should be launched and triggers the
     * supported activation flow.
     */
    fun evaluateAutoLaunch(context: Context, reason: String) {
        val appContext = context.applicationContext
        mainHandler.post {
            val currentState = MediaRepository.launchState.value
            val now = System.currentTimeMillis()

            Log.d(TAG, "evaluateAutoLaunch: trigger=$reason, state=$currentState")

            // Guard 1: Prevent duplicate launches if already launching or visible
            if (currentState == LaunchState.LAUNCHING || currentState == LaunchState.VISIBLE) {
                Log.d(TAG, "Skipping launch: Activity is already $currentState")
                return@post
            }

            // Guard 2: Debounce cooldown to prevent rapid cascading triggers
            if (now - lastLaunchAttemptTime < LAUNCH_COOLDOWN_MS) {
                Log.d(TAG, "Skipping launch: cooldown active (${now - lastLaunchAttemptTime}ms)")
                return@post
            }

            // Guard 3: Core conditions check
            if (!LockScreenStateManager.canAutoActivate(appContext)) {
                Log.d(TAG, "Skipping launch: canAutoActivate is false")
                return@post
            }

            lastLaunchAttemptTime = now
            MediaRepository.updateLaunchState(LaunchState.LAUNCHING)

            Log.i(TAG, "Activating Lock Screen Media UI (trigger: $reason)...")
            triggerLaunch(appContext)
        }
    }

    /**
     * Executes the launch using a dual approach for maximum reliability:
     * 1. Direct Activity Start with FLAG_ACTIVITY_NEW_TASK
     * 2. Supported High-Priority Full-Screen Intent Notification for modern Android OS compliance
     */
    private fun triggerLaunch(context: Context) {
        val launchIntent = Intent(context, LockscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("auto_launched", true)
        }

        var directStartSucceeded = false
        try {
            context.startActivity(launchIntent)
            directStartSucceeded = true
            Log.d(TAG, "Direct startActivity succeeded")
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity failed or blocked by background restrictions", e)
        }

        // Supported Android Fallback/Companion: Full-Screen Intent Notification
        // On modern Android (API 29+), Full-Screen Intent is the official, platform-supported
        // mechanism to present an activity over the keyguard when the screen is locked/off.
        try {
            ensureNotificationChannel(context)

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                pendingIntentFlags
            )

            val mediaInfo = MediaRepository.mediaInfo.value
            val notificationTitle = mediaInfo.title.ifBlank { "Now Playing" }
            val notificationText = mediaInfo.artist.ifBlank { "Swipe up to unlock" }

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                Log.d(TAG, "Full-Screen Intent notification dispatched")
            } catch (se: SecurityException) {
                Log.w(TAG, "SecurityException posting full-screen notification", se)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch full-screen intent notification", e)
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock Screen Media Player",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automatically shows full-screen media player on lock screen"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Called by LockscreenActivity onResume to acknowledge successful presentation.
     */
    fun onActivityResumed(context: Context) {
        MediaRepository.updateLaunchState(LaunchState.VISIBLE)
        // Clean up transient launch notification once UI is visible
        dismissNotification(context)
    }

    /**
     * Called by LockscreenActivity onDestroy to reset state.
     */
    fun onActivityFinished() {
        if (MediaRepository.launchState.value != LaunchState.LAUNCHING) {
            MediaRepository.updateLaunchState(LaunchState.NOT_VISIBLE)
        }
    }

    /**
     * Called when the user has unlocked the device (e.g. ACTION_USER_PRESENT).
     */
    fun onUserPresent(context: Context) {
        Log.d(TAG, "User unlocked device (USER_PRESENT). Dismissing lock screen activity.")
        MediaRepository.updateLockState(DeviceLockState.UNLOCKED)
        MediaRepository.requestDismissLockscreen()
        dismissNotification(context)
        MediaRepository.updateLaunchState(LaunchState.NOT_VISIBLE)
    }

    /**
     * Dismisses the transient full-screen notification.
     */
    fun dismissNotification(context: Context) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification", e)
        }
    }
}
