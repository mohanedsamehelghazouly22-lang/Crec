package com.mohaned.clashoverlay.core.session

import com.mohaned.clashoverlay.core.engine.*
import java.util.UUID

class MatchSessionManager(
    private val deck: DeckTracker,
    private val hand: HandTracker,
    private val cycle: CycleEngine,
    private val elixir: ElixirEngine,
    val corrections: ManualCorrectionStore = ManualCorrectionStore()
) {
    var matchId: String = ""
        private set
    var running: Boolean = false
        private set

    fun start() {
        matchId = UUID.randomUUID().toString()
        running = true
        deck.reset(); hand.reset(); cycle.reset(); elixir.reset(); corrections.clear()
    }
    fun end() { running = false }
    fun resetForNewMatch() = start()
}
