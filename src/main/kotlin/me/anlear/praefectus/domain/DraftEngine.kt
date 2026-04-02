package me.anlear.praefectus.domain

import me.anlear.praefectus.domain.models.*

class DraftEngine {

    fun applyAction(state: DraftState, heroId: Int, team: DraftTeam, actionType: DraftActionType): DraftState {
        if (heroId in state.allPickedOrBanned) return state

        val action = DraftAction(team, actionType, heroId)

        return when (actionType) {
            DraftActionType.PICK -> when (team) {
                DraftTeam.RADIANT -> state.copy(
                    radiantPicks = state.radiantPicks + heroId,
                    history = state.history + action
                )
                DraftTeam.DIRE -> state.copy(
                    direPicks = state.direPicks + heroId,
                    history = state.history + action
                )
            }
            DraftActionType.BAN -> state.copy(
                bans = state.bans + heroId,
                history = state.history + action
            )
        }
    }

    fun applyCmAction(state: DraftState, heroId: Int): DraftState {
        if (state.isComplete()) return state
        if (heroId in state.allPickedOrBanned) return state

        val step = DraftState.CM_SEQUENCE[state.cmStepIndex]
        val action = step.copy(heroId = heroId)

        val newState = when (step.type) {
            DraftActionType.BAN -> state.copy(
                bans = state.bans + heroId,
                history = state.history + action,
                cmStepIndex = state.cmStepIndex + 1
            )
            DraftActionType.PICK -> when (step.team) {
                DraftTeam.RADIANT -> state.copy(
                    radiantPicks = state.radiantPicks + heroId,
                    history = state.history + action,
                    cmStepIndex = state.cmStepIndex + 1
                )
                DraftTeam.DIRE -> state.copy(
                    direPicks = state.direPicks + heroId,
                    history = state.history + action,
                    cmStepIndex = state.cmStepIndex + 1
                )
            }
        }

        return newState
    }

    fun undo(state: DraftState): DraftState {
        if (state.history.isEmpty()) return state
        val last = state.history.last()
        val heroId = last.heroId ?: return state

        val newHistory = state.history.dropLast(1)
        return when (last.type) {
            DraftActionType.BAN -> state.copy(
                bans = state.bans - heroId,
                history = newHistory,
                cmStepIndex = if (state.mode == DraftMode.CAPTAINS_MODE) state.cmStepIndex - 1 else state.cmStepIndex
            )
            DraftActionType.PICK -> when (last.team) {
                DraftTeam.RADIANT -> state.copy(
                    radiantPicks = state.radiantPicks - heroId,
                    history = newHistory,
                    cmStepIndex = if (state.mode == DraftMode.CAPTAINS_MODE) state.cmStepIndex - 1 else state.cmStepIndex
                )
                DraftTeam.DIRE -> state.copy(
                    direPicks = state.direPicks - heroId,
                    history = newHistory,
                    cmStepIndex = if (state.mode == DraftMode.CAPTAINS_MODE) state.cmStepIndex - 1 else state.cmStepIndex
                )
            }
        }
    }

    /**
     * Remove a specific hero from the draft without affecting other heroes.
     */
    fun removeSpecificHero(state: DraftState, heroId: Int): DraftState {
        val removedCount = state.history.count { it.heroId == heroId }
        return state.copy(
            radiantPicks = state.radiantPicks - heroId,
            direPicks = state.direPicks - heroId,
            bans = state.bans - heroId,
            history = state.history.filter { it.heroId != heroId },
            cmStepIndex = if (state.mode == DraftMode.CAPTAINS_MODE) state.cmStepIndex - removedCount else state.cmStepIndex
        )
    }

    fun reset(mode: DraftMode): DraftState = DraftState(mode = mode)

    fun calculateTeamSynergy(
        teamPicks: List<Int>,
        matchups: Map<Int, List<HeroMatchup>>
    ): Double {
        if (teamPicks.size < 2) return 0.0

        var totalSynergy = 0.0
        var pairCount = 0

        for (i in teamPicks.indices) {
            for (j in i + 1 until teamPicks.size) {
                val heroMatchups = matchups[teamPicks[i]]
                val withMatchup = heroMatchups?.find { it.otherHeroId == teamPicks[j] && it.isWith }
                if (withMatchup != null && withMatchup.matchCount > 0) {
                    totalSynergy += withMatchup.winRate
                    pairCount++
                }
            }
        }

        return if (pairCount > 0) totalSynergy / pairCount else 50.0
    }
}
