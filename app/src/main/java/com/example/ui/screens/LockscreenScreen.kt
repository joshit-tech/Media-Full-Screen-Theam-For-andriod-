package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MediaInfo
import com.example.ui.components.AmbientBackground
import com.example.ui.components.DynamicAlbumArtwork
import com.example.ui.components.DynamicPlaybackControls
import com.example.ui.components.GlassSurface
import com.example.ui.components.PremiumClock
import com.example.ui.components.PremiumProgressBar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.rememberDominantColors
import com.example.util.PermissionUtils
import com.example.viewmodel.MediaViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Premium iOS-inspired Full-Screen Music Lock Screen.
 * Implements:
 *  - Dynamic ambient lighting derived from album artwork via Palette API
 *  - Apple Music-style dynamic album art with spring scale animations
 *  - Frosted glassmorphic player card with dynamic surface tint
 *  - Minimalist iOS clock and live battery status
 *  - Rich transport controls with real Shuffle and Repeat MediaSession support
 *  - Smooth swipe-up-to-unlock gesture with spring physics
 */
@Composable
fun LockscreenScreen(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    mediaViewModel: MediaViewModel = viewModel()
) {
    val mediaInfo by mediaViewModel.mediaInfo.collectAsState()
    val interpolatedPos by mediaViewModel.interpolatedPositionMs.collectAsState()

    LockscreenScreen(
        mediaInfo = mediaInfo,
        currentPositionMs = interpolatedPos,
        onPlayPause = { mediaViewModel.togglePlayPause() },
        onSkipNext = { mediaViewModel.skipToNext() },
        onSkipPrevious = { mediaViewModel.skipToPrevious() },
        onSeek = { mediaViewModel.seekTo(it) },
        onToggleShuffle = { mediaViewModel.toggleShuffle() },
        onCycleRepeat = { mediaViewModel.cycleRepeatMode() },
        onDismissScreen = onDismissRequest,
        modifier = modifier
    )
}

