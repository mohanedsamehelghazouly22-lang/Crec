package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.CardObservation
import com.mohaned.clashoverlay.core.model.TrackedCard

class HandTracker(private val threshold: Float = .84f) {
    private val slots = arrayOfNulls<TrackedCard>(4)
    fun reset() = java.util.Arrays.fill(slots, null)

    fun observe(o: CardObservation) {
        if (o.confidence < threshold) return
        val existing = slots.indexOfFirst { it?.cardId == o.cardId }
        val target = if (existing >= 0) existing else slots.indexOfFirst { it == null }.takeIf { it >= 0 } ?: lowestConfidenceIndex()
        slots[target] = TrackedCard(o.cardId, o.confidence, true, o.timestampMs, o.specialForm)
    }

    fun observeSlot(slot: Int, o: CardObservation) {
        if (slot !in 0..3 || o.confidence < threshold) return
        slots[slot] = TrackedCard(o.cardId, o.confidence, true, o.timestampMs, o.specialForm)
    }

    fun markPlayed(cardId: String) {
        val index = slots.indexOfFirst { it?.cardId == cardId }
        if (index >= 0) slots[index] = null
    }

    fun correct(slot: Int, cardId: String, timestampMs: Long) {
        if (slot in 0..3) slots[slot] = TrackedCard(cardId, 1f, true, timestampMs)
    }

    fun snapshot(): List<TrackedCard> = slots.filterNotNull()
    private fun lowestConfidenceIndex(): Int = slots.indices.minByOrNull { slots[it]?.confidence ?: -1f } ?: 0
}
