package me.anlear.praefectus.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Dota 2 inspired dark color palette
object DotaColors {
    val Background = Color(0xFF1A1A2E)
    val BackgroundSecondary = Color(0xFF16213E)
    val Surface = Color(0xFF1E293B)
    val SurfaceBorder = Color(0xFF334155)
    val Radiant = Color(0xFF4ADE80)
    val Dire = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFF9CA3AF)
    val Accent = Color(0xFF3B82F6)
    val ScoreGood = Color(0xFF4ADE80)
    val ScoreBad = Color(0xFFEF4444)
    val ScoreNeutral = Color(0xFFFBBF24)
    val TierS = Color(0xFFFF6B6B)
    val TierA = Color(0xFFFFA726)
    val TierB = Color(0xFFFBBF24)
    val TierC = Color(0xFF66BB6A)
    val TierD = Color(0xFF42A5F5)
    val StrAttribute = Color(0xFFEF4444)
    val AgiAttribute = Color(0xFF4ADE80)
    val IntAttribute = Color(0xFF3B82F6)
    val UniAttribute = Color(0xFFF5A623)
}

private val DarkColorScheme = darkColorScheme(
    primary = DotaColors.Accent,
    onPrimary = Color.White,
    secondary = DotaColors.Radiant,
    onSecondary = Color.Black,
    tertiary = DotaColors.Dire,
    onTertiary = Color.White,
    background = DotaColors.Background,
    onBackground = DotaColors.TextPrimary,
    surface = DotaColors.Surface,
    onSurface = DotaColors.TextPrimary,
    surfaceVariant = DotaColors.BackgroundSecondary,
    onSurfaceVariant = DotaColors.TextSecondary,
    outline = DotaColors.SurfaceBorder,
)

@Composable
fun PraefectusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DotaColors.TextPrimary),
            headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DotaColors.TextPrimary),
            titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DotaColors.TextPrimary),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = DotaColors.TextPrimary),
            bodyLarge = TextStyle(fontSize = 14.sp, color = DotaColors.TextPrimary),
            bodyMedium = TextStyle(fontSize = 13.sp, color = DotaColors.TextPrimary),
            bodySmall = TextStyle(fontSize = 12.sp, color = DotaColors.TextSecondary),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 10.sp)
        ),
        content = content
    )
}
