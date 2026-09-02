package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.FullScreenLockPlayerTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Result processed; notifications permission granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ for Full-Screen Intent notifications
        checkAndRequestNotificationPermission()

        setContent {
            FullScreenLockPlayerTheme {
                val isPermissionGranted by mainViewModel.isNotificationPermissionGranted.collectAsState()
                val activeSessionCount by mainViewModel.activeSessionCount.collectAsState()
                val isServiceConnected by mainViewModel.isServiceConnected.collectAsState()
                val mediaInfo by mainViewModel.mediaInfo.collectAsState()
                val isAutoLockScreenEnabled by mainViewModel.isAutoLockScreenEnabled.collectAsState()
                val isShowWhenPausedEnabled by mainViewModel.isShowWhenPausedEnabled.collectAsState()
                val activationStatus by mainViewModel.activationStatus.collectAsState()

                HomeScreen(
                    isPermissionGranted = isPermissionGranted,
                    activeSessionCount = activeSessionCount,
                    isServiceConnected = isServiceConnected,
                    mediaInfo = mediaInfo,
                    isAutoLockScreenEnabled = isAutoLockScreenEnabled,
                    isShowWhenPausedEnabled = isShowWhenPausedEnabled,
                    activationStatus = activationStatus,
                    onToggleAutoLockScreen = { mainViewModel.setAutoLockScreenEnabled(it) },
                    onToggleShowWhenPaused = { mainViewModel.setShowWhenPausedEnabled(it) },
                    onRequestPermission = { mainViewModel.openNotificationSettings() },
                    onPreviewLockScreen = {
                        val intent = Intent(this@MainActivity, LockscreenActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshPermissionStatus()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}
