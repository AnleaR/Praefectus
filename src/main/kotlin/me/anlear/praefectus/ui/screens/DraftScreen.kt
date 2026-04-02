package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.anlear.praefectus.data.repository.HeroRepository
import me.anlear.praefectus.domain.DraftEngine
import me.anlear.praefectus.domain.RecommendationEngine
import me.anlear.praefectus.domain.models.*
import me.anlear.praefectus.ui.components.*
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

data class SlotSelection(
    val team: DraftTeam,
    val type: DraftActionType,
    val index: Int
)

@Composable
fun DraftScreen(
    heroRepository: HeroRepository,
    bracket: RankBracket,
    lang: Lang,
    modifier: Modifier = Modifier
) {
    val draftEngine = remember { DraftEngine() }
    val recommendationEngine = remember { RecommendationEngine() }

    var heroes by remember { mutableStateOf<List<Hero>>(emptyList()) }
    var heroMap by remember { mutableStateOf<Map<Int, Hero>>(emptyMap()) }
    var statsMap by remember { mutableStateOf<Map<Int, HeroStats>>(emptyMap()) }
    var matchupsMap by remember { mutableStateOf<Map<Int, List<HeroMatchup>>>(emptyMap()) }
    var draftState by remember { mutableStateOf(DraftState()) }
    var draftMode by remember { mutableStateOf(DraftMode.ALL_PICK) }
    var yourSide by remember { mutableStateOf(DraftTeam.RADIANT) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var matchupsLoading by remember { mutableStateOf(false) }
    var matchupsProgress by remember { mutableStateOf(0f) }
    var selectedSlot by remember { mutableStateOf<SlotSelection?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(draftState, draftMode) {
        if (draftMode == DraftMode.ALL_PICK && selectedSlot == null) {
            selectedSlot = SlotSelection(yourSide, DraftActionType.PICK, 0)
        }
    }

    LaunchedEffect(bracket) {
        loading = true
        error = null
        try {
            val h = heroRepository.getHeroes()
            heroes = h.sortedBy { it.displayName }
            heroMap = h.associateBy { it.id }
            statsMap = heroRepository.getHeroStats(bracket)
        } catch (e: Exception) {
            error = e.message
        }
        loading = false

        if (heroes.isNotEmpty()) {
            matchupsLoading = true
            matchupsProgress = 0f
            val allMatchups = mutableMapOf<Int, List<HeroMatchup>>()
            heroes.forEachIndexed { index, hero ->
                try {
                    allMatchups[hero.id] = heroRepository.getHeroMatchups(hero.id, bracket)
                } catch (_: Exception) { }
                matchupsProgress = (index + 1).toFloat() / heroes.size
                if ((index + 1) % 10 == 0 || index == heroes.size - 1) {
                    matchupsMap = allMatchups.toMap()
                }
            }
            matchupsMap = allMatchups.toMap()
            matchupsLoading = false
        }
    }

    val tierMap = remember(heroes, statsMap) {
        if (heroes.isEmpty() || statsMap.isEmpty()) emptyMap()
        else {
            val withStats = heroes.mapNotNull { hero ->
                statsMap[hero.id]?.let { hero.id to it.winRate }
            }.sortedByDescending { it.second }
            val total = withStats.size
            withStats.mapIndexed { index, (heroId, _) ->
                val percentile = if (total > 0) index.toDouble() / total else 0.5
                val tier = when {
                    percentile < 0.10 -> TierRank.S
                    percentile < 0.30 -> TierRank.A
                    percentile < 0.55 -> TierRank.B
                    percentile < 0.80 -> TierRank.C
                    else -> TierRank.D
                }
                heroId to tier
            }.toMap()
        }
    }

    val recommendations = remember(draftState, matchupsMap, statsMap, yourSide) {
        if (heroes.isEmpty()) emptyList()
        else recommendationEngine.recommend(heroes, draftState, yourSide, statsMap, matchupsMap, null)
    }

    // Build heroId -> recommendation rank map (top 10 get badges on icons)
    val recommendRanks = remember(recommendations) {
        recommendations.take(10).mapIndexed { index, rec -> rec.hero.id to (index + 1) }.toMap()
    }

    // Combined highlighting: meta tier + recommendation score, only show tier A and above
    val combinedTierMap = remember(tierMap, recommendations) {
        if (recommendations.isEmpty()) {
            // No recommendations yet — use pure meta, but only S and A
            tierMap.filter { it.value <= TierRank.A }
        } else {
            // Score each hero: meta tier contributes + recommendation rank contributes
            // Meta tier score: S=4, A=3, B=2, C=1, D=0
            // Recommendation percentile also contributes
            val recScoreMap = recommendations.mapIndexed { index, rec ->
                val recPercentile = 1.0 - index.toDouble() / recommendations.size
                rec.hero.id to recPercentile
            }.toMap()

            val combined = mutableMapOf<Int, TierRank>()
            for ((heroId, metaTier) in tierMap) {
                val metaScore = when (metaTier) {
                    TierRank.S -> 4.0
                    TierRank.A -> 3.0
                    TierRank.B -> 2.0
                    TierRank.C -> 1.0
                    TierRank.D -> 0.0
                }
                val recScore = (recScoreMap[heroId] ?: 0.0) * 4.0  // scale to 0-4
                val finalScore = metaScore * 0.5 + recScore * 0.5
                val finalTier = when {
                    finalScore >= 3.5 -> TierRank.S
                    finalScore >= 2.5 -> TierRank.A
                    else -> null // Don't highlight below A
                }
                if (finalTier != null) {
                    combined[heroId] = finalTier
                }
            }
            combined.toMap()
        }
    }

    fun onHeroClick(hero: Hero) {
        if (hero.id in draftState.allPickedOrBanned) return
        if (draftMode == DraftMode.CAPTAINS_MODE) {
            draftState = draftEngine.applyCmAction(draftState, hero.id)
        } else {
            val slot = selectedSlot ?: return
            draftState = draftEngine.applyAction(draftState, hero.id, slot.team, slot.type)
            selectedSlot = findNextEmptySlot(draftState, slot, yourSide)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DotaColors.Accent)
                    Spacer(Modifier.height(8.dp))
                    Text(Strings.get("loading", lang), color = DotaColors.TextSecondary)
                }
            }
            return@Column
        }

        if (error != null && heroes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${Strings.get("error", lang)}: $error", color = DotaColors.Dire)
            }
            return@Column
        }

        // Help dialog
        if (showHelpDialog) {
            HelpDialog(lang = lang, onDismiss = { showHelpDialog = false })
        }

        // ===== TOP SECTION: Toolbar + Draft Slots =====

        // Toolbar row
        DraftToolbar(
            draftMode = draftMode,
            draftState = draftState,
            yourSide = yourSide,
            lang = lang,
            matchupsLoading = matchupsLoading,
            matchupsProgress = matchupsProgress,
            onModeChange = { mode ->
                draftMode = mode
                draftState = draftEngine.reset(mode)
                selectedSlot = if (mode == DraftMode.ALL_PICK) SlotSelection(yourSide, DraftActionType.PICK, 0) else null
            },
            onSideChange = { side ->
                yourSide = side
                if (draftMode == DraftMode.ALL_PICK) {
                    selectedSlot = findNextEmptySlot(draftState, SlotSelection(side, DraftActionType.PICK, 0), side)
                }
            },
            onUndo = {
                draftState = draftEngine.undo(draftState)
                if (draftMode == DraftMode.ALL_PICK && selectedSlot != null) {
                    selectedSlot = findNextEmptySlot(draftState, selectedSlot!!, yourSide)
                }
            },
            onReset = {
                draftState = draftEngine.reset(draftMode)
                selectedSlot = if (draftMode == DraftMode.ALL_PICK) SlotSelection(yourSide, DraftActionType.PICK, 0) else null
            },
            canUndo = draftState.history.isNotEmpty(),
            onHelpClick = { showHelpDialog = true }
        )

        Spacer(Modifier.height(4.dp))

        // Draft picks/bans — horizontal layout at top
        DraftBoardHorizontal(
            draftState = draftState,
            heroes = heroMap,
            selectedSlot = if (draftMode == DraftMode.ALL_PICK) selectedSlot else null,
            yourSide = yourSide,
            draftMode = draftMode,
            lang = lang,
            onSlotClick = { slot ->
                if (draftMode == DraftMode.ALL_PICK) selectedSlot = slot
            }
        )

        Spacer(Modifier.height(6.dp))

        // ===== BOTTOM SECTION: Hero Pool + Details =====

        // Hero pool grid (takes remaining space)
        HeroGridWithAttributes(
            heroes = heroes,
            disabledIds = draftState.allPickedOrBanned,
            tierMap = combinedTierMap,
            recommendRanks = recommendRanks,
            lang = lang,
            onHeroClick = { onHeroClick(it) },
            modifier = Modifier.weight(1f)
        )

        // Bottom recommendations — detailed cards with counter/synergy/meta/winrate
        if (recommendations.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            TopRecommendationsBar(
                recommendations = recommendations.take(8),
                lang = lang,
                onHeroClick = { heroId -> heroMap[heroId]?.let { onHeroClick(it) } }
            )
        }
    }
}

