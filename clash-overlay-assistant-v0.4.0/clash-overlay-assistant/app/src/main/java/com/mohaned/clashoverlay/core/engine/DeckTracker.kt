package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.CardObservation
import com.mohaned.clashoverlay.core.model.TrackedCard

class DeckTracker(private val threshold: Float = .86f) {
    private val cards = linkedMapOf<String, TrackedCard>()
    fun reset() = cards.clear()

    fun observe(o: CardObservation) {
        val old = cards[o.cardId]
        val confirmed = old?.confirmed == true || o.confidence >= threshold
        val merged = TrackedCard(o.cardId, maxOf(old?.confidence ?: 0f, o.confidence), confirmed, o.timestampMs, o.specialForm)
        if (old == null || merged.confidence >= old.confidence || confirmed) cards[o.cardId] = merged
    }

    fun correct(cardId: String, timestampMs: Long) {
        cards[cardId] = TrackedCard(cardId, 1f, true, timestampMs)
    }

    fun remove(cardId: String) { cards.remove(cardId) }
    fun contains(cardId: String) = cards.containsKey(cardId)
    fun snapshot(): List<TrackedCard> = cards.values.sortedWith(compareByDescending<TrackedCard> { it.confirmed }.thenByDescending { it.confidence }).take(8)
}
