package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.util.DominantColorState

/**
 * Multi-layer dynamic ambient backdrop inspired by iOS lock screen & Apple Music.
 * Renders atmospheric radial glows keyed to dominant track colors with subtle breathing motion.
 */
@Composable
fun AmbientBackground(
    dominantColors: DominantColorState,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Subtle breathing pulse for ambient glows when media is actively playing
    val infiniteTransition = rememberInfiniteTransition(label = "ambientPulse")
    val rawPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlphaAnim"
    )

    val rawPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScaleAnim"
    )

    // Smoothly transition pulse activity when playback pauses/resumes
    val activeAlphaFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.45f,
        animationSpec = tween(durationMillis = 800),
        label = "playbackActiveAlpha"
    )

    val currentPulseAlpha = (rawPulseAlpha * activeAlphaFactor).coerceIn(0.15f, 0.70f)
    val currentPulseScale = if (isPlaying) rawPulseScale else 1.0f

    val baseDarkBackground = Color(0xFF07070B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseDarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height * 0.42f // Centered slightly above midpoint behind album art

            val primaryGlowColor = dominantColors.primaryAccent.copy(alpha = currentPulseAlpha)
            val secondaryGlowColor = dominantColors.secondaryAccent.copy(alpha = currentPulseAlpha * 0.75f)

            // Primary radial glow behind artwork
            val primaryRadius = (width * 0.75f * currentPulseScale).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor,
                        primaryGlowColor.copy(alpha = primaryGlowColor.alpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = primaryRadius
                ),
                center = Offset(centerX, centerY),
                radius = primaryRadius
            )

            // Secondary offset glow creating rich multi-hue chromatic depth
            val secondaryCenter = Offset(centerX + width * 0.22f, centerY + height * 0.08f)
            val secondaryRadius = (width * 0.65f).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryGlowColor,
                        secondaryGlowColor.copy(alpha = secondaryGlowColor.alpha * 0.35f),
                        Color.Transparent
                    ),
                    center = secondaryCenter,
                    radius = secondaryRadius
                ),
                center = secondaryCenter,
                radius = secondaryRadius
            )

            // Top vignette for clock legibility
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseDarkBackground.copy(alpha = 0.82f),
                        baseDarkBackground.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height * 0.28f
                )
            )

            // Bottom vignette for glass controls legibility
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        baseDarkBackground.copy(alpha = 0.65f),
                        baseDarkBackground.copy(alpha = 0.95f)
                    ),
                    startY = height * 0.62f,
                    endY = height
                )
            )
        }
    }
}
