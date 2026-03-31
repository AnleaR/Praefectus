package me.anlear.praefectus.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

enum class Screen(val locKey: String, val icon: ImageVector) {
    DRAFT("draft", Icons.Default.SportsEsports),
    TIER_LIST("tier_list", Icons.Default.Leaderboard),
    SETTINGS("settings", Icons.Default.Settings)
}

@Composable
fun NavigationRail(
    currentScreen: Screen,
    lang: Lang,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(DotaColors.BackgroundSecondary)
            .border(width = 1.dp, color = DotaColors.SurfaceBorder)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App name
        Text(
            "P",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DotaColors.Accent,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Screen.entries.forEach { screen ->
            NavItem(
                screen = screen,
                isSelected = screen == currentScreen,
                lang = lang,
                onClick = { onNavigate(screen) }
            )
        }
    }
}

@Composable
fun NavItem(screen: Screen, isSelected: Boolean, lang: Lang, onClick: () -> Unit) {
    val bg = if (isSelected) DotaColors.Accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent
    val iconColor = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary
    val textColor = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = Strings.get(screen.locKey, lang),
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            Strings.get(screen.locKey, lang),
            fontSize = 9.sp,
            color = textColor,
            maxLines = 1
        )
    }
}
