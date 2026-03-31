package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.data.repository.HeroRepository
import me.anlear.praefectus.domain.models.*
import me.anlear.praefectus.ui.components.FilterChip
import me.anlear.praefectus.ui.components.HeroIcon
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

enum class SortColumn { HERO, WINRATE, PICKRATE, BANRATE, TIER, MATCHES }

@Composable
fun TierListScreen(
    heroRepository: HeroRepository,
    bracket: RankBracket,
    lang: Lang,
    modifier: Modifier = Modifier
) {
    var heroes by remember { mutableStateOf<List<Hero>>(emptyList()) }
    var statsMap by remember { mutableStateOf<Map<Int, HeroStats>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var roleFilter by remember { mutableStateOf<DotaRole?>(null) }
    var sortColumn by remember { mutableStateOf(SortColumn.WINRATE) }
    var sortAscending by remember { mutableStateOf(false) }
    var minMatches by remember { mutableStateOf(100) }

    LaunchedEffect(bracket) {
        loading = true
        try {
            heroes = heroRepository.getHeroes().sortedBy { it.displayName }
            statsMap = heroRepository.getHeroStats(bracket)
        } catch (_: Exception) { }
        loading = false
    }

    // Filter and compute tiers
    val entries = remember(heroes, statsMap, roleFilter, sortColumn, sortAscending, minMatches) {
        val filtered = heroes.filter { hero ->
            val stats = statsMap[hero.id]
            val matchesRole = roleFilter == null || hero.roles.any { it.roleId == roleFilter }
            val hasEnoughMatches = stats != null && stats.matchCount >= minMatches
            matchesRole && hasEnoughMatches
        }

        val winRates = filtered.mapNotNull { hero ->
            statsMap[hero.id]?.let { hero to it.winRate }
        }.sortedByDescending { it.second }

        val totalCount = winRates.size
        val tierEntries = winRates.mapIndexed { index, (hero, _) ->
            val percentile = index.toDouble() / totalCount
            val tier = when {
                percentile < 0.10 -> TierRank.S
                percentile < 0.30 -> TierRank.A
                percentile < 0.55 -> TierRank.B
                percentile < 0.80 -> TierRank.C
                else -> TierRank.D
            }
            TierEntry(hero, statsMap[hero.id]!!, tier)
        }

        when (sortColumn) {
            SortColumn.HERO -> if (sortAscending) tierEntries.sortedBy { it.hero.displayName } else tierEntries.sortedByDescending { it.hero.displayName }
            SortColumn.WINRATE -> if (sortAscending) tierEntries.sortedBy { it.stats.winRate } else tierEntries.sortedByDescending { it.stats.winRate }
            SortColumn.PICKRATE -> if (sortAscending) tierEntries.sortedBy { it.stats.pickRate } else tierEntries.sortedByDescending { it.stats.pickRate }
            SortColumn.BANRATE -> if (sortAscending) tierEntries.sortedBy { it.stats.banRate } else tierEntries.sortedByDescending { it.stats.banRate }
            SortColumn.TIER -> if (sortAscending) tierEntries.sortedBy { it.tier.ordinal } else tierEntries.sortedByDescending { it.tier.ordinal }
            SortColumn.MATCHES -> if (sortAscending) tierEntries.sortedBy { it.stats.matchCount } else tierEntries.sortedByDescending { it.stats.matchCount }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Text(
            Strings.get("tier_list", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DotaColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))

        // Role filter
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            FilterChip(Strings.get("all_roles", lang), roleFilter == null) { roleFilter = null }
            DotaRole.entries.forEach { role ->
                FilterChip(Strings.get(role.locKey, lang), roleFilter == role) { roleFilter = role }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DotaColors.Accent)
            }
            return@Column
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DotaColors.BackgroundSecondary)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortableHeader(Strings.get("hero", lang), SortColumn.HERO, sortColumn, sortAscending, Modifier.weight(2f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("tier", lang), SortColumn.TIER, sortColumn, sortAscending, Modifier.weight(0.7f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("winrate", lang), SortColumn.WINRATE, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("pickrate", lang), SortColumn.PICKRATE, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("banrate", lang), SortColumn.BANRATE, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("matches", lang), SortColumn.MATCHES, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
        }

        // Table body
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(entries, key = { it.hero.id }) { entry ->
                TierRow(entry)
            }
        }
    }
}

@Composable
fun SortableHeader(
    label: String,
    column: SortColumn,
    currentSort: SortColumn,
    ascending: Boolean,
    modifier: Modifier,
    onClick: (SortColumn) -> Unit
) {
    val arrow = if (currentSort == column) { if (ascending) " ▲" else " ▼" } else ""
    Text(
        "$label$arrow",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (currentSort == column) DotaColors.Accent else DotaColors.TextSecondary,
        modifier = modifier.clickable { onClick(column) }
    )
}

@Composable
fun TierRow(entry: TierEntry) {
    val tierColor = when (entry.tier) {
        TierRank.S -> DotaColors.TierS
        TierRank.A -> DotaColors.TierA
        TierRank.B -> DotaColors.TierB
        TierRank.C -> DotaColors.TierC
        TierRank.D -> DotaColors.TierD
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(DotaColors.Surface)
            .border(1.dp, DotaColors.SurfaceBorder.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hero
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            HeroIcon(hero = entry.hero, size = 28)
            Spacer(Modifier.width(6.dp))
            Text(entry.hero.displayName, fontSize = 12.sp, color = DotaColors.TextPrimary)
        }

        // Tier
        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tierColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.tier.display, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = tierColor)
            }
        }

        // Win Rate
        val wrColor = when {
            entry.stats.winRate >= 53 -> DotaColors.ScoreGood
            entry.stats.winRate >= 48 -> DotaColors.TextPrimary
            else -> DotaColors.ScoreBad
        }
        Text(
            "${"%.1f".format(entry.stats.winRate)}%",
            fontSize = 12.sp,
            color = wrColor,
            modifier = Modifier.weight(1f)
        )

        // Pick Rate
        Text(
            "${entry.stats.pickCount}",
            fontSize = 12.sp,
            color = DotaColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )

        // Ban Rate
        Text(
            "${entry.stats.banCount}",
            fontSize = 12.sp,
            color = DotaColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )

        // Matches
        Text(
            "${entry.stats.matchCount}",
            fontSize = 12.sp,
            color = DotaColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
