package com.whisper.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.whisper.android.R

val playfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display_regular, weight = FontWeight.Normal),
    Font(R.font.playfair_display_bold, weight = FontWeight.Bold),
)

val crimsonProFamily = FontFamily(
    Font(R.font.crimson_pro_regular, weight = FontWeight.Normal),
    Font(R.font.crimson_pro_medium, weight = FontWeight.Medium),
)

val interFamily = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
)

private val WhisperColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9F7),
    onBackground = Color(0xFF2D2D2D),
    surface = Color(0xFFF9F9F7),
    onSurface = Color(0xFF2D2D2D),
    surfaceVariant = Color(0xFFF0EFED),
    onSurfaceVariant = Color(0xFF2D2D2D),
    outline = Color(0xFF2D2D2D),
)

private val WhisperTypography = Typography(
    displayLarge = TextStyle(fontFamily = playfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    displayMedium = TextStyle(fontFamily = playfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = playfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = crimsonProFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = crimsonProFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = interFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = interFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp),
)

@Composable
fun WhisperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhisperColorScheme,
        typography = WhisperTypography,
        content = content,
    )
}