/**
 * Draft board — Radiant picks (5 slots) | Bans | Dire picks (5 slots)
 * All horizontal at the top of the screen.
 */
@Composable
fun DraftBoardHorizontal(
    draftState: DraftState,
    heroes: Map<Int, Hero>,
    selectedSlot: SlotSelection?,
    yourSide: DraftTeam,
    draftMode: DraftMode,
    lang: Lang,
    onSlotClick: (SlotSelection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DotaColors.BackgroundSecondary)
            .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Radiant picks
        TeamPicksRow(
            team = DraftTeam.RADIANT,
            teamLabel = Strings.get("forces_of_light", lang),
            picks = draftState.radiantPicks,
            heroes = heroes,
            selectedSlot = selectedSlot,
            isYourSide = yourSide == DraftTeam.RADIANT,
            teamColor = DotaColors.Radiant,
            lang = lang,
            onSlotClick = onSlotClick,
            modifier = Modifier.weight(1f)
        )

        // Bans in center
        BansRow(
            bans = draftState.bans,
            heroes = heroes,
            selectedSlot = selectedSlot,
            draftMode = draftMode,
            lang = lang,
            onSlotClick = onSlotClick
        )

        // Dire picks
        TeamPicksRow(
            team = DraftTeam.DIRE,
            teamLabel = Strings.get("forces_of_dark", lang),
            picks = draftState.direPicks,
            heroes = heroes,
            selectedSlot = selectedSlot,
            isYourSide = yourSide == DraftTeam.DIRE,
            teamColor = DotaColors.Dire,
            lang = lang,
            onSlotClick = onSlotClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TeamPicksRow(
    team: DraftTeam,
    teamLabel: String,
    picks: List<Int>,
    heroes: Map<Int, Hero>,
    selectedSlot: SlotSelection?,
    isYourSide: Boolean,
    teamColor: Color,
    lang: Lang,
    onSlotClick: (SlotSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Team label
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(teamLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = teamColor)
            if (isYourSide) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(teamColor.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(Strings.get("your_side", lang).lowercase(), fontSize = 10.sp, color = teamColor)
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        // 5 pick slots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 5) {
                val heroId = picks.getOrNull(i)
                val hero = heroId?.let { heroes[it] }
                val isSlotSelected = selectedSlot?.team == team &&
                        selectedSlot.type == DraftActionType.PICK &&
                        selectedSlot.index == i

                HorizontalPickSlot(
                    hero = hero,
                    index = i,
                    teamColor = teamColor,
                    isSelected = isSlotSelected,
                    onClick = {
                        if (hero == null) onSlotClick(SlotSelection(team, DraftActionType.PICK, i))
                    }
                )
            }
        }
    }
}

@Composable
fun HorizontalPickSlot(
    hero: Hero?,
    index: Int,
    teamColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> DotaColors.Accent
        hero != null -> teamColor.copy(alpha = 0.5f)
        else -> DotaColors.SurfaceBorder
    }
    val bgColor = when {
        isSelected -> DotaColors.Accent.copy(alpha = 0.1f)
        else -> DotaColors.Surface
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable(enabled = hero == null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (hero != null) {
                HeroIcon(hero = hero, size = 36)
            } else {
                Text(
                    "${index + 1}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary.copy(0.3f)
                )
            }
        }
        // Hero name below slot
        if (hero != null) {
            Text(
                hero.displayName,
                fontSize = 10.sp,
                color = DotaColors.TextSecondary,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 72.dp)
            )
        }
    }
}

