package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.service.MediaListenerService

object PermissionUtils {

    private const val TAG = "PermissionUtils"

    /**
     * Checks whether this app has been granted Notification Listener access.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(packageName)) {
            return true
        }

        // Secondary fallback check via Secure Settings string
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Opens the system settings screen where the user can grant Notification Listener access.
     * Implements fallback steps to support standard Android, Samsung, Pixel, and diverse OEM flavors.
     */
    fun openNotificationListenerSettings(context: Context) {
        val componentName = ComponentName(context, MediaListenerService::class.java)

        // Attempt 1: Android 11+ Direct detail settings if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (detailIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(detailIntent)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct detail settings failed, falling back to general list", e)
            }
        }

        // Attempt 2: General Notification Listener settings screen
        try {
            val listIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (listIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(listIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "General notification listener settings failed", e)
        }

        // Attempt 3: Fallback to General Settings
        try {
            val generalIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open any system settings page", e)
        }
    }

    /**
     * Attempts to request dismissal of the keyguard using the supported Android KeyguardManager API.
     */
    fun dismissKeyguard(
        activity: Activity,
        onDismissed: () -> Unit = {},
        onCancelled: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null) {
            onError()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(
                activity,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        activity.runOnUiThread {
                            onDismissed()
                        }
                    }

                    override fun onDismissCancelled() {
                        super.onDismissCancelled()
                        activity.runOnUiThread {
                            onCancelled()
                        }
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        activity.runOnUiThread {
                            onError()
                        }
                    }
                }
            )
        } else {
            // Fallback for older versions if keyguard is not locked or handled directly
            onDismissed()
        }
    }
}
