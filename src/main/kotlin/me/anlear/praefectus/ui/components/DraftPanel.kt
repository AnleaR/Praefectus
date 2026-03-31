package me.anlear.praefectus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.domain.models.DraftState
import me.anlear.praefectus.domain.models.DraftTeam
import me.anlear.praefectus.domain.models.Hero
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

@Composable
fun DraftPanel(
    draftState: DraftState,
    heroes: Map<Int, Hero>,
    teamSynergyRadiant: Double,
    teamSynergyDire: Double,
    lang: Lang,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radiant
            TeamColumn(
                teamName = Strings.get("radiant", lang),
                picks = draftState.radiantPicks,
                heroes = heroes,
                teamColor = DotaColors.Radiant,
                synergy = teamSynergyRadiant,
                lang = lang,
                modifier = Modifier.weight(1f)
            )

            // Dire
            TeamColumn(
                teamName = Strings.get("dire", lang),
                picks = draftState.direPicks,
                heroes = heroes,
                teamColor = DotaColors.Dire,
                synergy = teamSynergyDire,
                lang = lang,
                modifier = Modifier.weight(1f)
            )
        }

        // Bans section
        if (draftState.bans.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.get("bans", lang),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DotaColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                draftState.bans.forEach { heroId ->
                    heroes[heroId]?.let { hero ->
                        HeroCard(
                            hero = hero,
                            isDisabled = true,
                            modifier = Modifier.size(48.dp, 40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamColumn(
    teamName: String,
    picks: List<Int>,
    heroes: Map<Int, Hero>,
    teamColor: Color,
    synergy: Double,
    lang: Lang,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DotaColors.BackgroundSecondary)
            .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(teamName, fontWeight = FontWeight.Bold, color = teamColor, fontSize = 14.sp)
            if (picks.size >= 2) {
                val synergyColor = when {
                    synergy >= 52 -> DotaColors.ScoreGood
                    synergy >= 48 -> DotaColors.ScoreNeutral
                    else -> DotaColors.ScoreBad
                }
                Text(
                    "${Strings.get("synergy", lang)}: ${"%.1f".format(synergy)}%",
                    fontSize = 10.sp,
                    color = synergyColor
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        for (i in 0 until 5) {
            val heroId = picks.getOrNull(i)
            val hero = heroId?.let { heroes[it] }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DotaColors.Surface)
                    .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (hero != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        HeroIcon(hero = hero, size = 28)
                        Spacer(Modifier.width(6.dp))
                        Text(hero.displayName, fontSize = 12.sp, color = DotaColors.TextPrimary)
                    }
                } else {
                    Text(
                        "  ${i + 1}",
                        fontSize = 12.sp,
                        color = DotaColors.TextSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
