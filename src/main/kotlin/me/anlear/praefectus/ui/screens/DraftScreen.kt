package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.anlear.praefectus.data.repository.HeroRepository
import me.anlear.praefectus.domain.DraftEngine
import me.anlear.praefectus.domain.RecommendationEngine
import me.anlear.praefectus.domain.models.*
import me.anlear.praefectus.ui.components.*
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

enum class PickTarget { RADIANT, DIRE, BAN }

@Composable
fun DraftScreen(
    heroRepository: HeroRepository,
    bracket: RankBracket,
    lang: Lang,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val draftEngine = remember { DraftEngine() }
    val recommendationEngine = remember { RecommendationEngine() }

    var heroes by remember { mutableStateOf<List<Hero>>(emptyList()) }
    var heroMap by remember { mutableStateOf<Map<Int, Hero>>(emptyMap()) }
    var statsMap by remember { mutableStateOf<Map<Int, HeroStats>>(emptyMap()) }
    var matchupsMap by remember { mutableStateOf<Map<Int, List<HeroMatchup>>>(emptyMap()) }
    var draftState by remember { mutableStateOf(DraftState()) }
    var draftMode by remember { mutableStateOf(DraftMode.ALL_PICK) }
    var pickTarget by remember { mutableStateOf(PickTarget.RADIANT) }
    var roleFilter by remember { mutableStateOf<DotaRole?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load data
    LaunchedEffect(bracket) {
        loading = true
        error = null
        try {
            val h = heroRepository.getHeroes()
            heroes = h.sortedBy { it.displayName }
            heroMap = h.associateBy { it.id }
            statsMap = heroRepository.getHeroStats(bracket)
            // Load matchups for all heroes
            val allMatchups = mutableMapOf<Int, List<HeroMatchup>>()
            for (hero in h) {
                try {
                    allMatchups[hero.id] = heroRepository.getHeroMatchups(hero.id, bracket)
                } catch (_: Exception) { }
            }
            matchupsMap = allMatchups
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    // Compute recommendations
    val recommendTeam = if (pickTarget == PickTarget.DIRE) DraftTeam.DIRE else DraftTeam.RADIANT
    val recommendations = remember(draftState, matchupsMap, statsMap, roleFilter, recommendTeam) {
        if (heroes.isEmpty()) emptyList()
        else recommendationEngine.recommend(
            allHeroes = heroes,
            draftState = draftState,
            team = recommendTeam,
            statsMap = statsMap,
            matchupsMap = matchupsMap,
            roleFilter = roleFilter
        )
    }

    // Team synergies
    val synergyRadiant = remember(draftState.radiantPicks, matchupsMap) {
        draftEngine.calculateTeamSynergy(draftState.radiantPicks, matchupsMap)
    }
    val synergyDire = remember(draftState.direPicks, matchupsMap) {
        draftEngine.calculateTeamSynergy(draftState.direPicks, matchupsMap)
    }

    fun onHeroClick(hero: Hero) {
        draftState = if (draftMode == DraftMode.CAPTAINS_MODE) {
            draftEngine.applyCmAction(draftState, hero.id)
        } else {
            when (pickTarget) {
                PickTarget.RADIANT -> draftEngine.applyAction(draftState, hero.id, DraftTeam.RADIANT, DraftActionType.PICK)
                PickTarget.DIRE -> draftEngine.applyAction(draftState, hero.id, DraftTeam.DIRE, DraftActionType.PICK)
                PickTarget.BAN -> draftEngine.applyAction(draftState, hero.id, DraftTeam.RADIANT, DraftActionType.BAN)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
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

        // Top bar: mode selector + rank + controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode toggle
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModeChip(Strings.get("all_pick", lang), draftMode == DraftMode.ALL_PICK) {
                    draftMode = DraftMode.ALL_PICK
                    draftState = draftEngine.reset(DraftMode.ALL_PICK)
                }
                ModeChip(Strings.get("captains_mode", lang), draftMode == DraftMode.CAPTAINS_MODE) {
                    draftMode = DraftMode.CAPTAINS_MODE
                    draftState = draftEngine.reset(DraftMode.CAPTAINS_MODE)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Undo
                Button(
                    onClick = { draftState = draftEngine.undo(draftState) },
                    enabled = draftState.history.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DotaColors.Surface,
                        contentColor = DotaColors.TextPrimary,
                        disabledContainerColor = DotaColors.Surface.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(Strings.get("undo", lang), fontSize = 12.sp)
                }
                // Reset
                Button(
                    onClick = { draftState = draftEngine.reset(draftMode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DotaColors.Dire.copy(alpha = 0.3f),
                        contentColor = DotaColors.Dire
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(Strings.get("reset_draft", lang), fontSize = 12.sp)
                }
            }
        }

        // CM step indicator
        if (draftMode == DraftMode.CAPTAINS_MODE && !draftState.isComplete()) {
            val step = DraftState.CM_SEQUENCE.getOrNull(draftState.cmStepIndex)
            if (step != null) {
                val teamName = if (step.team == DraftTeam.RADIANT) Strings.get("radiant", lang) else Strings.get("dire", lang)
                val actionName = if (step.type == DraftActionType.PICK) Strings.get("pick", lang) else Strings.get("ban", lang)
                val teamColor = if (step.team == DraftTeam.RADIANT) DotaColors.Radiant else DotaColors.Dire
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(teamColor.copy(alpha = 0.1f))
                        .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${Strings.get("current_step", lang)}: $teamName — $actionName (${draftState.cmStepIndex + 1}/${DraftState.CM_SEQUENCE.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = teamColor
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Pick target selector (All Pick mode only)
        if (draftMode == DraftMode.ALL_PICK) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                PickTargetChip(Strings.get("radiant", lang), pickTarget == PickTarget.RADIANT, DotaColors.Radiant) { pickTarget = PickTarget.RADIANT }
                PickTargetChip(Strings.get("dire", lang), pickTarget == PickTarget.DIRE, DotaColors.Dire) { pickTarget = PickTarget.DIRE }
                PickTargetChip(Strings.get("ban", lang), pickTarget == PickTarget.BAN, DotaColors.TextSecondary) { pickTarget = PickTarget.BAN }
            }
        }

        // Main content: Draft Panel + Hero Grid + Recommendations
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Draft panel
            DraftPanel(
                draftState = draftState,
                heroes = heroMap,
                teamSynergyRadiant = synergyRadiant,
                teamSynergyDire = synergyDire,
                lang = lang,
                modifier = Modifier.width(280.dp)
            )

            // Center: Hero grid
            HeroGrid(
                heroes = heroes,
                disabledIds = draftState.allPickedOrBanned,
                lang = lang,
                onHeroClick = { onHeroClick(it) },
                modifier = Modifier.weight(1f)
            )

            // Right: Recommendations
            Column(modifier = Modifier.width(280.dp)) {
                // Role filter for recommendations
                Text(Strings.get("recommend", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DotaColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    FilterChip(Strings.get("all_roles", lang), roleFilter == null) { roleFilter = null }
                    DotaRole.entries.forEach { role ->
                        FilterChip(Strings.get(role.locKey, lang), roleFilter == role) { roleFilter = role }
                    }
                }

                RecommendationList(
                    recommendations = recommendations,
                    lang = lang,
                    onHeroClick = { heroId ->
                        heroMap[heroId]?.let { onHeroClick(it) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) DotaColors.Accent.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (isSelected) DotaColors.Accent else DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary)
    }
}

@Composable
fun PickTargetChip(label: String, isSelected: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (isSelected) color else DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = if (isSelected) color else DotaColors.TextSecondary)
    }
}
