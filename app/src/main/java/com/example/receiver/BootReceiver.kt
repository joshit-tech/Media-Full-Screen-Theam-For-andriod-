package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.repository.MediaRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles ACTION_BOOT_COMPLETED to ensure user preferences are restored
 * seamlessly across device restarts.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Device reboot completed. Restoring user settings...")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = SettingsRepository.getInstance(context)
                val isAutoEnabled = settingsRepo.isAutoLockScreenEnabled.first()
                MediaRepository.setAutoModeEnabled(isAutoEnabled)
                Log.i(TAG, "Restored auto lockscreen mode: $isAutoEnabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring settings on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
