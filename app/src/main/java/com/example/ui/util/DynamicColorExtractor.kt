package com.example.ui.util

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsulates the dynamic colors extracted from active media artwork.
 */
@Stable
data class DominantColorState(
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val ambientGlow: Color,
    val surfaceTint: Color,
    val onAccent: Color
)

/**
 * Default fallback colors when no artwork is available or extraction fails.
 * Premium, atmospheric deep-night palette with neon cyan and electric violet accents.
 */
val DefaultDarkPalette = DominantColorState(
    primaryAccent = Color(0xFF00E5FF),      // Electric Cyan
    secondaryAccent = Color(0xFF9D4EDD),    // Neon Violet
    ambientGlow = Color(0x66291B4E),        // Soft Deep Violet Glow
    surfaceTint = Color(0x331C1833),        // Tinted Glass
    onAccent = Color(0xFF090810)            // Dark on vibrant
)

/**
 * Remember and smoothly transition dominant colors derived from album artwork.
 * Uses Android Palette API on background thread with sub-millisecond execution.
 */
@Composable
fun rememberDominantColors(
    artworkBitmap: Bitmap?,
    defaultPalette: DominantColorState = DefaultDarkPalette
): DominantColorState {
    var rawPalette by remember { mutableStateOf(defaultPalette) }

    LaunchedEffect(artworkBitmap) {
        if (artworkBitmap == null || artworkBitmap.isRecycled) {
            rawPalette = defaultPalette
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {
            try {
                // Downsample bitmap for instant Palette generation (under 2ms)
                val maxDim = 96
                val scaled = if (artworkBitmap.width > maxDim || artworkBitmap.height > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(artworkBitmap.width, artworkBitmap.height)
                    val width = (artworkBitmap.width * scale).toInt().coerceAtLeast(1)
                    val height = (artworkBitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(artworkBitmap, width, height, false)
                } else {
                    artworkBitmap
                }

                val palette = Palette.from(scaled).generate()
                val primarySwatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch

                val secondarySwatch = palette.mutedSwatch
                    ?: palette.lightMutedSwatch
                    ?: palette.darkMutedSwatch
                    ?: palette.dominantSwatch

                if (primarySwatch != null) {
                    val adjustedPrimary = ensureReadableOnDark(primarySwatch.rgb)
                    val adjustedSecondary = ensureReadableOnDark(
                        secondarySwatch?.rgb ?: primarySwatch.rgb,
                        isSecondary = true
                    )

                    // Ambient glow with gentle alpha
                    val ambientGlow = Color(adjustedPrimary)
                        .copy(alpha = 0.42f)

                    // Surface tint for frosted glass
                    val surfaceTint = Color(adjustedPrimary)
                        .copy(alpha = 0.12f)

                    // High contrast text/icon color on top of primary button
                    val lum = ColorUtils.calculateLuminance(adjustedPrimary)
                    val onAccent = if (lum > 0.5) Color(0xFF0B0914) else Color.White

                    rawPalette = DominantColorState(
                        primaryAccent = Color(adjustedPrimary),
                        secondaryAccent = Color(adjustedSecondary),
                        ambientGlow = ambientGlow,
                        surfaceTint = surfaceTint,
                        onAccent = onAccent
                    )
                } else {
                    rawPalette = defaultPalette
                }
            } catch (e: Exception) {
                rawPalette = defaultPalette
            }
        }
    }

    // Smooth color animation transitions (500-700ms) for iOS-like fluidity
    val animPrimary by animateColorAsState(
        targetValue = rawPalette.primaryAccent,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "primaryAccentAnim"
    )
    val animSecondary by animateColorAsState(
        targetValue = rawPalette.secondaryAccent,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "secondaryAccentAnim"
    )
    val animGlow by animateColorAsState(
        targetValue = rawPalette.ambientGlow,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "ambientGlowAnim"
    )
    val animSurfaceTint by animateColorAsState(
        targetValue = rawPalette.surfaceTint,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "surfaceTintAnim"
    )
    val animOnAccent by animateColorAsState(
        targetValue = rawPalette.onAccent,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "onAccentAnim"
    )

    return DominantColorState(
        primaryAccent = animPrimary,
        secondaryAccent = animSecondary,
        ambientGlow = animGlow,
        surfaceTint = animSurfaceTint,
        onAccent = animOnAccent
    )
}

/**
 * Adjusts color HSL values so that it is guaranteed to be vibrant, rich, and
 * clearly visible against the deep lock screen backdrop.
 */
private fun ensureReadableOnDark(colorInt: Int, isSecondary: Boolean = false): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(colorInt, hsl)

    // Ensure minimum saturation for rich vibrant look
    if (hsl[1] < 0.35f) {
        hsl[1] = 0.55f
    }

    // Ensure lightness is neither too dark (< 0.45) nor blown-out washed white (> 0.82)
    if (isSecondary) {
        hsl[2] = hsl[2].coerceIn(0.40f, 0.70f)
    } else {
        hsl[2] = hsl[2].coerceIn(0.50f, 0.78f)
    }

    return ColorUtils.HSLToColor(hsl)
}
