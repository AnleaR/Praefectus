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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    supportBonus: Boolean = false,
    supportBonusValue: Double = 3.0,
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

    val recommendations = remember(draftState, matchupsMap, statsMap, yourSide, supportBonus, supportBonusValue) {
        if (heroes.isEmpty()) emptyList()
        else recommendationEngine.recommend(heroes, draftState, yourSide, statsMap, matchupsMap, null, supportBonus, supportBonusValue)
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

    fun onHeroUndoClick(hero: Hero) {
        draftState = draftEngine.removeSpecificHero(draftState, hero.id)
        if (draftMode == DraftMode.ALL_PICK && selectedSlot != null) {
            selectedSlot = findNextEmptySlot(draftState, selectedSlot!!, yourSide)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
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

        // Draft picks/bans — horizontal layout at top with draft analysis
        DraftBoardHorizontal(
            draftState = draftState,
            heroes = heroMap,
            selectedSlot = if (draftMode == DraftMode.ALL_PICK) selectedSlot else null,
            yourSide = yourSide,
            draftMode = draftMode,
            lang = lang,
            statsMap = statsMap,
            matchupsMap = matchupsMap,
            recommendationEngine = recommendationEngine,
            allHeroes = heroes,
            onSlotClick = { slot ->
                if (draftMode == DraftMode.ALL_PICK) selectedSlot = slot
            },
            onHeroUndoClick = { heroId ->
                heroMap[heroId]?.let { onHeroUndoClick(it) }
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
            onHeroUndoClick = { onHeroUndoClick(it) },
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
    } // end Column

    // Help overlay — rendered inside the same Box to avoid hierarchy issues (CMP-2326)
    if (showHelpDialog) {
        HelpOverlay(lang = lang, onDismiss = { showHelpDialog = false })
    }
    } // end Box
}

/**
 * Draft board — Radiant picks (5 slots) | Bans + analysis | Dire picks (5 slots)
 * All horizontal at the top of the screen with draft analysis under each team's picks.
 */
@Composable
fun DraftBoardHorizontal(
    draftState: DraftState,
    heroes: Map<Int, Hero>,
    selectedSlot: SlotSelection?,
    yourSide: DraftTeam,
    draftMode: DraftMode,
    lang: Lang,
    statsMap: Map<Int, HeroStats>,
    matchupsMap: Map<Int, List<HeroMatchup>>,
    recommendationEngine: RecommendationEngine,
    allHeroes: List<Hero>,
    onSlotClick: (SlotSelection) -> Unit,
    onHeroUndoClick: (Int) -> Unit = {}
) {
    // Calculate draft analysis for both teams
    val radiantAnalysis = remember(draftState.radiantPicks, draftState.direPicks, matchupsMap, statsMap) {
        calculateTeamAnalysis(draftState.radiantPicks, draftState.direPicks, matchupsMap, statsMap)
    }
    val direAnalysis = remember(draftState.direPicks, draftState.radiantPicks, matchupsMap, statsMap) {
        calculateTeamAnalysis(draftState.direPicks, draftState.radiantPicks, matchupsMap, statsMap)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DotaColors.BackgroundSecondary)
            .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Radiant picks + analysis
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
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
                onHeroUndoClick = onHeroUndoClick
            )
            if (draftState.radiantPicks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TeamAnalysisRow(radiantAnalysis, DotaColors.Radiant, lang)
            }
        }

        // Bans in center + draft advantage
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BansRow(
                bans = draftState.bans,
                heroes = heroes,
                selectedSlot = selectedSlot,
                draftMode = draftMode,
                lang = lang,
                onSlotClick = onSlotClick,
                onHeroUndoClick = onHeroUndoClick
            )

            // Draft advantage indicator
            if (draftState.radiantPicks.isNotEmpty() && draftState.direPicks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                DraftAdvantageIndicator(radiantAnalysis, direAnalysis, lang)
            }
        }

        // Dire picks + analysis
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
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
                onHeroUndoClick = onHeroUndoClick
            )
            if (draftState.direPicks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TeamAnalysisRow(direAnalysis, DotaColors.Dire, lang)
            }
        }
    }
}

data class TeamAnalysis(
    val counterScore: Double = 0.0,
    val synergyScore: Double = 0.0,
    val metaScore: Double = 0.0
) {
    val totalScore: Double get() = counterScore + synergyScore + metaScore
}

private fun calculateTeamAnalysis(
    teamPicks: List<Int>,
    enemyPicks: List<Int>,
    matchupsMap: Map<Int, List<HeroMatchup>>,
    statsMap: Map<Int, HeroStats>
): TeamAnalysis {
    if (teamPicks.isEmpty()) return TeamAnalysis()

    var counterTotal = 0.0
    var synergyTotal = 0.0
    var metaTotal = 0.0

    for (heroId in teamPicks) {
        // Counter score vs enemies
        if (enemyPicks.isNotEmpty()) {
            val heroMatchups = matchupsMap[heroId]
            if (heroMatchups != null) {
                for (enemyId in enemyPicks) {
                    val vs = heroMatchups.find { it.otherHeroId == enemyId && !it.isWith }
                    if (vs != null && vs.matchCount > 0) {
                        counterTotal += vs.winRate - 50.0
                    }
                }
            }
        }

        // Synergy score with allies
        val heroMatchups = matchupsMap[heroId]
        if (heroMatchups != null) {
            for (allyId in teamPicks) {
                if (allyId == heroId) continue
                val withMatchup = heroMatchups.find { it.otherHeroId == allyId && it.isWith }
                if (withMatchup != null && withMatchup.matchCount > 0) {
                    synergyTotal += withMatchup.winRate - 50.0
                }
            }
        }

        // Meta score
        val stats = statsMap[heroId]
        if (stats != null) {
            metaTotal += stats.winRate - 50.0
        }
    }

    // Synergy is counted from both sides, divide by 2
    return TeamAnalysis(
        counterScore = counterTotal,
        synergyScore = synergyTotal / 2.0,
        metaScore = metaTotal
    )
}

