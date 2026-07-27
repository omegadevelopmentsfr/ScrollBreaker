package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SageGreen40,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF422100),
    background = WarmSand,
    onBackground = Color(0xFF1B1D1B),
    surface = SurfaceLight,
    onSurface = Color(0xFF1B1D1B),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF454845)
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen80,
    onPrimary = Color(0xFF0F381F),
    primaryContainer = Color(0xFF1F442A),
    onPrimaryContainer = MintContainer,
    secondary = AmberAccent,
    onSecondary = Color(0xFF422100),
    background = WarmSandDark,
    onBackground = Color(0xFFE2E3E0),
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E3E0),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC3C8C2)
)

@Composable
fun ScrollBreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    ScrollBreakTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
