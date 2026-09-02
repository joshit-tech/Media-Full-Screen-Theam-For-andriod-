package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Dynamic iOS-style album artwork presentation.
 * Features:
 *  - Spring scale physics on play/pause (1.0f playing vs 0.88f paused)
 *  - Interactive tap toggle between Hero and Compact sizing
 *  - Matching dynamic ambient glow shadow
 *  - Frosted inner border highlight
 */
@Composable
fun DynamicAlbumArtwork(
    artworkBitmap: Bitmap?,
    artworkUri: String?,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onArtworkClick: (() -> Unit)? = null
) {
    var isExpandedHero by remember { mutableStateOf(false) }

    // Apple Music scale response: 1.0f when playing, 0.88f when paused
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playPauseScale"
    )

    // Interactive Hero vs Normal max width
    val artworkMaxWidth by animateDpAsState(
        targetValue = if (isExpandedHero) 360.dp else 290.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "heroWidth"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "shadowElevation"
    )

    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = artworkMaxWidth)
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(playPauseScale)
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    ambientColor = accentColor.copy(alpha = 0.5f),
                    spotColor = accentColor.copy(alpha = 0.85f)
                )
                .clip(shape)
                .background(Color(0xFF14131D))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = shape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isExpandedHero = !isExpandedHero
                    onArtworkClick?.invoke()
                }
                .testTag("dynamic_album_artwork"),
            contentAlignment = Alignment.Center
        ) {
            when {
                artworkBitmap != null && !artworkBitmap.isRecycled -> {
                    Image(
                        bitmap = artworkBitmap.asImageBitmap(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                !artworkUri.isNullOrBlank() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri)
                            .crossfade(300)
                            .build(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    // Refined fallback icon with dark gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF231E34),
                                        Color(0xFF110F1B)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No Artwork",
                            tint = accentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }
            }
        }
    }
}
