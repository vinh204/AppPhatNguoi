package com.tuhoc.phatnguoi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dùng các màu anh đã khai ở Colors.kt
// RedPrimary, RedAccent, TextPrimary, TextSub, WarningRed

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,

    secondary = RedAccent,
    onSecondary = Color.White,

    background = Color(0xFFF5F5F5),
    onBackground = TextPrimary,

    surface = Color.White,
    onSurface = TextPrimary,

    error = WarningRed,
    onError = Color.White
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,

    secondary = RedAccent,
    onSecondary = Color.White,

    background = Color(0xFF101010),
    onBackground = Color(0xFFE0E0E0),

    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFE0E0E0),

    error = WarningRed,
    onError = Color.White
)

/**
 * Theme chính của app – cái này MainActivity đang gọi.
 */
@Composable
fun PhatNguoiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // Nếu chưa custom typography & shapes thì để mặc định
        typography = androidx.compose.material3.Typography(),
        shapes = androidx.compose.material3.Shapes(),
        content = content
    )
}
