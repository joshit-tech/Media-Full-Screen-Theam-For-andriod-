package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaViewModel

/**
 * Modern progress scrubber bar with track elapsed and remaining time indicators.
 */
@Composable
fun TrackProgressBar(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0) return

    var isDragging by remember { mutableFloatStateOf(-1f) }
    val currentProgress = if (isDragging >= 0) isDragging else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentProgress,
            onValueChange = { newValue ->
                if (canSeek) {
                    isDragging = newValue
                }
            },
            onValueChangeFinished = {
                if (canSeek && isDragging >= 0) {
                    val targetMs = (isDragging * durationMs).toLong()
                    onSeek(targetMs)
                    isDragging = -1f
                }
            },
            enabled = canSeek,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPos = if (isDragging >= 0) (isDragging * durationMs).toLong() else positionMs
            Text(
                text = MediaViewModel.formatTime(displayPos),
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = MediaViewModel.formatTime(durationMs),
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