@Composable
private fun TeamAnalysisRow(analysis: TeamAnalysis, teamColor: Color, lang: Lang) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnalysisChip(Strings.get("counter", lang).take(3), analysis.counterScore, teamColor)
        AnalysisChip(Strings.get("synergy", lang).take(3), analysis.synergyScore, teamColor)
        AnalysisChip(Strings.get("meta", lang), analysis.metaScore, teamColor)
    }
}

@Composable
private fun AnalysisChip(label: String, value: Double, teamColor: Color) {
    val valueColor = when {
        value > 1 -> DotaColors.ScoreGood
        value < -1 -> DotaColors.ScoreBad
        else -> DotaColors.TextSecondary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(teamColor.copy(alpha = 0.08f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 9.sp, color = DotaColors.TextSecondary)
        Text("%+.1f".format(value), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun DraftAdvantageIndicator(radiantAnalysis: TeamAnalysis, direAnalysis: TeamAnalysis, lang: Lang) {
    val radiantTotal = radiantAnalysis.totalScore
    val direTotal = direAnalysis.totalScore
    val diff = radiantTotal - direTotal
    // Rough win probability: sigmoid based on score difference
    val radiantWinProb = (50.0 + diff * 1.5).coerceIn(20.0, 80.0)

    val betterTeam = if (diff >= 0) Strings.get("forces_of_light", lang) else Strings.get("forces_of_dark", lang)
    val betterColor = if (diff >= 0) DotaColors.Radiant else DotaColors.Dire
    val winProb = if (diff >= 0) radiantWinProb else 100.0 - radiantWinProb

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            betterTeam,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = betterColor
        )
        Text(
            "${"%.0f".format(winProb)}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = betterColor
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
    onHeroUndoClick: (Int) -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    },
                    onUndoClick = { heroId?.let { onHeroUndoClick(it) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HorizontalPickSlot(
    hero: Hero?,
    index: Int,
    teamColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUndoClick: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }

    val borderColor = when {
        isSelected -> DotaColors.Accent
        hero != null && isHovered -> DotaColors.Dire
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
                .size(width = 64.dp, height = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .clickable(onClick = if (hero != null) onUndoClick else onClick),
            contentAlignment = Alignment.Center
        ) {
            if (hero != null) {
                HeroIcon(hero = hero, size = 32)
                // Block overlay on hover
                if (isHovered) {
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
                modifier = Modifier.widthIn(max = 64.dp)
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
    onSlotClick: (SlotSelection) -> Unit,
    onHeroUndoClick: (Int) -> Unit = {}
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
                                    .clickable {
                                        if (hero != null) {
                                            onHeroUndoClick(hero.id)
                                        } else {
                                            onSlotClick(SlotSelection(DraftTeam.RADIANT, DraftActionType.BAN, i))
                                        }
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
                                .border(1.dp, DotaColors.Dire.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                .clickable { onHeroUndoClick(heroId) },
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

                    // Breakdown: counter / synergy / support bonus / meta / WR
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        ScoreLabel(Strings.get("counter", lang), rec.counterScore, DotaColors.Dire)
                        ScoreLabel(Strings.get("synergy", lang), rec.synergyScore, DotaColors.Radiant)
                        if (rec.supportScore > 0) {
                            ScoreLabel(Strings.get("support_bonus", lang), rec.supportScore, DotaColors.Accent)
                        }
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
 * Help overlay — rendered as a modal overlay inside the same window hierarchy.
 * Avoids "layouts are not part of the same hierarchy" error (CMP-2326).
 */
@Composable
fun HelpOverlay(lang: Lang, onDismiss: () -> Unit) {
    // Semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Dialog card — clickable with no-op to prevent backdrop click-through
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .heightIn(max = 500.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DotaColors.BackgroundSecondary)
                .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume click */ }
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
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    Strings.get("help_text", lang),
                    fontSize = 13.sp,
                    color = DotaColors.TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp).verticalScroll(scrollState)
                )
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = DotaColors.TextSecondary.copy(alpha = 0.3f),
                        hoverColor = DotaColors.TextSecondary.copy(alpha = 0.6f)
                    )
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
            SegmentButton(Strings.get("all_pick", lang), draftMode == DraftMode.ALL_PICK) { onModeChange(DraftMode.ALL_PICK) }
            SegmentButton(Strings.get("captains_mode", lang), draftMode == DraftMode.CAPTAINS_MODE) { onModeChange(DraftMode.CAPTAINS_MODE) }
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
