package com.easylocker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF6C8EF5)
val SoftGreen = Color(0xFF7CCBA2)
val SoftPurple = Color(0xFFC6B5F7)
val AppBackground = Color(0xFFF7F8FC)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)

private val EasyLockerColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SoftGreen,
    tertiary = SoftPurple,
    background = AppBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun EasyLockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EasyLockerColors,
        content = content
    )
}
