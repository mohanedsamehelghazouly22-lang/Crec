package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.TrackedCard

/** Models the four-card visible hand/cycle without ever issuing game input. */
class CycleEngine {
    private val playedOrder = ArrayDeque<String>()
    private var confidence = 0f

    fun reset() { playedOrder.clear(); confidence = 0f }

    fun observePlayed(cardId: String, confidence: Float) {
        playedOrder.remove(cardId)
        playedOrder.add(cardId)
        while (playedOrder.size > 8) playedOrder.removeFirst()
        this.confidence = (this.confidence * .75f + confidence * .25f).coerceIn(0f, 1f)
    }

    fun updateFromHand(hand: List<TrackedCard>) {
        if (hand.isEmpty()) return
        val observed = hand.map { it.cardId }.toSet()
        val consistency = hand.map { it.confidence }.average().toFloat()
        val duplicatePenalty = if (observed.size == hand.size) 1f else .65f
        confidence = ((confidence * .6f) + consistency * .4f) * duplicatePenalty
    }

    fun predictedNext(knownDeck: List<TrackedCard>, hand: List<TrackedCard>): String? {
        val inHand = hand.map { it.cardId }.toSet()
        return knownDeck.asSequence()
            .filter { it.cardId !in inHand }
            .sortedBy { playedOrder.indexOf(it.cardId).let { i -> if (i < 0) Int.MAX_VALUE else i } }
            .firstOrNull()?.cardId
    }

    fun index(): Int = playedOrder.size % 4
    fun confidence(): Float = confidence
}
