package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet

/**
 * Full-screen edge-to-edge Album Artwork renderer featuring:
 * 1. Base full-screen artwork (Bitmap or Coil URI) with ContentScale.Crop
 * 2. Blurred atmospheric glow backdrop
 * 3. Gradient scrim for optimal foreground text and control readability
 * 4. Elegant animated fallback artwork when no media or cover is present
 */
@Composable
fun MediaArtwork(
    artworkBitmap: Bitmap?,
    artworkUri: String?,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    blurRadius: Int = 30,
    overlayAlpha: Float = 0.65f
) {
    FullScreenMediaArtwork(
        artworkBitmap = artworkBitmap,
        artworkUri = artworkUri,
        modifier = modifier,
        blurRadius = blurRadius,
        overlayAlpha = overlayAlpha
    )
}

@Composable
fun FullScreenMediaArtwork(
    artworkBitmap: Bitmap?,
    artworkUri: String?,
    modifier: Modifier = Modifier,
    blurRadius: Int = 30,
    overlayAlpha: Float = 0.65f
) {
    val context = LocalContext.current
    val hasArtwork = artworkBitmap != null || !artworkUri.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Crossfade(
            targetState = Pair(artworkBitmap, artworkUri),
            animationSpec = tween(durationMillis = 600),
            label = "ArtworkCrossfade"
        ) { (bitmap, uri) ->
            if (bitmap != null) {
                // Layer 1: Blurred background ambient glow
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.15f)
                        .blur(blurRadius.dp)
                        .alpha(0.85f)
                )

                // Layer 2: Sharp foreground art fill
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Album Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!uri.isNullOrBlank()) {
                // Layer 1: Blurred ambient glow from Coil
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.15f)
                        .blur(blurRadius.dp)
                        .alpha(0.85f)
                )

                // Layer 2: Sharp image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback: Elegant animated gradient canvas
                FallbackArtworkBackground()
            }
        }

        // Layer 3: Dynamic Multi-Stop Gradient Scrim
        // Ensures track metadata, clock, and glassmorphism controls are legible regardless of art luminosity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )
    }
}

@Composable
fun FallbackArtworkBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = 0.35f),
                        DarkSurface,
                        DarkBackground
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing aura
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            NeonViolet.copy(alpha = 0.4f),
                            NeonCyan.copy(alpha = 0.2f),
                            NeonViolet.copy(alpha = 0.4f)
                        )
                    )
                )
                .blur(40.dp)
        )

        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Default Music Icon",
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(96.dp)
        )
    }
}
