package me.anlear.praefectus.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.anlear.praefectus.domain.models.Hero
import me.anlear.praefectus.domain.models.TierRank
import me.anlear.praefectus.ui.theme.DotaColors
import java.net.URI
import javax.imageio.ImageIO

/**
 * Compact hero icon for the hero pool grid.
 * Only shows the hero portrait. On hover: scales up and shows displayName below.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeroPoolIcon(
    hero: Hero,
    isDisabled: Boolean = false,
    tierRank: TierRank? = null,
    recommendRank: Int? = null, // 1-based rank in recommendations, null if not recommended
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isHovered && !isDisabled) 1.25f else 1.0f)

    val tierColor = tierRank?.let {
        when (it) {
            TierRank.S -> DotaColors.TierS
            TierRank.A -> DotaColors.TierA
            TierRank.B -> DotaColors.TierB
            TierRank.C -> DotaColors.TierC
            TierRank.D -> DotaColors.TierD
        }
    }

    val borderColor = when {
        isDisabled -> Color.Transparent
        isHovered -> DotaColors.Accent
        tierColor != null -> tierColor.copy(alpha = 0.5f)
        else -> DotaColors.SurfaceBorder.copy(alpha = 0.3f)
    }

    val alpha = if (isDisabled) 0.2f else 1.0f

    Box(
        modifier = modifier
            .zIndex(if (isHovered) 10f else 0f),
        contentAlignment = Alignment.TopCenter
    ) {
        // Use graphicsLayer for scale so it does NOT affect layout measurement
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(4.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
                .background(DotaColors.Surface.copy(alpha = alpha))
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .then(if (!isDisabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            HeroIcon(hero = hero, size = 42)

            // Tier indicator — thin line at bottom
            if (tierColor != null && !isDisabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(tierColor.copy(alpha = 0.8f))
                )
            }

            // Recommendation badge
            if (recommendRank != null && recommendRank <= 10 && !isDisabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DotaColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$recommendRank",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Hero name tooltip shown on hover — absolutely positioned, doesn't affect layout
        if (isHovered && !isDisabled) {
            Text(
                hero.displayName,
                fontSize = 10.sp,
                color = DotaColors.TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 42.dp)
                    .widthIn(max = 90.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DotaColors.Surface.copy(alpha = 0.9f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Hero card for tier list rows — horizontal with icon + name.
 */
@Composable
fun HeroCard(
    hero: Hero,
    isDisabled: Boolean = false,
    isSelected: Boolean = false,
    tierRank: TierRank? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // kept for backward compatibility but not used in new draft UI
    HeroPoolIcon(
        hero = hero,
        isDisabled = isDisabled,
        tierRank = tierRank,
        onClick = onClick,
        modifier = modifier
    )
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

    // Dota 2 hero portraits are 16:9ish (127×71 on Valve CDN)
    val iconWidth = (size * 1.78).toInt()
    Box(modifier = modifier.size(iconWidth.dp, size.dp), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = hero.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            loading -> CircularProgressIndicator(
                modifier = Modifier.size((size / 2).dp),
                strokeWidth = 1.dp,
                color = DotaColors.Accent
            )
            else -> Text(
                hero.displayName.take(3),
                fontSize = (size / 4).sp,
                color = DotaColors.TextSecondary
            )
        }
    }
}
