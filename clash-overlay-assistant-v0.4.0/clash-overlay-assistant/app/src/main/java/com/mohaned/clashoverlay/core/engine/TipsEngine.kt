package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.GameState

data class Tip(val text: String, val priority: Int)

class TipsEngine {
    fun evaluate(state: GameState): Tip? {
        if (!state.running) return null
        if (state.elixir.confidence >= .72f && state.elixir.current <= 1.2f) return Tip("Opponent elixir is low", 1)
        if (state.opponentDeck.count { it.confirmed } >= 8 && state.cycleConfidence >= .72f) return Tip("Opponent cycle tracked", 2)
        if (state.predictedNextCardId != null && state.cycleConfidence >= .8f) return Tip("Next cycle: ${state.predictedNextCardId}", 3)
        return null
    }
}
