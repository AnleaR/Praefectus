package me.anlear.praefectus.data.cache.tables

import org.jetbrains.exposed.sql.Table

object HeroesTable : Table("heroes") {
    val id = integer("id")
    val name = text("name")
    val displayName = text("display_name")
    val shortName = text("short_name")
    val primaryAttribute = text("primary_attribute")
    val attackType = text("attack_type")
    val roles = text("roles") // JSON array
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object HeroStatsTable : Table("hero_stats") {
    val heroId = integer("hero_id")
    val bracket = text("bracket")
    val patchId = integer("patch_id")
    val matchCount = long("match_count")
    val winCount = long("win_count")
    val pickCount = long("pick_count")
    val banCount = long("ban_count")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(heroId, bracket, patchId)
}

object HeroMatchupsTable : Table("hero_matchups") {
    val heroId = integer("hero_id")
    val againstHeroId = integer("against_hero_id")
    val bracket = text("bracket")
    val patchId = integer("patch_id")
    val matchCount = long("match_count")
    val winCount = long("win_count")
    val isWith = bool("is_with")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(heroId, againstHeroId, bracket, patchId, isWith)
}

object CacheMetadataTable : Table("cache_metadata") {
    val key = text("key")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(key)
}
