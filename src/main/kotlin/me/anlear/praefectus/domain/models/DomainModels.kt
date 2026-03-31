package me.anlear.praefectus.domain.models

data class Hero(
    val id: Int,
    val name: String,
    val displayName: String,
    val shortName: String,
    val primaryAttribute: HeroAttribute,
    val attackType: AttackType,
    val roles: List<HeroRole>
) {
    val iconUrl: String
        get() {
            val internalName = name.removePrefix("npc_dota_hero_")
            return "https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/$internalName.png"
        }
}

enum class HeroAttribute(val apiName: String, val displayShort: String) {
    STRENGTH("STRENGTH", "Str"),
    AGILITY("AGILITY", "Agi"),
    INTELLIGENCE("INTELLIGENCE", "Int"),
    UNIVERSAL("UNIVERSAL", "Uni");

    companion object {
        fun fromApi(value: String): HeroAttribute =
            entries.find { it.apiName.equals(value, ignoreCase = true) } ?: UNIVERSAL
    }
}

enum class AttackType(val apiName: String) {
    MELEE("Melee"),
    RANGED("Ranged");

    companion object {
        fun fromApi(value: String): AttackType =
            if (value.equals("Melee", ignoreCase = true)) MELEE else RANGED
    }
}

data class HeroRole(
    val roleId: DotaRole,
    val level: Int
)

enum class DotaRole(val id: String, val locKey: String) {
    CARRY("CORE", "carry"),
    MID("MID", "mid"),
    OFFLANE("OFFLANE", "offlane"),
    SUPPORT("SUPPORT", "support"),
    HARD_SUPPORT("HARD_SUPPORT", "hard_support");

    companion object {
        fun fromApi(roleId: String): DotaRole? = entries.find { it.id.equals(roleId, ignoreCase = true) }
    }
}

data class HeroStats(
    val heroId: Int,
    val matchCount: Long,
    val winCount: Long,
    val pickCount: Long,
    val banCount: Long
) {
    val winRate: Double get() = if (matchCount > 0) winCount.toDouble() / matchCount * 100 else 0.0
    val pickRate: Double get() = pickCount.toDouble()
    val banRate: Double get() = banCount.toDouble()
}

data class HeroMatchup(
    val heroId: Int,
    val otherHeroId: Int,
    val matchCount: Long,
    val winCount: Long,
    val isWith: Boolean
) {
    val winRate: Double get() = if (matchCount > 0) winCount.toDouble() / matchCount * 100 else 0.0
}

enum class RankBracket(val apiName: String, val display: String) {
    HERALD_GUARDIAN("HERALD_GUARDIAN", "Herald/Guardian"),
    CRUSADER_ARCHON("CRUSADER_ARCHON", "Crusader/Archon"),
    LEGEND_ANCIENT("LEGEND_ANCIENT", "Legend/Ancient"),
    DIVINE_IMMORTAL("DIVINE_IMMORTAL", "Divine/Immortal"),
    IMMORTAL("IMMORTAL", "Immortal");

    companion object {
        fun fromString(s: String): RankBracket =
            entries.find { it.apiName == s } ?: DIVINE_IMMORTAL
    }
}

// Draft state

enum class DraftMode { ALL_PICK, CAPTAINS_MODE }

enum class DraftTeam { RADIANT, DIRE }

enum class DraftActionType { PICK, BAN }

data class DraftAction(
    val team: DraftTeam,
    val type: DraftActionType,
    val heroId: Int? = null
)

data class DraftState(
    val mode: DraftMode = DraftMode.ALL_PICK,
    val radiantPicks: List<Int> = emptyList(),
    val direPicks: List<Int> = emptyList(),
    val bans: List<Int> = emptyList(),
    val history: List<DraftAction> = emptyList(),
    val cmStepIndex: Int = 0
) {
    val allPickedOrBanned: Set<Int>
        get() = (radiantPicks + direPicks + bans).toSet()

    fun isComplete(): Boolean = when (mode) {
        DraftMode.ALL_PICK -> radiantPicks.size >= 5 && direPicks.size >= 5
        DraftMode.CAPTAINS_MODE -> cmStepIndex >= CM_SEQUENCE.size
    }

    companion object {
        // CM draft sequence: B=Ban, P=Pick, R=Radiant, D=Dire
        val CM_SEQUENCE: List<DraftAction> = listOf(
            // Ban Phase 1
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),
            // Pick Phase 1
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),
            // Ban Phase 2
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),
            // Pick Phase 2
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),
            // Ban Phase 3
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),
            // Pick Phase 3
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),
        )
    }
}

data class HeroRecommendation(
    val hero: Hero,
    val totalScore: Double,
    val counterScore: Double,
    val synergyScore: Double,
    val metaScore: Double,
    val winRate: Double
)

enum class TierRank(val display: String) {
    S("S"), A("A"), B("B"), C("C"), D("D")
}

data class TierEntry(
    val hero: Hero,
    val stats: HeroStats,
    val tier: TierRank
)
