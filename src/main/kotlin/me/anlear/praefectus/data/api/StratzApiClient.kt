package me.anlear.praefectus.data.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.anlear.praefectus.data.api.models.*

class StratzApiClient(private val tokenProvider: () -> String) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@StratzApiClient.json)
        }
    }

    private suspend fun query(graphql: String): String {
        val body = buildString {
            append("""{"query":""")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), graphql))
            append("}")
        }

        val response = client.post("https://api.stratz.com/graphql") {
            header("Authorization", "Bearer ${tokenProvider()}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (response.status != HttpStatusCode.OK) {
            throw ApiException("API returned ${response.status}: ${response.bodyAsText()}")
        }
        return response.bodyAsText()
    }

    suspend fun fetchHeroes(): List<ApiHero> {
        val q = """
            query {
              constants {
                heroes {
                  id
                  name
                  displayName
                  shortName
                  stats { primaryAttribute attackType }
                  roles { roleId level }
                }
              }
            }
        """.trimIndent()

        val text = query(q)
        val resp = json.decodeFromString<GraphQlResponse<ConstantsData>>(text)
        if (resp.errors != null) throw ApiException(resp.errors.joinToString { it.message })
        return resp.data?.constants?.heroes ?: emptyList()
    }

    suspend fun fetchGameVersions(): List<ApiGameVersion> {
        val q = """
            query {
              constants {
                gameVersions { id name }
              }
            }
        """.trimIndent()

        val text = query(q)
        val resp = json.decodeFromString<GraphQlResponse<ConstantsData>>(text)
        if (resp.errors != null) throw ApiException(resp.errors.joinToString { it.message })
        return resp.data?.constants?.gameVersions ?: emptyList()
    }

    /**
     * Fetch hero win/match stats using winWeek.
     * bracketId uses RankBracket enum (HERALD, GUARDIAN, ..., IMMORTAL).
     */
    suspend fun fetchHeroStats(bracketId: String): List<ApiHeroWinEntry> {
        val q = """
            query {
              heroStats {
                winWeek(take: 1, bracketIds: [$bracketId]) {
                  heroId
                  matchCount
                  winCount
                }
              }
            }
        """.trimIndent()

        val text = query(q)
        val resp = json.decodeFromString<GraphQlResponse<HeroStatsData>>(text)
        if (resp.errors != null) throw ApiException(resp.errors.joinToString { it.message })
        return resp.data?.heroStats?.winWeek ?: emptyList()
    }

    /**
     * Fetch hero vs hero matchups.
     * Uses heroVsHeroMatchup with bracketBasicIds (may need RankBracketBasicEnum values).
     */
    suspend fun fetchHeroMatchups(heroId: Int, bracketId: String): MatchupAdvantage? {
        val q = """
            query {
              heroStats {
                heroVsHeroMatchup(heroId: $heroId, bracketBasicIds: [$bracketId]) {
                  advantage {
                    heroId
                    with { heroId2 matchCount winCount }
                    vs { heroId2 matchCount winCount }
                  }
                }
              }
            }
        """.trimIndent()

        val text = query(q)
        val resp = json.decodeFromString<GraphQlResponse<HeroStatsData>>(text)
        if (resp.errors != null) throw ApiException(resp.errors.joinToString { it.message })
        return resp.data?.heroStats?.heroVsHeroMatchup?.advantage?.firstOrNull()
    }

    fun close() {
        client.close()
    }
}

class ApiException(message: String) : Exception(message)
