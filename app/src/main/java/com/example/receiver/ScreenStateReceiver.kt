package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.manager.LockScreenLaunchManager
import com.example.manager.LockScreenStateManager
import com.example.model.DeviceLockState
import com.example.repository.MediaRepository

/**
 * Dynamic broadcast receiver monitoring screen and device keyguard transitions:
 *  - ACTION_SCREEN_OFF: Phone screen turned off by user or timeout
 *  - ACTION_SCREEN_ON: Phone screen woke up while keyguard may be active
 *  - ACTION_USER_PRESENT: User authenticated / unlocked keyguard
 */
class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "ScreenStateReceiver onReceive: $action")

        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                MediaRepository.updateLockState(DeviceLockState.SCREEN_OFF)
                // Evaluate auto-launch as soon as the screen turns off
                LockScreenLaunchManager.evaluateAutoLaunch(context, reason = "ACTION_SCREEN_OFF")
            }

            Intent.ACTION_SCREEN_ON -> {
                val isLocked = LockScreenStateManager.isDeviceLocked(context)
                if (isLocked) {
                    MediaRepository.updateLockState(DeviceLockState.SCREEN_ON_LOCKED)
                    LockScreenLaunchManager.evaluateAutoLaunch(context, reason = "ACTION_SCREEN_ON")
                } else {
                    MediaRepository.updateLockState(DeviceLockState.UNLOCKED)
                }
            }

            Intent.ACTION_USER_PRESENT -> {
                // User has successfully unlocked the phone -> dismiss custom lockscreen activity
                MediaRepository.updateLockState(DeviceLockState.UNLOCKED)
                LockScreenLaunchManager.onUserPresent(context)
            }
        }
    }
}
