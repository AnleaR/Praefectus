package me.anlear.praefectus.data.api.models

import kotlinx.serialization.SerialName
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
    val primaryAttribute: String,
    val attackType: String,
    val roles: List<ApiHeroRole>? = null
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

// --- Hero Stats ---

@Serializable
data class HeroStatsData(
    val heroStats: HeroStatsWrapper
)

@Serializable
data class HeroStatsWrapper(
    val stats: List<ApiHeroStat>? = null,
    val heroVsHeroMatchup: HeroVsHeroMatchup? = null
)

@Serializable
data class ApiHeroStat(
    val heroId: Int,
    val matchCount: Long,
    val winCount: Long,
    @SerialName("pickCount") val pickCount: Long = 0,
    @SerialName("banCount") val banCount: Long = 0
)

// --- Hero Matchups ---

@Serializable
data class HeroVsHeroMatchup(
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
