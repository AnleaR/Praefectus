package me.anlear.praefectus.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo
        Image(
            painter = painterResource("icon_white.png"),
            contentDescription = "Praefectus",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

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
    val bg = if (isSelected) DotaColors.Accent.copy(alpha = 0.15f) else Color.Transparent
    val color = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = Strings.get(screen.locKey, lang),
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            Strings.get(screen.locKey, lang),
            fontSize = 11.sp,
            color = color,
            maxLines = 1
        )
    }
}