@Composable
fun LockscreenScreen(
    mediaInfo: MediaInfo,
    currentPositionMs: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onDismissScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Dynamically extract colors from album artwork
    val dominantColors = rememberDominantColors(mediaInfo.artworkBitmap)

    // Swipe-to-unlock gesture physics state
    val offsetY = remember { Animatable(0f) }
    val dismissThreshold = -260f

    val dragModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            // Only allow dragging upwards (negative delta) with resistance
            val newOffset = (offsetY.value + delta).coerceAtMost(0f)
            scope.launch {
                offsetY.snapTo(newOffset)
            }
        },
        onDragStopped = { velocity ->
            if (offsetY.value < dismissThreshold || velocity < -1000f) {
                scope.launch {
                    offsetY.animateTo(
                        targetValue = -1200f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                    if (activity != null) {
                        PermissionUtils.dismissKeyguard(
                            activity = activity,
                            onDismissed = onDismissScreen,
                            onCancelled = {
                                scope.launch {
                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            },
                            onError = onDismissScreen
                        )
                    } else {
                        onDismissScreen()
                    }
                }
            } else {
                scope.launch {
                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    )

    val contentAlpha = (1f - ((-offsetY.value) / 380f)).coerceIn(0f, 1f)

    // Subtle pulsing arrow for swipe indicator
    val infiniteTransition = rememberInfiniteTransition(label = "swipeIndicator")
    val chevronOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevronAnim"
    )

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .then(dragModifier)
    ) {
        // --- 1. Dynamic Multi-Hue Ambient Backdrop ---
        AmbientBackground(
            dominantColors = dominantColors,
            isPlaying = mediaInfo.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Foreground UI with Inset Adaptation and Swipe-Up Physics ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .alpha(contentAlpha)
                .padding(
                    top = statusBarPadding.calculateTopPadding(),
                    bottom = navBarPadding.calculateBottomPadding()
                )
        ) {
            val availableHeight = maxHeight
            val isCompactHeight = availableHeight < 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- Top Section: iOS Clock & Battery Status ---
                PremiumClock(
                    appName = mediaInfo.appName,
                    textColor = Color.White,
                    modifier = Modifier.padding(top = if (isCompactHeight) 4.dp else 12.dp)
                )

                // --- Center Section: Dynamic Album Artwork ---
                if (mediaInfo.hasActiveMedia) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DynamicAlbumArtwork(
                            artworkBitmap = mediaInfo.artworkBitmap,
                            artworkUri = mediaInfo.artworkUri,
                            isPlaying = mediaInfo.isPlaying,
                            accentColor = dominantColors.primaryAccent,
                            modifier = Modifier.padding(vertical = if (isCompactHeight) 4.dp else 12.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // --- Bottom Section: Frosted Glass Player Panel & Unlock Pill ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mediaInfo.hasActiveMedia) {
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lockscreen_media_card"),
                            surfaceTint = dominantColors.surfaceTint,
                            cornerRadius = 28.dp,
                            contentPadding = if (isCompactHeight) 14.dp else 20.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Sound source indicator pill
                                if (mediaInfo.appName.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = dominantColors.primaryAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PLAYING FROM ${mediaInfo.appName.uppercase()}",
                                            color = dominantColors.primaryAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // Track Title
                                Text(
                                    text = mediaInfo.title.ifBlank { "Unknown Track" },
                                    color = TextPrimary,
                                    fontSize = if (isCompactHeight) 18.sp else 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Artist and Album subtitle
                                val artistAlbumText = buildString {
                                    append(mediaInfo.artist.ifBlank { "Unknown Artist" })
                                    if (mediaInfo.album.isNotBlank() && mediaInfo.album != mediaInfo.title) {
                                        append(" • ")
                                        append(mediaInfo.album)
                                    }
                                }
                                Text(
                                    text = artistAlbumText,
                                    color = TextSecondary,
                                    fontSize = if (isCompactHeight) 13.sp else 14.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Scrubber Progress Bar
                                if (mediaInfo.durationMs > 0) {
                                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 12.dp))
                                    PremiumProgressBar(
                                        positionMs = currentPositionMs,
                                        durationMs = mediaInfo.durationMs,
                                        canSeek = mediaInfo.canSeek,
                                        accentColor = dominantColors.primaryAccent,
                                        onSeek = onSeek
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

                                // Playback Transport Controls
                                DynamicPlaybackControls(
                                    isPlaying = mediaInfo.isPlaying,
                                    isBuffering = mediaInfo.isBuffering,
                                    hasPrevious = mediaInfo.hasPrevious,
                                    hasNext = mediaInfo.hasNext,
                                    isShuffleEnabled = mediaInfo.isShuffleEnabled,
                                    canShuffle = mediaInfo.canShuffle,
                                    repeatMode = mediaInfo.repeatMode,
                                    canRepeat = mediaInfo.canRepeat,
                                    accentColor = dominantColors.primaryAccent,
                                    onAccentColor = dominantColors.onAccent,
                                    onPrevious = onSkipPrevious,
                                    onPlayPause = onPlayPause,
                                    onNext = onSkipNext,
                                    onToggleShuffle = onToggleShuffle,
                                    onCycleRepeat = onCycleRepeat
                                )
                            }
                        }
                    } else {
                        // Standby Card when no media is playing
                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lockscreen_idle_card"),
                            surfaceTint = dominantColors.surfaceTint,
                            cornerRadius = 24.dp,
                            contentPadding = 18.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicOff,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No music playing",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Play media in Spotify or YouTube Music",
                                        color = TextMuted,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

                    // --- iOS-Style Swipe Up Indicator ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .testTag("swipe_unlock_indicator")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Swipe up to unlock",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .size(24.dp)
                                .offset(y = chevronOffset.dp)
                        )
                        Text(
                            text = "Swipe up to unlock",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // iOS Home bar indicator pill
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}
