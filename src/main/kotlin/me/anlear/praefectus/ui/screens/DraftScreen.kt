package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(bracket) {
        loading = true
        error = null
        try {
            val h = heroRepository.getHeroes()
            heroes = h.sortedBy { it.displayName }
            heroMap = h.associateBy { it.id }
            statsMap = heroRepository.getHeroStats(bracket)
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

    val recommendTeam = if (pickTarget == PickTarget.DIRE) DraftTeam.DIRE else DraftTeam.RADIANT
    val recommendations = remember(draftState, matchupsMap, statsMap, roleFilter, recommendTeam) {
        if (heroes.isEmpty()) emptyList()
        else recommendationEngine.recommend(heroes, draftState, recommendTeam, statsMap, matchupsMap, roleFilter)
    }
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

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
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

        // Toolbar: mode toggle + actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode toggle (segmented button style)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
            ) {
                SegmentButton("AP", draftMode == DraftMode.ALL_PICK) {
                    draftMode = DraftMode.ALL_PICK; draftState = draftEngine.reset(DraftMode.ALL_PICK)
                }
                SegmentButton("CM", draftMode == DraftMode.CAPTAINS_MODE) {
                    draftMode = DraftMode.CAPTAINS_MODE; draftState = draftEngine.reset(DraftMode.CAPTAINS_MODE)
                }
            }

            // Pick target (All Pick only)
            if (draftMode == DraftMode.ALL_PICK) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(6.dp))
                ) {
                    TargetButton(Icons.Default.Shield, Strings.get("radiant", lang), pickTarget == PickTarget.RADIANT, DotaColors.Radiant) { pickTarget = PickTarget.RADIANT }
                    TargetButton(Icons.Default.Shield, Strings.get("dire", lang), pickTarget == PickTarget.DIRE, DotaColors.Dire) { pickTarget = PickTarget.DIRE }
                    TargetButton(Icons.Default.Block, Strings.get("ban", lang), pickTarget == PickTarget.BAN, DotaColors.TextSecondary) { pickTarget = PickTarget.BAN }
                }
            }

            // CM step indicator
            if (draftMode == DraftMode.CAPTAINS_MODE && !draftState.isComplete()) {
                val step = DraftState.CM_SEQUENCE.getOrNull(draftState.cmStepIndex)
                if (step != null) {
                    val teamColor = if (step.team == DraftTeam.RADIANT) DotaColors.Radiant else DotaColors.Dire
                    val actionIcon = if (step.type == DraftActionType.PICK) Icons.Default.CheckCircle else Icons.Default.Block
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(teamColor.copy(alpha = 0.1f))
                            .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(actionIcon, null, tint = teamColor, modifier = Modifier.size(16.dp))
                        Text(
                            "${draftState.cmStepIndex + 1}/${DraftState.CM_SEQUENCE.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = teamColor
                        )
                    }
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { draftState = draftEngine.undo(draftState) },
                    enabled = draftState.history.isNotEmpty(),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Undo, Strings.get("undo", lang), tint = if (draftState.history.isNotEmpty()) DotaColors.TextPrimary else DotaColors.TextSecondary.copy(0.3f), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { draftState = draftEngine.reset(draftMode) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, Strings.get("reset_draft", lang), tint = DotaColors.Dire, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Main content
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: Draft panel
            DraftPanel(
                draftState = draftState,
                heroes = heroMap,
                teamSynergyRadiant = synergyRadiant,
                teamSynergyDire = synergyDire,
                lang = lang,
                modifier = Modifier.width(260.dp)
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
            Column(modifier = Modifier.width(260.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    FilterChip(Strings.get("all", lang), roleFilter == null) { roleFilter = null }
                    DotaRole.entries.forEach { role ->
                        FilterChip(Strings.get(role.locKey, lang), roleFilter == role) { roleFilter = role }
                    }
                }

                RecommendationList(
                    recommendations = recommendations,
                    lang = lang,
                    onHeroClick = { heroId -> heroMap[heroId]?.let { onHeroClick(it) } },
                    modifier = Modifier.weight(1f)
                )
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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DotaColors.Accent else DotaColors.TextSecondary
        )
    }
}

@Composable
private fun TargetButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) color else DotaColors.TextSecondary, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 11.sp, color = if (isSelected) color else DotaColors.TextSecondary)
    }
}
