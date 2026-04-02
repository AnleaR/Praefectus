package me.anlear.praefectus.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class GraphQlResponse<T>(
    val data: T? = null,
    val errors: List<GraphQlError>? = null
)

@Serializable
data class GraphQlError(
    val message: String
)

// --- Constants (heroes, game versions) ---

@Serializable
data class ConstantsData(
    val constants: Constants
)

@Serializable
data class Constants(
    val heroes: List<ApiHero>? = null,
    val gameVersions: List<ApiGameVersion>? = null
)

@Serializable
data class ApiHero(
    val id: Int,
    val name: String,
    val displayName: String,
    val shortName: String,
    val stats: ApiHeroStats? = null,
    val roles: List<ApiHeroRole>? = null
)

@Serializable
data class ApiHeroStats(
    val primaryAttribute: String? = null,
    val attackType: String? = null
)

@Serializable
data class ApiHeroRole(
    val roleId: String,
    val level: Int
)

@Serializable
data class ApiGameVersion(
    val id: Int,
    val name: String? = null
)

// --- Hero Stats (winWeek) ---

@Serializable
data class HeroStatsData(
    val heroStats: HeroStatsWrapper
)

@Serializable
data class HeroStatsWrapper(
    val winWeek: List<ApiHeroWinEntry>? = null,
    val heroVsHeroMatchup: HeroMatchupType? = null
)

@Serializable
data class ApiHeroWinEntry(
    val heroId: Int,
    val matchCount: Long = 0,
    val winCount: Long = 0
)

// --- Hero Matchups ---

@Serializable
data class HeroMatchupType(
    val advantage: List<MatchupAdvantage>? = null
)

@Serializable
data class MatchupAdvantage(
    val heroId: Int,
    val with: List<MatchupEntry>? = null,
    val vs: List<MatchupEntry>? = null
)

@Serializable
data class MatchupEntry(
    val heroId2: Int,
    val matchCount: Long,
    val winCount: Long
)
