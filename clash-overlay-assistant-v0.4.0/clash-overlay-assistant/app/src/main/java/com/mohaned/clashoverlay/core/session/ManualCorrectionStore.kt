package com.mohaned.clashoverlay.core.session

import com.mohaned.clashoverlay.core.model.CardObservation

/** Session-scoped corrections. Corrections never generate screen input or control the game. */
class ManualCorrectionStore {
    private val corrections = ArrayDeque<CardObservation>()

    fun add(roiId: String, cardId: String, timestampMs: Long) {
        corrections.addLast(CardObservation(cardId, 1f, timestampMs, "manual", roiId))
    }

    fun undo(): CardObservation? = corrections.removeLastOrNull()
    fun latest(roiId: String): CardObservation? = corrections.lastOrNull { it.roiId == roiId }
    fun all(): List<CardObservation> = corrections.toList()
    fun clear() = corrections.clear()
}
