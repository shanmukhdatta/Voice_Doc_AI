package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DoctorBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DoctorBlueContainer,
    onPrimaryContainer = DoctorBlueDark,
    secondary = DoctorBlueLight,
    onSecondary = DoctorBlueDark,
    background = DoctorBackground,
    onBackground = DoctorTextPrimary,
    surface = DoctorSurface,
    onSurface = DoctorTextPrimary,
    surfaceVariant = DoctorSurfaceVariant,
    onSurfaceVariant = DoctorTextSecondary,
    outline = DoctorBorderSubtle
)

@Composable
fun VoiceDoctorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
