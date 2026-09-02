package com.example.manager

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import com.example.model.AutoActivationStatus
import com.example.model.DeviceLockState
import com.example.repository.MediaRepository
import com.example.util.PermissionUtils

/**
 * Manages and evaluates lock screen, keyguard, and auto-activation readiness conditions.
 */
object LockScreenStateManager {

    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    fun isScreenInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive ?: true
    }

    /**
     * Determines whether all conditions required for automatically displaying
     * the full screen lock media player are met.
     */
    fun canAutoActivate(context: Context): Boolean {
        // 1. User must have enabled Notification Listener Access
        if (!PermissionUtils.isNotificationListenerEnabled(context)) {
            return false
        }

        // 2. User must have turned ON Auto Lock Screen Media Mode
        if (!MediaRepository.isAutoModeEnabled.value) {
            return false
        }

        // 3. Valid MediaSession must exist with active title
        val mediaInfo = MediaRepository.mediaInfo.value
        if (!mediaInfo.hasActiveMedia || mediaInfo.title.isBlank()) {
            return false
        }

        // 4. Device must be locked or screen must be off
        val isLocked = isDeviceLocked(context)
        val lockState = MediaRepository.lockState.value
        val isOffOrLocked = isLocked || lockState == DeviceLockState.SCREEN_OFF || lockState == DeviceLockState.SCREEN_ON_LOCKED

        return isOffOrLocked
    }

    /**
     * Provides clear status description for the MainActivity dashboard.
     */
    fun getActivationStatus(context: Context): AutoActivationStatus {
        if (!PermissionUtils.isNotificationListenerEnabled(context)) {
            return AutoActivationStatus.PERMISSION_REQUIRED
        }
        if (!MediaRepository.isAutoModeEnabled.value) {
            return AutoActivationStatus.AUTO_MODE_DISABLED
        }
        val mediaInfo = MediaRepository.mediaInfo.value
        if (!mediaInfo.hasActiveMedia || mediaInfo.title.isBlank()) {
            return AutoActivationStatus.WAITING_FOR_MEDIA
        }
        return AutoActivationStatus.READY
    }
}
