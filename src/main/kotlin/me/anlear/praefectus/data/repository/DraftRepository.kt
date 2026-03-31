package me.anlear.praefectus.data.repository

import me.anlear.praefectus.domain.models.*

class DraftRepository(private val heroRepository: HeroRepository) {

    suspend fun getMatchupsForHeroes(
        heroIds: List<Int>,
        bracket: RankBracket
    ): Map<Int, List<HeroMatchup>> {
        val result = mutableMapOf<Int, List<HeroMatchup>>()
        for (heroId in heroIds) {
            result[heroId] = heroRepository.getHeroMatchups(heroId, bracket)
        }
        return result
    }
}
