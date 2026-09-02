package com.example.model

/**
 * Represents the current physical lock/screen condition of the device.
 */
enum class DeviceLockState {
    /** Screen is ON and user is unlocked / using device */
    UNLOCKED,
    /** Screen turned off */
    SCREEN_OFF,
    /** Screen turned on, but system keyguard is still engaged */
    SCREEN_ON_LOCKED
}

/**
 * State lifecycle tracking for the LockscreenActivity to prevent duplicate
 * or rapid cascading launches.
 */
enum class LaunchState {
    NOT_VISIBLE,
    LAUNCHING,
    VISIBLE,
    DISMISSED
}

/**
 * High-level system readiness status for auto-activation.
 */
enum class AutoActivationStatus {
    READY,
    WAITING_FOR_MEDIA,
    AUTO_MODE_DISABLED,
    PERMISSION_REQUIRED
}
