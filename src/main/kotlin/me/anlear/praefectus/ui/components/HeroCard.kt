package me.anlear.praefectus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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

// Grayscale color matrix (saturation = 0)
private val GrayscaleMatrix = ColorMatrix(
    floatArrayOf(
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0.33f, 0.33f, 0.33f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
)

/**
 * Compact hero icon for the hero pool grid.
 * Only shows the hero portrait. On hover: scales up and shows displayName below.
 * Disabled heroes (picked/banned) show grayscale + block icon on hover, click undoes them.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeroPoolIcon(
    hero: Hero,
    isDisabled: Boolean = false,
    tierRank: TierRank? = null,
    recommendRank: Int? = null,
    onClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isHovered) 1.25f else 1.0f)

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
        isDisabled && isHovered -> DotaColors.Dire.copy(alpha = 0.7f)
        isDisabled -> Color.Transparent
        isHovered -> DotaColors.Accent
        tierColor != null -> tierColor.copy(alpha = 0.5f)
        else -> DotaColors.SurfaceBorder.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .zIndex(if (isHovered) 10f else 0f),
        contentAlignment = Alignment.TopCenter
    ) {
        // Use graphicsLayer for scale — does NOT affect layout measurement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = false
                }
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                .background(DotaColors.Surface)
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .clickable(onClick = if (isDisabled) onUndoClick else onClick),
            contentAlignment = Alignment.Center
        ) {
            HeroIcon(hero = hero, size = 42, grayscale = isDisabled, fillWidth = true)

            // Block icon overlay on hover for disabled (picked/banned) heroes
            if (isDisabled && isHovered) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = DotaColors.Dire,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

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

        // Hero name tooltip on hover (not for disabled heroes — they show block icon instead)
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
                    .graphicsLayer { clip = false }
                    .offset(y = 46.dp)
                    .widthIn(max = 90.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DotaColors.Surface.copy(alpha = 0.95f))
                    .border(1.dp, DotaColors.SurfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
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

/**
 * @param fillWidth if true, fills parent width and uses aspectRatio instead of fixed size
 */
@Composable
fun HeroIcon(hero: Hero, size: Int = 48, grayscale: Boolean = false, fillWidth: Boolean = false, modifier: Modifier = Modifier) {
    var bitmap by remember(hero.id) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(hero.id) { mutableStateOf(true) }

    LaunchedEffect(hero.id) {
        loading = true
        bitmap = HeroImageCache.load(hero.iconUrl)
        loading = false
    }

    // Dota 2 hero portraits are ~1.78:1 (127×71 on Valve CDN)
    val iconWidth = (size * 1.78).toInt()
    val sizeModifier = if (fillWidth) {
        modifier.fillMaxWidth().aspectRatio(1.78f)
    } else {
        modifier.size(iconWidth.dp, size.dp)
    }
    Box(modifier = sizeModifier, contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = hero.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (grayscale) ColorFilter.colorMatrix(GrayscaleMatrix) else null
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
