package me.anlear.praefectus.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.domain.models.*
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

/**
 * Hero pool grid with 4 attribute groups side by side.
 * Each group shows heroes as icon-only cards, 6 per row.
 * Search bar at top. Scrolls vertically.
 */
@Composable
fun HeroGridWithAttributes(
    heroes: List<Hero>,
    disabledIds: Set<Int>,
    tierMap: Map<Int, TierRank>,
    recommendRanks: Map<Int, Int>, // heroId -> 1-based rank
    lang: Lang,
    onHeroClick: (Hero) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    val filteredHeroes = heroes.filter { hero ->
        searchText.isBlank() ||
            hero.displayName.contains(searchText, ignoreCase = true) ||
            hero.shortName.contains(searchText, ignoreCase = true)
    }

    val strHeroes = filteredHeroes.filter { it.primaryAttribute == HeroAttribute.STRENGTH }
    val agiHeroes = filteredHeroes.filter { it.primaryAttribute == HeroAttribute.AGILITY }
    val intHeroes = filteredHeroes.filter { it.primaryAttribute == HeroAttribute.INTELLIGENCE }
    val uniHeroes = filteredHeroes.filter { it.primaryAttribute == HeroAttribute.UNIVERSAL }

    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text(Strings.get("search_hero", lang), color = DotaColors.TextSecondary, fontSize = 14.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DotaColors.TextPrimary,
                unfocusedTextColor = DotaColors.TextPrimary,
                focusedBorderColor = DotaColors.Accent,
                unfocusedBorderColor = DotaColors.SurfaceBorder,
                cursorColor = DotaColors.Accent,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )

        // 4 attribute groups side by side, each with icons in 6-column rows
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttributeGroup(
                    label = Strings.get("filter_str", lang),
                    color = DotaColors.StrAttribute,
                    heroes = strHeroes,
                    disabledIds = disabledIds,
                    tierMap = tierMap,
                    recommendRanks = recommendRanks,
                    onHeroClick = onHeroClick,
                    modifier = Modifier.weight(1f)
                )
                AttributeGroup(
                    label = Strings.get("filter_agi", lang),
                    color = DotaColors.AgiAttribute,
                    heroes = agiHeroes,
                    disabledIds = disabledIds,
                    tierMap = tierMap,
                    recommendRanks = recommendRanks,
                    onHeroClick = onHeroClick,
                    modifier = Modifier.weight(1f)
                )
                AttributeGroup(
                    label = Strings.get("filter_int", lang),
                    color = DotaColors.IntAttribute,
                    heroes = intHeroes,
                    disabledIds = disabledIds,
                    tierMap = tierMap,
                    recommendRanks = recommendRanks,
                    onHeroClick = onHeroClick,
                    modifier = Modifier.weight(1f)
                )
                AttributeGroup(
                    label = Strings.get("filter_uni", lang),
                    color = DotaColors.UniAttribute,
                    heroes = uniHeroes,
                    disabledIds = disabledIds,
                    tierMap = tierMap,
                    recommendRanks = recommendRanks,
                    onHeroClick = onHeroClick,
                    modifier = Modifier.weight(1f)
                )
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}

@Composable
fun AttributeGroup(
    label: String,
    color: Color,
    heroes: List<Hero>,
    disabledIds: Set<Int>,
    tierMap: Map<Int, TierRank>,
    recommendRanks: Map<Int, Int>,
    onHeroClick: (Hero) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Attribute header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }

        // Heroes grid: 6 icons per row
        val rows = heroes.chunked(6)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(DotaColors.BackgroundSecondary.copy(alpha = 0.3f))
                .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .padding(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            rows.forEach { rowHeroes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowHeroes.forEach { hero ->
                        HeroPoolIcon(
                            hero = hero,
                            isDisabled = hero.id in disabledIds,
                            tierRank = tierMap[hero.id],
                            recommendRank = recommendRanks[hero.id],
                            onClick = { onHeroClick(hero) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining slots so layout is even
                    repeat(6 - rowHeroes.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color = DotaColors.Accent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (isSelected) activeColor else DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) activeColor else DotaColors.TextSecondary
        )
    }
}
