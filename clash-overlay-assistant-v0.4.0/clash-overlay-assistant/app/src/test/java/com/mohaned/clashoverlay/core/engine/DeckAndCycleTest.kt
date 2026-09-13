package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.CardObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckAndCycleTest {
    @Test fun deckKeepsAtMostEightCards() {
        val deck = DeckTracker()
        (1..10).forEach { deck.observe(CardObservation("c$it", .95f, it.toLong())) }
        assertEquals(8, deck.snapshot().size)
    }

    @Test fun cycleMovesPlayedCardToBack() {
        val cycle = CycleEngine()
        cycle.observePlayed("a", .95f)
        cycle.observePlayed("b", .95f)
        cycle.observePlayed("a", .95f)
        assertEquals("b", cycle.predictedNext())
        assertTrue(cycle.confidence() > .7f)
    }
}
