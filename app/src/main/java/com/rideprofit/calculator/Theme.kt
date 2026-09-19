package com.rideprofit.calculator

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    onPrimary = Color.White,
    secondary = Color(0xFF4A7C8C),
    background = Color(0xFFF3F6F1),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2A24),
    onBackground = Color(0xFF1F2A24),
    error = Color(0xFFB3261E),
    surfaceVariant = Color(0xFFE4EBE3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FDFB5),
    onPrimary = Color(0xFF0C2B1C),
    secondary = Color(0xFF9BC9D8),
    background = Color(0xFF10161A),
    surface = Color(0xFF1A2227),
    onSurface = Color(0xFFDCE6E0),
    onBackground = Color(0xFFDCE6E0),
    error = Color(0xFFF2B8B5),
    surfaceVariant = Color(0xFF243036)
)

val LightGradient = Brush.verticalGradient(
    listOf(Color(0xFFE8F1E9), Color(0xFFDCE9E3), Color(0xFFE9E4D8))
)
val DarkGradient = Brush.verticalGradient(
    listOf(Color(0xFF0E1418), Color(0xFF131C20), Color(0xFF101A17))
)

@Composable
fun RideTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

@Composable
fun AppBackground(dark: Boolean, modifier: Modifier = Modifier) =
    modifier.background(if (dark) DarkGradient else LightGradient)
