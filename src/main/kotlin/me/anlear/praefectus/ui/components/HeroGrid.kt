package me.anlear.praefectus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.domain.models.AttackType
import me.anlear.praefectus.domain.models.Hero
import me.anlear.praefectus.domain.models.HeroAttribute
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

@Composable
fun HeroGrid(
    heroes: List<Hero>,
    disabledIds: Set<Int>,
    lang: Lang,
    onHeroClick: (Hero) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var attrFilter by remember { mutableStateOf<HeroAttribute?>(null) }
    var attackFilter by remember { mutableStateOf<AttackType?>(null) }

    val filteredHeroes = heroes.filter { hero ->
        val matchesSearch = searchText.isBlank() ||
            hero.displayName.contains(searchText, ignoreCase = true) ||
            hero.shortName.contains(searchText, ignoreCase = true)
        val matchesAttr = attrFilter == null || hero.primaryAttribute == attrFilter
        val matchesAttack = attackFilter == null || hero.attackType == attackFilter
        matchesSearch && matchesAttr && matchesAttack
    }

    Column(modifier = modifier) {
        // Search
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text(Strings.get("search_hero", lang), color = DotaColors.TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DotaColors.TextPrimary,
                unfocusedTextColor = DotaColors.TextPrimary,
                focusedBorderColor = DotaColors.Accent,
                unfocusedBorderColor = DotaColors.SurfaceBorder,
                cursorColor = DotaColors.Accent,
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Attribute filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            FilterChip(Strings.get("all", lang), attrFilter == null) { attrFilter = null; attackFilter = null }
            FilterChip(Strings.get("filter_str", lang), attrFilter == HeroAttribute.STRENGTH, DotaColors.StrAttribute) { attrFilter = HeroAttribute.STRENGTH }
            FilterChip(Strings.get("filter_agi", lang), attrFilter == HeroAttribute.AGILITY, DotaColors.AgiAttribute) { attrFilter = HeroAttribute.AGILITY }
            FilterChip(Strings.get("filter_int", lang), attrFilter == HeroAttribute.INTELLIGENCE, DotaColors.IntAttribute) { attrFilter = HeroAttribute.INTELLIGENCE }
            FilterChip(Strings.get("filter_uni", lang), attrFilter == HeroAttribute.UNIVERSAL, DotaColors.UniAttribute) { attrFilter = HeroAttribute.UNIVERSAL }
            Spacer(Modifier.width(8.dp))
            FilterChip(Strings.get("filter_melee", lang), attackFilter == AttackType.MELEE) { attackFilter = if (attackFilter == AttackType.MELEE) null else AttackType.MELEE }
            FilterChip(Strings.get("filter_ranged", lang), attackFilter == AttackType.RANGED) { attackFilter = if (attackFilter == AttackType.RANGED) null else AttackType.RANGED }
        }

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(66.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(filteredHeroes, key = { it.id }) { hero ->
                HeroCard(
                    hero = hero,
                    isDisabled = hero.id in disabledIds,
                    onClick = { onHeroClick(hero) }
                )
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
            fontSize = 11.sp,
            color = if (isSelected) activeColor else DotaColors.TextSecondary
        )
    }
}
