package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Premium iOS-style scrubber bar with interactive thumb, remaining time display,
 * and dynamic accent styling.
 */
@Composable
fun PremiumProgressBar(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualFraction = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragFraction else actualFraction

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 10.dp,
        label = "thumbSize"
    )

    val currentDisplayMs = if (isDragging) {
        (dragFraction * safeDuration).toLong()
    } else {
        positionMs
    }

    val remainingMs = (safeDuration - currentDisplayMs).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("premium_progress_bar")
    ) {
        // Scrubber Track & Thumb Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(canSeek, safeDuration) {
                    if (!canSeek) return@pointerInput
                    detectTapGestures { offset ->
                        val targetFrac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        onSeek((targetFrac * safeDuration).toLong())
                    }
                }
                .pointerInput(canSeek, safeDuration) {
                    if (!canSeek) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragFraction * safeDuration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Inactive track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            )

            // Active progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayFraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )

            // Circular Scrubber Thumb
            if (canSeek) {
                Box(
                    modifier = Modifier
                        .offset {
                            val thumbX = (displayFraction * trackWidthPx) - (thumbSize.toPx() / 2f)
                            IntOffset(thumbX.roundToInt(), 0)
                        }
                        .size(thumbSize)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Time labels: [Elapsed] on left, [-Remaining] on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(currentDisplayMs),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )

            Text(
                text = "-${formatDuration(remainingMs)}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
