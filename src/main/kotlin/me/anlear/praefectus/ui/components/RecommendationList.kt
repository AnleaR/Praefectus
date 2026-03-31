package me.anlear.praefectus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.domain.models.HeroRecommendation
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

@Composable
fun RecommendationList(
    recommendations: List<HeroRecommendation>,
    lang: Lang,
    onHeroClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            Strings.get("recommend", lang),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DotaColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recommendations.take(20), key = { it.hero.id }) { rec ->
                RecommendationRow(rec, lang) { onHeroClick(rec.hero.id) }
            }
        }
    }
}

@Composable
fun RecommendationRow(
    rec: HeroRecommendation,
    lang: Lang,
    onClick: () -> Unit
) {
    val scoreColor = when {
        rec.totalScore > 5 -> DotaColors.ScoreGood
        rec.totalScore > 0 -> DotaColors.ScoreNeutral
        else -> DotaColors.ScoreBad
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(DotaColors.Surface)
            .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroIcon(hero = rec.hero, size = 32)
        Spacer(Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(rec.hero.displayName, fontSize = 12.sp, color = DotaColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreChip(Strings.get("counter", lang), rec.counterScore, DotaColors.Dire)
                ScoreChip(Strings.get("synergy", lang), rec.synergyScore, DotaColors.Radiant)
                ScoreChip(Strings.get("meta", lang), rec.metaScore, DotaColors.Accent)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${"%.1f".format(rec.totalScore)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = scoreColor
            )
            Text(
                "WR: ${"%.1f".format(rec.winRate)}%",
                fontSize = 10.sp,
                color = DotaColors.TextSecondary
            )
        }
    }
}

@Composable
fun ScoreChip(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    val displayColor = when {
        value > 0 -> color.copy(alpha = 0.8f)
        value < 0 -> DotaColors.ScoreBad.copy(alpha = 0.6f)
        else -> DotaColors.TextSecondary
    }
    Text(
        "$label: ${"%.1f".format(value)}",
        fontSize = 9.sp,
        color = displayColor
    )
}
