package me.anlear.praefectus.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import me.anlear.praefectus.data.api.StratzApiClient
import me.anlear.praefectus.data.cache.tables.*
import me.anlear.praefectus.domain.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class HeroRepository(private val api: StratzApiClient) {
    private val json = Json { ignoreUnknownKeys = true }

    private val CONST_TTL = 24 * 60 * 60 * 1000L  // 24 hours
    private val STATS_TTL = 6 * 60 * 60 * 1000L    // 6 hours

    private fun isCacheValid(key: String, ttl: Long): Boolean {
        val meta = transaction {
            CacheMetadataTable.selectAll().where { CacheMetadataTable.key eq key }.firstOrNull()
        } ?: return false
        return (System.currentTimeMillis() - meta[CacheMetadataTable.updatedAt]) < ttl
    }

    private fun updateCacheTimestamp(key: String) {
        transaction {
            CacheMetadataTable.deleteWhere { CacheMetadataTable.key eq key }
            CacheMetadataTable.insert {
                it[CacheMetadataTable.key] = key
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    suspend fun getHeroes(forceRefresh: Boolean = false): List<Hero> {
        if (!forceRefresh && isCacheValid("heroes", CONST_TTL)) {
            return loadHeroesFromCache()
        }

        return try {
            val apiHeroes = api.fetchHeroes()
            transaction {
                HeroesTable.deleteAll()
                for (h in apiHeroes) {
                    HeroesTable.insert {
                        it[id] = h.id
                        it[name] = h.name
                        it[displayName] = h.displayName
                        it[shortName] = h.shortName
                        it[primaryAttribute] = h.stats?.primaryAttribute ?: "all"
                        it[attackType] = h.stats?.attackType ?: "Melee"
                        it[roles] = json.encodeToString(h.roles?.map { r -> "${r.roleId}:${r.level}" } ?: emptyList<String>())
                        it[updatedAt] = System.currentTimeMillis()
                    }
                }
            }
            updateCacheTimestamp("heroes")
            loadHeroesFromCache()
        } catch (e: Exception) {
            val cached = loadHeroesFromCache()
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    private fun loadHeroesFromCache(): List<Hero> = transaction {
        HeroesTable.selectAll().map { row ->
            val rolesStr = row[HeroesTable.roles]
            val rolesList = try {
                json.decodeFromString<List<String>>(rolesStr).mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        DotaRole.fromApi(parts[0])?.let { role ->
                            HeroRole(role, parts[1].toIntOrNull() ?: 0)
                        }
                    } else null
                }
            } catch (_: Exception) { emptyList() }

            Hero(
                id = row[HeroesTable.id],
                name = row[HeroesTable.name],
                displayName = row[HeroesTable.displayName],
                shortName = row[HeroesTable.shortName],
                primaryAttribute = HeroAttribute.fromApi(row[HeroesTable.primaryAttribute]),
                attackType = AttackType.fromApi(row[HeroesTable.attackType]),
                roles = rolesList
            )
        }
    }

    suspend fun getCurrentPatchId(forceRefresh: Boolean = false): Int {
        if (!forceRefresh && isCacheValid("patch_id", CONST_TTL)) {
            val cached = transaction {
                CacheMetadataTable.selectAll().where { CacheMetadataTable.key eq "current_patch_id" }.firstOrNull()
            }
            if (cached != null) return cached[CacheMetadataTable.updatedAt].toInt()
        }

        val versions = api.fetchGameVersions()
        val latestId = versions.maxByOrNull { it.id }?.id ?: throw Exception("No game versions found")

        transaction {
            CacheMetadataTable.deleteWhere { key eq "current_patch_id" }
            CacheMetadataTable.insert {
                it[key] = "current_patch_id"
                it[updatedAt] = latestId.toLong()
            }
        }
        updateCacheTimestamp("patch_id")
        return latestId
    }

    suspend fun getHeroStats(bracket: RankBracket, forceRefresh: Boolean = false): Map<Int, HeroStats> {
        val cacheKey = "stats_${bracket.apiName}"
        if (!forceRefresh && isCacheValid(cacheKey, STATS_TTL)) {
            return loadStatsFromCache(bracket)
        }

        return try {
            val patchId = getCurrentPatchId()
            val stats = api.fetchHeroStats(bracket.apiName, patchId)
            transaction {
                HeroStatsTable.deleteWhere {
                    (HeroStatsTable.bracket eq bracket.apiName) and (HeroStatsTable.patchId eq patchId)
                }
                for (s in stats) {
                    HeroStatsTable.insert {
                        it[heroId] = s.heroId
                        it[HeroStatsTable.bracket] = bracket.apiName
                        it[HeroStatsTable.patchId] = patchId
                        it[matchCount] = s.matchCount
                        it[winCount] = s.winCount
                        it[pickCount] = s.pickCount
                        it[banCount] = s.banCount
                        it[updatedAt] = System.currentTimeMillis()
                    }
                }
            }
            updateCacheTimestamp(cacheKey)
            loadStatsFromCache(bracket)
        } catch (e: Exception) {
            val cached = loadStatsFromCache(bracket)
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    private fun loadStatsFromCache(bracket: RankBracket): Map<Int, HeroStats> = transaction {
        HeroStatsTable.selectAll().where { HeroStatsTable.bracket eq bracket.apiName }
            .associate { row ->
                row[HeroStatsTable.heroId] to HeroStats(
                    heroId = row[HeroStatsTable.heroId],
                    matchCount = row[HeroStatsTable.matchCount],
                    winCount = row[HeroStatsTable.winCount],
                    pickCount = row[HeroStatsTable.pickCount],
                    banCount = row[HeroStatsTable.banCount]
                )
            }
    }

    suspend fun getHeroMatchups(heroId: Int, bracket: RankBracket, forceRefresh: Boolean = false): List<HeroMatchup> {
        val cacheKey = "matchups_${heroId}_${bracket.apiName}"
        if (!forceRefresh && isCacheValid(cacheKey, STATS_TTL)) {
            return loadMatchupsFromCache(heroId, bracket)
        }

        return try {
            val patchId = getCurrentPatchId()
            val matchup = api.fetchHeroMatchups(heroId, bracket.apiName, patchId)
            transaction {
                HeroMatchupsTable.deleteWhere {
                    (HeroMatchupsTable.heroId eq heroId) and
                        (HeroMatchupsTable.bracket eq bracket.apiName) and
                        (HeroMatchupsTable.patchId eq patchId)
                }
                matchup?.with?.forEach { entry ->
                    HeroMatchupsTable.insert {
                        it[HeroMatchupsTable.heroId] = heroId
                        it[againstHeroId] = entry.heroId2
                        it[HeroMatchupsTable.bracket] = bracket.apiName
                        it[HeroMatchupsTable.patchId] = patchId
                        it[HeroMatchupsTable.matchCount] = entry.matchCount
                        it[HeroMatchupsTable.winCount] = entry.winCount
                        it[isWith] = true
                        it[updatedAt] = System.currentTimeMillis()
                    }
                }
                matchup?.vs?.forEach { entry ->
                    HeroMatchupsTable.insert {
                        it[HeroMatchupsTable.heroId] = heroId
                        it[againstHeroId] = entry.heroId2
                        it[HeroMatchupsTable.bracket] = bracket.apiName
                        it[HeroMatchupsTable.patchId] = patchId
                        it[HeroMatchupsTable.matchCount] = entry.matchCount
                        it[HeroMatchupsTable.winCount] = entry.winCount
                        it[isWith] = false
                        it[updatedAt] = System.currentTimeMillis()
                    }
                }
            }
            updateCacheTimestamp(cacheKey)
            loadMatchupsFromCache(heroId, bracket)
        } catch (e: Exception) {
            val cached = loadMatchupsFromCache(heroId, bracket)
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    private fun loadMatchupsFromCache(heroId: Int, bracket: RankBracket): List<HeroMatchup> = transaction {
        HeroMatchupsTable.selectAll().where {
            (HeroMatchupsTable.heroId eq heroId) and (HeroMatchupsTable.bracket eq bracket.apiName)
        }.map { row ->
            HeroMatchup(
                heroId = row[HeroMatchupsTable.heroId],
                otherHeroId = row[HeroMatchupsTable.againstHeroId],
                matchCount = row[HeroMatchupsTable.matchCount],
                winCount = row[HeroMatchupsTable.winCount],
                isWith = row[HeroMatchupsTable.isWith]
            )
        }
    }
}
