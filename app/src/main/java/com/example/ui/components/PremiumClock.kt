package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium, iOS-inspired Lock Screen Clock & Status Header.
 * Features ultra-clean typography, live ticking clock, localized date, and device status.
 */
@Composable
fun PremiumClock(
    appName: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    val context = LocalContext.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val currentDate = remember(currentTimeMillis) { Date(currentTimeMillis) }

    // Format time: "9:41"
    val timeString = remember(currentTimeMillis) {
        DateFormat.getTimeFormat(context).format(currentDate)
    }

    // Format date: "Wednesday, September 2"
    val dateString = remember(currentTimeMillis) {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, MMMM d")
        SimpleDateFormat(pattern, Locale.getDefault()).format(currentDate)
    }

    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.45f),
        offset = Offset(0f, 2f),
        blurRadius = 8f
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Row: Lock pill and Battery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle lock badge / app source indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), shape = CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Device Locked",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
                if (appName.isNotBlank()) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = appName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Live Battery percentage indicator
            BatteryIndicator(
                textColor = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Date Display
        Text(
            text = dateString,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            style = TextStyle(shadow = textShadow)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Large Display Clock
        Text(
            text = timeString,
            color = textColor,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            style = TextStyle(shadow = textShadow)
        )
    }
}
