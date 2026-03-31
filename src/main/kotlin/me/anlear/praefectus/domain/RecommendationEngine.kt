package me.anlear.praefectus.domain

import me.anlear.praefectus.domain.models.*

class RecommendationEngine(
    private val counterWeight: Double = 1.5,
    private val synergyWeight: Double = 1.0,
    private val metaWeight: Double = 0.5
) {

    fun recommend(
        allHeroes: List<Hero>,
        draftState: DraftState,
        team: DraftTeam,
        statsMap: Map<Int, HeroStats>,
        matchupsMap: Map<Int, List<HeroMatchup>>,
        roleFilter: DotaRole? = null
    ): List<HeroRecommendation> {
        val excluded = draftState.allPickedOrBanned
        val allies = if (team == DraftTeam.RADIANT) draftState.radiantPicks else draftState.direPicks
        val enemies = if (team == DraftTeam.RADIANT) draftState.direPicks else draftState.radiantPicks

        return allHeroes
            .filter { it.id !in excluded }
            .filter { hero ->
                roleFilter == null || hero.roles.any { it.roleId == roleFilter }
            }
            .map { hero ->
                val counterScore = calculateCounterScore(hero.id, enemies, matchupsMap)
                val synergyScore = calculateSynergyScore(hero.id, allies, matchupsMap)
                val heroStats = statsMap[hero.id]
                val heroWinRate = heroStats?.winRate ?: 50.0
                val metaScore = (heroWinRate - 50.0) * metaWeight
                val totalScore = counterScore * counterWeight + synergyScore * synergyWeight + metaScore

                HeroRecommendation(
                    hero = hero,
                    totalScore = totalScore,
                    counterScore = counterScore * counterWeight,
                    synergyScore = synergyScore * synergyWeight,
                    metaScore = metaScore,
                    winRate = heroWinRate
                )
            }
            .sortedByDescending { it.totalScore }
    }

    private fun calculateCounterScore(
        heroId: Int,
        enemies: List<Int>,
        matchupsMap: Map<Int, List<HeroMatchup>>
    ): Double {
        if (enemies.isEmpty()) return 0.0

        val heroMatchups = matchupsMap[heroId] ?: return 0.0
        var total = 0.0

        for (enemyId in enemies) {
            val vsMatchup = heroMatchups.find { it.otherHeroId == enemyId && !it.isWith }
            if (vsMatchup != null && vsMatchup.matchCount > 0) {
                total += vsMatchup.winRate - 50.0
            }
        }

        return total
    }

    private fun calculateSynergyScore(
        heroId: Int,
        allies: List<Int>,
        matchupsMap: Map<Int, List<HeroMatchup>>
    ): Double {
        if (allies.isEmpty()) return 0.0

        val heroMatchups = matchupsMap[heroId] ?: return 0.0
        var total = 0.0

        for (allyId in allies) {
            val withMatchup = heroMatchups.find { it.otherHeroId == allyId && it.isWith }
            if (withMatchup != null && withMatchup.matchCount > 0) {
                total += withMatchup.winRate - 50.0
            }
        }

        return total
    }
}
