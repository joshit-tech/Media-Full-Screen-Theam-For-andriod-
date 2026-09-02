package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaInfo
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaViewModel

/**
 * Premium Glassmorphism Media Control Panel containing track info,
 * interactive seekbar progress, transport buttons, and source app badge.
 */
@Composable
fun PlaybackControlsPanel(
    mediaInfo: MediaInfo,
    currentPositionMs: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        backgroundColor = Color(0x38120F24),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Source app chip if available
            if (mediaInfo.appName.isNotBlank() || mediaInfo.packageName.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (mediaInfo.isPlaying) NeonCyan else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (mediaInfo.appName.ifBlank { mediaInfo.packageName.substringAfterLast('.') }).uppercase(),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Track Title
            Text(
                text = mediaInfo.title.ifBlank { "No Active Track" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("track_title_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Artist & Album
            val secondaryText = buildString {
                if (mediaInfo.artist.isNotBlank()) append(mediaInfo.artist)
                if (mediaInfo.album.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(mediaInfo.album)
                }
            }.ifBlank { "Play audio on Spotify, YouTube Music, etc." }

            Text(
                text = secondaryText,
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("track_artist_text")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            if (mediaInfo.durationMs > 0) {
                val progressFraction = if (isUserDragging) {
                    dragFraction
                } else {
                    (currentPositionMs.toFloat() / mediaInfo.durationMs.toFloat()).coerceIn(0f, 1f)
                }

                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        isUserDragging = true
                        dragFraction = fraction
                    },
                    onValueChangeFinished = {
                        val targetMs = (dragFraction * mediaInfo.durationMs).toLong()
                        onSeek(targetMs)
                        isUserDragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("playback_progress_slider")
                )

                val displayedCurrentMs = if (isUserDragging) {
                    (dragFraction * mediaInfo.durationMs).toLong()
                } else {
                    currentPositionMs
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = MediaViewModel.formatTime(displayedCurrentMs),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = MediaViewModel.formatTime(mediaInfo.durationMs),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport Control Buttons Row
            PlaybackControls(
                isPlaying = mediaInfo.isPlaying,
                isBuffering = mediaInfo.isBuffering,
                hasPrevious = mediaInfo.hasPrevious,
                hasNext = mediaInfo.hasNext,
                onPrevious = onSkipPrevious,
                onPlayPause = onPlayPause,
                onNext = onSkipNext
            )
        }
    }
}

/**
 * Premium glassmorphic playback transport controls bar featuring a central
 * luminous Hero Play/Pause button and sleek Previous/Next secondary controls.
 */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryControlButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous Track",
            enabled = hasPrevious,
            testTag = "previous_button",
            onClick = onPrevious
        )

        HeroPlayPauseButton(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onPlayPause = onPlayPause
        )

        SecondaryControlButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next Track",
            enabled = hasNext,
            testTag = "next_button",
            onClick = onNext
        )
    }
}

@Composable
private fun HeroPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(elevation = 12.dp, shape = CircleShape, spotColor = NeonCyan)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonCyan,
                        NeonViolet
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Button,
                onClick = onPlayPause
            )
            .testTag("play_pause_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "PlayPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
private fun SecondaryControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val alpha = if (enabled) 1.0f else 0.35f
    val bgAlpha = if (enabled) 0.30f else 0.12f

    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(GlassBorderBright.copy(alpha = alpha), GlassBorder.copy(alpha = alpha * 0.5f))
                ),
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(28.dp)
        )
    }
}