@Composable
fun BansRow(
    bans: List<Int>,
    heroes: Map<Int, Hero>,
    selectedSlot: SlotSelection?,
    draftMode: DraftMode,
    lang: Lang,
    onSlotClick: (SlotSelection) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Strings.get("bans", lang), fontSize = 12.sp, color = DotaColors.TextSecondary)
        Spacer(Modifier.height(3.dp))

        if (draftMode == DraftMode.ALL_PICK) {
            // Show ban slots in 2 rows of 7
            val maxBans = 14
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (rowStart in 0 until maxBans step 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in rowStart until minOf(rowStart + 7, maxBans)) {
                            val heroId = bans.getOrNull(i)
                            val hero = heroId?.let { heroes[it] }
                            val isSlotSelected = selectedSlot?.type == DraftActionType.BAN && selectedSlot.index == i

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isSlotSelected -> DotaColors.Accent.copy(alpha = 0.15f)
                                            hero != null -> DotaColors.Dire.copy(alpha = 0.1f)
                                            else -> DotaColors.Surface.copy(alpha = 0.3f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isSlotSelected -> DotaColors.Accent
                                            hero != null -> DotaColors.Dire.copy(alpha = 0.3f)
                                            else -> DotaColors.SurfaceBorder.copy(alpha = 0.3f)
                                        },
                                        RoundedCornerShape(3.dp)
                                    )
                                    .clickable(enabled = hero == null) {
                                        onSlotClick(SlotSelection(DraftTeam.RADIANT, DraftActionType.BAN, i))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hero != null) {
                                    HeroIcon(hero = hero, size = 18)
                                } else if (isSlotSelected) {
                                    Icon(Icons.Default.Block, null, tint = DotaColors.Accent, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // CM bans
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                bans.forEach { heroId ->
                    heroes[heroId]?.let { hero ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DotaColors.Dire.copy(alpha = 0.1f))
                                .border(1.dp, DotaColors.Dire.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            HeroIcon(hero = hero, size = 18)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed recommendation bar at the bottom showing top picks with counter/synergy/meta/winrate breakdown.
 */
@Composable
fun TopRecommendationsBar(
    recommendations: List<HeroRecommendation>,
    lang: Lang,
    onHeroClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DotaColors.BackgroundSecondary)
            .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            Strings.get("recommend", lang),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DotaColors.Accent
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            recommendations.forEachIndexed { index, rec ->
                val scoreColor = when {
                    rec.totalScore > 5 -> DotaColors.ScoreGood
                    rec.totalScore > 0 -> DotaColors.ScoreNeutral
                    else -> DotaColors.ScoreBad
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DotaColors.Surface)
                        .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { onHeroClick(rec.hero.id) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Rank badge + icon row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(DotaColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        HeroIcon(hero = rec.hero, size = 28)
                    }

                    // Hero name
                    Text(
                        rec.hero.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DotaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Total score
                    Text(
                        "%.1f".format(rec.totalScore),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )

                    // Breakdown: counter / synergy / meta / WR
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        ScoreLabel(Strings.get("counter", lang), rec.counterScore, DotaColors.Dire)
                        ScoreLabel(Strings.get("synergy", lang), rec.synergyScore, DotaColors.Radiant)
                        ScoreLabel(Strings.get("meta", lang), rec.metaScore, DotaColors.ScoreNeutral)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("WR", fontSize = 9.sp, color = DotaColors.TextSecondary)
                            Text(
                                "${"%.1f".format(rec.winRate)}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (rec.winRate >= 50) DotaColors.ScoreGood else DotaColors.ScoreBad
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreLabel(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 9.sp, color = DotaColors.TextSecondary, maxLines = 1)
        Text(
            "%+.1f".format(value),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                value > 0.5 -> color
                value < -0.5 -> DotaColors.ScoreBad
                else -> DotaColors.TextSecondary
            }
        )
    }
}

/**
 * Help dialog explaining how to use the app.
 */
@Composable
fun HelpDialog(lang: Lang, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DotaColors.BackgroundSecondary)
                .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                Strings.get("help_title", lang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DotaColors.TextPrimary
            )

            val scrollState = rememberScrollState()
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                Text(
                    Strings.get("help_text", lang),
                    fontSize = 13.sp,
                    color = DotaColors.TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DotaColors.Accent)
                ) {
                    Text(Strings.get("close", lang))
                }
            }
        }
    }
}

// ===== Toolbar =====

@Composable
private fun DraftToolbar(
    draftMode: DraftMode,
    draftState: DraftState,
    yourSide: DraftTeam,
    lang: Lang,
    matchupsLoading: Boolean,
    matchupsProgress: Float,
    onModeChange: (DraftMode) -> Unit,
    onSideChange: (DraftTeam) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    canUndo: Boolean,
    onHelpClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode toggle
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
        ) {
            SegmentButton("AP", draftMode == DraftMode.ALL_PICK) { onModeChange(DraftMode.ALL_PICK) }
            SegmentButton("CM", draftMode == DraftMode.CAPTAINS_MODE) { onModeChange(DraftMode.CAPTAINS_MODE) }
        }

        // Side selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(Strings.get("your_side", lang), fontSize = 13.sp, color = DotaColors.TextSecondary)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
            ) {
                SideButton(Strings.get("forces_of_light", lang), yourSide == DraftTeam.RADIANT, DotaColors.Radiant) { onSideChange(DraftTeam.RADIANT) }
                SideButton(Strings.get("forces_of_dark", lang), yourSide == DraftTeam.DIRE, DotaColors.Dire) { onSideChange(DraftTeam.DIRE) }
            }
        }

        // CM step indicator
        if (draftMode == DraftMode.CAPTAINS_MODE && !draftState.isComplete()) {
            val step = DraftState.CM_SEQUENCE.getOrNull(draftState.cmStepIndex)
            if (step != null) {
                val teamColor = if (step.team == DraftTeam.RADIANT) DotaColors.Radiant else DotaColors.Dire
                val actionText = if (step.type == DraftActionType.PICK) Strings.get("pick", lang) else Strings.get("ban", lang)
                val teamText = if (step.team == DraftTeam.RADIANT) Strings.get("radiant", lang) else Strings.get("dire", lang)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(teamColor.copy(alpha = 0.1f))
                        .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("${draftState.cmStepIndex + 1}/${DraftState.CM_SEQUENCE.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = teamColor)
                    Text("$teamText $actionText", fontSize = 13.sp, color = teamColor)
                }
            }
        }

        // Matchups loading indicator (small corner element)
        if (matchupsLoading) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DotaColors.Surface)
                    .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = DotaColors.Accent
                )
                Text(
                    "${(matchupsProgress * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = DotaColors.TextSecondary
                )
            }
        }

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onHelpClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    @Suppress("DEPRECATION") Icons.Default.HelpOutline,
                    Strings.get("help", lang),
                    tint = DotaColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    Strings.get("undo", lang),
                    tint = if (canUndo) DotaColors.TextPrimary else DotaColors.TextSecondary.copy(0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onReset, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.RestartAlt, Strings.get("reset_draft", lang), tint = DotaColors.Dire, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SegmentButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) DotaColors.Accent.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary
        )
    }
}

