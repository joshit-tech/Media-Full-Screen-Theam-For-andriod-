package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium iOS-inspired Frosted Glass Surface.
 * Combines translucent surface tinting, fine-line specular top reflection,
 * and deep drop shadow for realistic glassmorphism.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    surfaceTint: Color = Color.Transparent,
    cornerRadius: Dp = 28.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val baseGlassColor = Color(0x38191629)
    val combinedGlassColor = if (surfaceTint != Color.Transparent) {
        // Blend surface tint into base glass
        Color(
            red = (baseGlassColor.red * 0.7f + surfaceTint.red * 0.3f),
            green = (baseGlassColor.green * 0.7f + surfaceTint.green * 0.3f),
            blue = (baseGlassColor.blue * 0.7f + surfaceTint.blue * 0.3f),
            alpha = 0.55f
        )
    } else {
        baseGlassColor
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.75f)
            )
            .clip(shape)
            .background(combinedGlassColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f), // Specular rim on top
                        Color.White.copy(alpha = 0.04f)  // Soft fade on bottom
                    )
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}
