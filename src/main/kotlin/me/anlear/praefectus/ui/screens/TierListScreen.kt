package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class SortColumn { HERO, WINRATE, TIER, MATCHES }

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
    var error by remember { mutableStateOf<String?>(null) }
    var roleFilter by remember { mutableStateOf<DotaRole?>(null) }
    var sortColumn by remember { mutableStateOf(SortColumn.WINRATE) }
    var sortAscending by remember { mutableStateOf(false) }
    var minMatches by remember { mutableStateOf(100) }

    LaunchedEffect(bracket) {
        loading = true
        error = null
        try {
            heroes = heroRepository.getHeroes().sortedBy { it.displayName }
        } catch (e: Exception) {
            error = "Heroes: ${e.message}"
        }
        try {
            statsMap = heroRepository.getHeroStats(bracket)
        } catch (e: Exception) {
            error = (error?.let { "$it | " } ?: "") + "Stats: ${e.message}"
        }
        loading = false
    }

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
            val percentile = if (totalCount > 0) index.toDouble() / totalCount else 0.5
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
            SortColumn.TIER -> if (sortAscending) tierEntries.sortedBy { it.tier.ordinal } else tierEntries.sortedByDescending { it.tier.ordinal }
            SortColumn.MATCHES -> if (sortAscending) tierEntries.sortedBy { it.stats.matchCount } else tierEntries.sortedByDescending { it.stats.matchCount }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // Role filter — wrapping row
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

        if (error != null) {
            Text(
                "${Strings.get("error", lang)}: $error",
                fontSize = 12.sp,
                color = DotaColors.Dire,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (entries.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    Strings.get("no_data", lang),
                    color = DotaColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(DotaColors.BackgroundSecondary)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortableHeader(Strings.get("hero", lang), SortColumn.HERO, sortColumn, sortAscending, Modifier.weight(2.5f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("tier", lang), SortColumn.TIER, sortColumn, sortAscending, Modifier.weight(0.7f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("winrate", lang), SortColumn.WINRATE, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
            SortableHeader(Strings.get("matches", lang), SortColumn.MATCHES, sortColumn, sortAscending, Modifier.weight(1f)) { col ->
                if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = false }
            }
        }

        // Table body with scrollbar
        val listState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = 10.dp) // space for scrollbar
            ) {
                items(entries, key = { it.hero.id }) { entry ->
                    TierRow(entry)
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState)
            )
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
    val arrow = if (currentSort == column) { if (ascending) " \u25B2" else " \u25BC" } else ""
    Text(
        "$label$arrow",
        fontSize = 13.sp,
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
            .height(40.dp)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(DotaColors.Surface)
            .border(1.dp, DotaColors.SurfaceBorder.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hero icon + name — fixed layout
        Row(
            modifier = Modifier.weight(2.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroIcon(hero = entry.hero, size = 28)
            Spacer(Modifier.width(8.dp))
            Text(
                entry.hero.displayName,
                fontSize = 14.sp,
                color = DotaColors.TextPrimary,
                maxLines = 1
            )
        }

        // Tier badge
        Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tierColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.tier.display, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tierColor)
            }
        }

        // Win rate
        val wrColor = when {
            entry.stats.winRate >= 53 -> DotaColors.ScoreGood
            entry.stats.winRate >= 48 -> DotaColors.TextPrimary
            else -> DotaColors.ScoreBad
        }
        Text(
            "${"%.1f".format(entry.stats.winRate)}%",
            fontSize = 14.sp,
            color = wrColor,
            modifier = Modifier.weight(1f)
        )

        // Matches
        Text(
            "${entry.stats.matchCount}",
            fontSize = 14.sp,
            color = DotaColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
