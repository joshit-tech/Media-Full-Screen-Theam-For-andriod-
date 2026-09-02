package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Modern iOS-inspired dynamic transport controls.
 * Implements: [Shuffle] [Previous] [Play/Pause] [Next] [Repeat]
 * With dynamic accent highlighting, real MediaSession capabilities, and 48dp touch targets.
 */
@Composable
fun DynamicPlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    isShuffleEnabled: Boolean,
    canShuffle: Boolean,
    repeatMode: Int, // 0: None, 1: One, 2: All
    canRepeat: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_playback_controls"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. Shuffle Button ---
        val shuffleAlpha = if (canShuffle) 1f else 0.25f
        val isShuffleActive = isShuffleEnabled && canShuffle
        val shuffleTint = if (isShuffleActive) accentColor else Color.White.copy(alpha = 0.5f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .alpha(shuffleAlpha)
                .clip(CircleShape)
                .clickable(
                    enabled = canShuffle,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp)
                ) { onToggleShuffle() }
                .testTag("control_shuffle")
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = if (isShuffleActive) "Shuffle On" else "Shuffle Off",
                tint = shuffleTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isShuffleActive) accentColor else Color.Transparent)
            )
        }

        // --- 2. Previous Track Button ---
        val prevAlpha = if (hasPrevious) 1f else 0.35f
        Box(
            modifier = Modifier
                .size(48.dp)
                .alpha(prevAlpha)
                .clip(CircleShape)
                .clickable(
                    enabled = hasPrevious,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp)
                ) { onPrevious() }
                .testTag("control_previous"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous Track",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // --- 3. Main Center Play/Pause Floating Button ---
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape, ambientColor = accentColor.copy(alpha = 0.4f), spotColor = accentColor)
                .clip(CircleShape)
                .background(accentColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 32.dp)
                ) { onPlayPause() }
                .testTag("control_play_pause"),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = onAccentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Crossfade(
                    targetState = isPlaying,
                    animationSpec = tween(180),
                    label = "playPauseCrossfade"
                ) { playing ->
                    if (playing) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Pause",
                            tint = onAccentColor,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = onAccentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // --- 4. Next Track Button ---
        val nextAlpha = if (hasNext) 1f else 0.35f
        Box(
            modifier = Modifier
                .size(48.dp)
                .alpha(nextAlpha)
                .clip(CircleShape)
                .clickable(
                    enabled = hasNext,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp)
                ) { onNext() }
                .testTag("control_next"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Track",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // --- 5. Repeat Button ---
        val repeatAlpha = if (canRepeat) 1f else 0.25f
        val isRepeatActive = repeatMode > 0 && canRepeat
        val repeatTint = if (isRepeatActive) accentColor else Color.White.copy(alpha = 0.5f)
        val repeatIcon = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .alpha(repeatAlpha)
                .clip(CircleShape)
                .clickable(
                    enabled = canRepeat,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp)
                ) { onCycleRepeat() }
                .testTag("control_repeat")
        ) {
            Icon(
                imageVector = repeatIcon,
                contentDescription = when (repeatMode) {
                    1 -> "Repeat One"
                    2 -> "Repeat All"
                    else -> "Repeat Off"
                },
                tint = repeatTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isRepeatActive) accentColor else Color.Transparent)
            )
        }
    }
}
