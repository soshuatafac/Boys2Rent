package com.huatafac.boys2rent.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = B2rBlueNight,
    onPrimary = B2rBlueDark,
    primaryContainer = B2rBlueDark,
    onPrimaryContainer = B2rBlueLight,
    secondary = B2rYellow,
    background = B2rSurfaceNight,
    surface = B2rSurfaceNight
)

private val LightColorScheme = lightColorScheme(
    primary = B2rBlue,
    onPrimary = Color.White,
    primaryContainer = B2rBlueLight,
    onPrimaryContainer = B2rBlueDark,
    secondary = B2rYellow,
    onSecondary = B2rInk,
    background = B2rBackground,
    onBackground = B2rInk,
    surface = Color.White,
    onSurface = B2rInk,
    onSurfaceVariant = B2rMuted,
    outlineVariant = B2rDivider
)

@Composable
fun Boys2RentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desligado para manter a identidade azul do Boys2Rent
    dynamicColor: Boolean = false,
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