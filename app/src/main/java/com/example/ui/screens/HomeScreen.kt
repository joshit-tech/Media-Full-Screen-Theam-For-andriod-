package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AutoActivationStatus
import com.example.model.MediaInfo
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

/**
 * Control Dashboard for FullScreenLockPlayer.
 * Provides system status, persistent auto-activation settings,
 * media diagnostics, and an optional preview launcher for testing.
 */
@Composable
fun HomeScreen(
    isPermissionGranted: Boolean,
    activeSessionCount: Int,
    isServiceConnected: Boolean,
    mediaInfo: MediaInfo,
    isAutoLockScreenEnabled: Boolean = false,
    isShowWhenPausedEnabled: Boolean = true,
    activationStatus: AutoActivationStatus = AutoActivationStatus.AUTO_MODE_DISABLED,
    onToggleAutoLockScreen: (Boolean) -> Unit = {},
    onToggleShowWhenPaused: (Boolean) -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onPreviewLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Brand Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonViolet, NeonCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "FullScreenLockPlayer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Automatic Lock Screen Media Experience",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // CARD 1: SYSTEM STATUS DASHBOARD
            // ==========================================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_dashboard_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "SYSTEM STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Notification Access
                    StatusRowItem(
                        icon = Icons.Rounded.NotificationsActive,
                        label = "Notification Access",
                        value = if (isPermissionGranted) "✓ Enabled" else "⚠ Action Required",
                        isSuccess = isPermissionGranted
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // 2. Auto Lock Screen
                    StatusRowItem(
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = "Auto Lock Screen",
                        value = if (isAutoLockScreenEnabled) "✓ Enabled" else "Disabled",
                        isSuccess = isAutoLockScreenEnabled
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // 3. Active Media
                    val mediaDisplay = when {
                        mediaInfo.hasActiveMedia && mediaInfo.appName.isNotBlank() -> mediaInfo.appName
                        mediaInfo.hasActiveMedia && mediaInfo.packageName.isNotBlank() -> mediaInfo.packageName.substringAfterLast('.')
                        mediaInfo.hasActiveMedia -> "Active Player"
                        else -> "None Detected"
                    }
                    StatusRowItem(
                        icon = Icons.Rounded.MusicNote,
                        label = "Active Media",
                        value = mediaDisplay,
                        isSuccess = mediaInfo.hasActiveMedia
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // 4. Lock Screen Status
                    val (statusText, statusColor) = when (activationStatus) {
                        AutoActivationStatus.READY -> "Ready (activates on lock)" to SuccessGreen
                        AutoActivationStatus.WAITING_FOR_MEDIA -> "Waiting for playback" to WarningAmber
                        AutoActivationStatus.AUTO_MODE_DISABLED -> "Auto Mode Disabled" to TextMuted
                        AutoActivationStatus.PERMISSION_REQUIRED -> "Action Required" to WarningAmber
                    }
                    StatusRowItem(
                        icon = Icons.Rounded.Lock,
                        label = "Lock Screen Status",
                        value = statusText,
                        overrideColor = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 2: AUTOMATIC LOCK SCREEN SETTINGS
            // ==========================================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_mode_settings_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "SETTINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonViolet,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Switch: Automatically Show Media Lock Screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Automatically Show Media Lock Screen",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The app automatically activates when compatible media is detected while the phone is locked.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }

                        Switch(
                            checked = isAutoLockScreenEnabled,
                            onCheckedChange = onToggleAutoLockScreen,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("auto_lock_screen_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Secondary Switch: Show When Paused
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Activate When Paused",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Display lock screen player for paused tracks with active metadata.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }

                        Switch(
                            checked = isShowWhenPausedEnabled,
                            onCheckedChange = onToggleShowWhenPaused,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonViolet,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("show_when_paused_switch")
                        )
                    }

                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_notification_access_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable Notification Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 3: ACTIVE MEDIA SESSION PREVIEW (IF ANY)
            // ==========================================
            if (mediaInfo.hasActiveMedia) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_media_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "ACTIVE MEDIA DETECTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33000000))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mediaInfo.artworkBitmap != null) {
                                Image(
                                    bitmap = mediaInfo.artworkBitmap.asImageBitmap(),
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else if (!mediaInfo.artworkUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = mediaInfo.artworkUri,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaInfo.title.ifBlank { "Untitled Track" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = mediaInfo.artist.ifBlank { "Unknown Artist" },
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (mediaInfo.isPlaying) NeonCyan else WarningAmber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (mediaInfo.isPlaying) "Playing" else "Paused",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (mediaInfo.isPlaying) NeonCyan else WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ==========================================
            // CARD 4: TESTING & PREVIEW (TEST ONLY)
            // ==========================================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_testing_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TESTING & PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "Test only",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "During normal daily phone usage, the lock screen activates automatically when you lock your phone while media is playing. Use the preview button below to test the UI layout.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onPreviewLockScreen,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_lock_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preview Lock Screen",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 5: HOW IT WORKS & ANDROID PLATFORM INFO
            // ==========================================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_info_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HOW AUTOMATIC ACTIVATION WORKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. Play audio in Spotify, YouTube Music, Apple Music, or any media app.\n" +
                                "2. Lock your phone with the power button.\n" +
                                "3. FullScreenLockPlayer automatically presents the full-bleed album art and glassmorphic player.\n" +
                                "4. Swipe up to unlock and return directly to your phone.\n\n" +
                                "Built using modern Android KeyguardManager, NotificationListenerService, and supported Full-Screen Intent notifications for battery-efficient event-driven activation.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    isSuccess: Boolean = false,
    overrideColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = overrideColor ?: if (isSuccess) SuccessGreen else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = overrideColor ?: if (isSuccess) SuccessGreen else WarningAmber
        )
    }
}
