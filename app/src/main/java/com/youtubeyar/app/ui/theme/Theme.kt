package com.youtubeyar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* -------------------------------- رنگ‌ها -------------------------------- */

val YoutubeRed = Color(0xFFE62C2C)
val YoutubeRedDark = Color(0xFFB71C1C)
val GrowthGreen = Color(0xFF7ED957)
val GoldAccent = Color(0xFFFFC107)

// تیره
private val DarkColors: ColorScheme = darkColorScheme(
    primary = YoutubeRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1414),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = GrowthGreen,
    onSecondary = Color(0xFF10290A),
    secondaryContainer = Color(0xFF1E3517),
    onSecondaryContainer = Color(0xFFCFF5C2),
    tertiary = GoldAccent,
    background = Color(0xFF0E0E11),
    onBackground = Color(0xFFECE6E6),
    surface = Color(0xFF17171B),
    onSurface = Color(0xFFECE6E6),
    surfaceVariant = Color(0xFF232329),
    onSurfaceVariant = Color(0xFFC9C2C2),
    outline = Color(0xFF4A4A52),
    error = Color(0xFFFF5449)
)

// روشن
private val LightColors: ColorScheme = lightColorScheme(
    primary = YoutubeRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF2E6D1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5C2),
    onSecondaryContainer = Color(0xFF0A2904),
    tertiary = Color(0xFF8A6A00),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF1D1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1D1B1B),
    surfaceVariant = Color(0xFFF2F2F4),
    onSurfaceVariant = Color(0xFF4A4A4F),
    outline = Color(0xFFB9B9BF),
    error = Color(0xFFBA1A1A)
)

/* ------------------------------- تایپوگرافی ------------------------------ */

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.5.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

/* --------------------------------- تم اپ -------------------------------- */

@Composable
fun YouTubeYarTheme(
    themePref: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
