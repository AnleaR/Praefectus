package me.anlear.praefectus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.anlear.praefectus.domain.models.Hero
import me.anlear.praefectus.ui.theme.DotaColors
import java.net.URI
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeroCard(
    hero: Hero,
    isDisabled: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val borderColor = when {
        isSelected -> DotaColors.Accent
        isHovered && !isDisabled -> DotaColors.TextSecondary
        else -> Color.Transparent
    }

    val alpha = if (isDisabled) 0.3f else 1.0f

    Box(
        modifier = modifier
            .size(64.dp, 52.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(DotaColors.Surface.copy(alpha = alpha))
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .then(if (!isDisabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            HeroIcon(hero = hero, size = 36, modifier = Modifier.fillMaxWidth())
            Text(
                text = hero.shortName,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = DotaColors.TextSecondary.copy(alpha = alpha)
            )
        }

        if (isHovered && !isDisabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-24).dp)
                    .background(DotaColors.Background, RoundedCornerShape(4.dp))
                    .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = hero.displayName,
                    fontSize = 11.sp,
                    color = DotaColors.TextPrimary
                )
            }
        }
    }
}

object HeroImageCache {
    private val cache = mutableMapOf<String, ImageBitmap?>()

    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val image = ImageIO.read(URI(url).toURL())
                image?.toComposeImageBitmap()?.also { cache[url] = it }
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
fun HeroIcon(hero: Hero, size: Int = 48, modifier: Modifier = Modifier) {
    var bitmap by remember(hero.id) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(hero.id) { mutableStateOf(true) }

    LaunchedEffect(hero.id) {
        loading = true
        bitmap = HeroImageCache.load(hero.iconUrl)
        loading = false
    }

    Box(modifier = modifier.height(size.dp), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = hero.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
            loading -> CircularProgressIndicator(
                modifier = Modifier.size((size / 2).dp),
                strokeWidth = 1.dp,
                color = DotaColors.Accent
            )
            else -> Text(
                hero.shortName.take(3),
                fontSize = (size / 4).sp,
                color = DotaColors.TextSecondary
            )
        }
    }
}
