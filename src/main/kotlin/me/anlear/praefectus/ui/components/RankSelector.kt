package me.anlear.praefectus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.anlear.praefectus.domain.models.RankBracket
import me.anlear.praefectus.ui.theme.DotaColors
import java.net.URI
import javax.imageio.ImageIO

private val rankIconCache = mutableMapOf<Int, ImageBitmap?>()

@Composable
fun RankMedalIcon(bracket: RankBracket, size: Int = 28) {
    var bitmap by remember(bracket) { mutableStateOf(rankIconCache[bracket.iconIndex]) }
    var loading by remember(bracket) { mutableStateOf(bitmap == null) }

    LaunchedEffect(bracket) {
        if (bitmap != null) return@LaunchedEffect
        loading = true
        bitmap = withContext(Dispatchers.IO) {
            try {
                val url = "https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/rank_icons/rank_icon_${bracket.iconIndex}.png"
                val img = ImageIO.read(URI(url).toURL())
                img?.toComposeImageBitmap()?.also { rankIconCache[bracket.iconIndex] = it }
            } catch (_: Exception) { null }
        }
        loading = false
    }

    Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = bracket.display,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            loading -> CircularProgressIndicator(
                modifier = Modifier.size((size / 2).dp),
                strokeWidth = 1.dp,
                color = DotaColors.Accent
            )
            else -> Text(bracket.display.take(2), fontSize = (size / 3).sp, color = DotaColors.TextSecondary)
        }
    }
}

@Composable
fun RankSelector(
    selected: RankBracket,
    onSelect: (RankBracket) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DotaColors.Surface)
                .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RankMedalIcon(selected, size = 28)
            Text(selected.display, fontSize = 13.sp, color = DotaColors.TextPrimary)
            Text("\u25BE", fontSize = 11.sp, color = DotaColors.TextSecondary)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DotaColors.Surface)
        ) {
            RankBracket.entries.forEach { bracket ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RankMedalIcon(bracket, size = 28)
                            Text(bracket.display, color = DotaColors.TextPrimary, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        onSelect(bracket)
                        expanded = false
                    }
                )
            }
        }
    }
}
