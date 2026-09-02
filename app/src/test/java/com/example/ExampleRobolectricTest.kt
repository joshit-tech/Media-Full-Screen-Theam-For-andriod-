package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.manager.LockScreenLaunchManager
import com.example.manager.LockScreenStateManager
import com.example.model.AutoActivationStatus
import com.example.model.DeviceLockState
import com.example.model.LaunchState
import com.example.model.MediaInfo
import com.example.repository.MediaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        MediaRepository.clearMediaInfo()
        MediaRepository.setAutoModeEnabled(false)
        MediaRepository.updateLockState(DeviceLockState.UNLOCKED)
        MediaRepository.updateLaunchState(LaunchState.NOT_VISIBLE)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("FullScreenLockPlayer", appName)
    }

    @Test
    fun `auto activation fails when auto mode is disabled`() {
        MediaRepository.setAutoModeEnabled(false)
        MediaRepository.updateMediaInfo(
            MediaInfo(
                title = "Song Title",
                artist = "Artist Name",
                isPlaying = true
            )
        )
        assertFalse(LockScreenStateManager.canAutoActivate(context))
        val status = LockScreenStateManager.getActivationStatus(context)
        // Without listener permission or auto-mode, status indicates action needed
        assertTrue(
            status == AutoActivationStatus.PERMISSION_REQUIRED ||
                    status == AutoActivationStatus.AUTO_MODE_DISABLED
        )
    }

    @Test
    fun `media repository state management and dismiss event`() {
        MediaRepository.updateLockState(DeviceLockState.SCREEN_OFF)
        assertEquals(DeviceLockState.SCREEN_OFF, MediaRepository.lockState.value)

        MediaRepository.updateLaunchState(LaunchState.LAUNCHING)
        assertEquals(LaunchState.LAUNCHING, MediaRepository.launchState.value)

        MediaRepository.updateLaunchState(LaunchState.VISIBLE)
        assertEquals(LaunchState.VISIBLE, MediaRepository.launchState.value)

        MediaRepository.requestDismissLockscreen()
        assertEquals(LaunchState.DISMISSED, MediaRepository.launchState.value)
    }

    @Test
    fun `launch manager skips duplicate launch if already visible`() {
        MediaRepository.updateLaunchState(LaunchState.VISIBLE)
        LockScreenLaunchManager.evaluateAutoLaunch(context, reason = "TEST")
        // Should remain VISIBLE and not crash or enter duplicate state
        assertEquals(LaunchState.VISIBLE, MediaRepository.launchState.value)
    }
}