@Composable
private fun SideButton(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, color = if (isSelected) color else DotaColors.TextSecondary)
    }
}

private fun findNextEmptySlot(state: DraftState, current: SlotSelection, yourSide: DraftTeam): SlotSelection {
    if (current.type == DraftActionType.PICK) {
        val picks = if (current.team == DraftTeam.RADIANT) state.radiantPicks else state.direPicks
        for (i in 0 until 5) {
            if (i >= picks.size) return SlotSelection(current.team, DraftActionType.PICK, i)
        }
        val otherTeam = if (current.team == DraftTeam.RADIANT) DraftTeam.DIRE else DraftTeam.RADIANT
        val otherPicks = if (otherTeam == DraftTeam.RADIANT) state.radiantPicks else state.direPicks
        for (i in 0 until 5) {
            if (i >= otherPicks.size) return SlotSelection(otherTeam, DraftActionType.PICK, i)
        }
    }
    if (current.type == DraftActionType.BAN) {
        for (i in current.index until 14) {
            if (i >= state.bans.size) return SlotSelection(DraftTeam.RADIANT, DraftActionType.BAN, i)
        }
    }
    val yourPicks = if (yourSide == DraftTeam.RADIANT) state.radiantPicks else state.direPicks
    for (i in 0 until 5) {
        if (i >= yourPicks.size) return SlotSelection(yourSide, DraftActionType.PICK, i)
    }
    return current
}
