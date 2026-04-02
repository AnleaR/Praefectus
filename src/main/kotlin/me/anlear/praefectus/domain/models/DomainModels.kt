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

enum class HeroAttribute(val apiNames: List<String>, val displayShort: String) {
    STRENGTH(listOf("str", "STRENGTH"), "Str"),
    AGILITY(listOf("agi", "AGILITY"), "Agi"),
    INTELLIGENCE(listOf("int", "INTELLIGENCE"), "Int"),
    UNIVERSAL(listOf("all", "UNIVERSAL"), "Uni");

    companion object {
        fun fromApi(value: String): HeroAttribute =
            entries.find { attr -> attr.apiNames.any { it.equals(value, ignoreCase = true) } } ?: UNIVERSAL
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

enum class DotaRole(val locKey: String) {
    CARRY("carry"),
    MID("mid"),
    OFFLANE("offlane"),
    SUPPORT("support"),
    HARD_SUPPORT("hard_support");

    companion object {
        /**
         * Map STRATZ hero role tags to Dota 2 lane positions.
         * A single STRATZ tag may map to one or more lane roles.
         */
        fun fromApiTag(tag: String, level: Int): List<DotaRole> = when (tag.uppercase()) {
            "CARRY" -> listOf(CARRY)
            "NUKER" -> listOf(MID)
            "INITIATOR", "DURABLE" -> listOf(OFFLANE)
            "SUPPORT" -> if (level >= 2) listOf(SUPPORT, HARD_SUPPORT) else listOf(SUPPORT)
            else -> emptyList()
        }
    }
}

data class HeroStats(
    val heroId: Int,
    val matchCount: Long,
    val winCount: Long
) {
    val winRate: Double get() = if (matchCount > 0) winCount.toDouble() / matchCount * 100 else 0.0
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

enum class RankBracket(
    val apiName: String,
    val display: String,
    val iconIndex: Int,
    val locKey: String,
    val basicApiName: String // for bracketBasicIds (RankBracketBasicEnum)
) {
    HERALD("HERALD", "Herald", 1, "rank_herald", "HERALD_GUARDIAN"),
    GUARDIAN("GUARDIAN", "Guardian", 2, "rank_guardian", "HERALD_GUARDIAN"),
    CRUSADER("CRUSADER", "Crusader", 3, "rank_crusader", "CRUSADER_ARCHON"),
    ARCHON("ARCHON", "Archon", 4, "rank_archon", "CRUSADER_ARCHON"),
    LEGEND("LEGEND", "Legend", 5, "rank_legend", "LEGEND_ANCIENT"),
    ANCIENT("ANCIENT", "Ancient", 6, "rank_ancient", "LEGEND_ANCIENT"),
    DIVINE("DIVINE", "Divine", 7, "rank_divine", "DIVINE_IMMORTAL"),
    IMMORTAL("IMMORTAL", "Immortal", 8, "rank_immortal", "DIVINE_IMMORTAL");

    companion object {
        fun fromString(s: String): RankBracket =
            entries.find { it.apiName == s } ?: DIVINE
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
        // CM draft sequence (F=first pick, S=second pick, B=ban, P=pick):
        // FB FB SB SB FB SB SB FP SP FB FB SB SP FP FP SP SP FP FB SB FB SB FP SP
        // F=RADIANT (first pick team), S=DIRE (second pick team)
        val CM_SEQUENCE: List<DraftAction> = listOf(
            // Ban Phase 1: FB FB SB SB FB SB SB
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 1
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 2
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 3
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 4
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 5
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 6
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 7
            // Pick Phase 1: FP SP
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),  // 8
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),     // 9
            // Ban Phase 2: FB FB SB
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 10
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 11
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 12
            // Pick Phase 2: SP FP FP SP SP FP
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),     // 13
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),  // 14
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),  // 15
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),     // 16
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),     // 17
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),  // 18
            // Ban Phase 3: FB SB FB SB
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 19
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 20
            DraftAction(DraftTeam.RADIANT, DraftActionType.BAN),   // 21
            DraftAction(DraftTeam.DIRE, DraftActionType.BAN),      // 22
            // Pick Phase 3: FP SP
            DraftAction(DraftTeam.RADIANT, DraftActionType.PICK),  // 23
            DraftAction(DraftTeam.DIRE, DraftActionType.PICK),     // 24
        )
    }
}

data class HeroRecommendation(
    val hero: Hero,
    val totalScore: Double,
    val counterScore: Double,
    val synergyScore: Double,
    val supportScore: Double,
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
