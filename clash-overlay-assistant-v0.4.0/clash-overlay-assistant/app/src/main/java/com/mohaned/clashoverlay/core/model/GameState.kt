package com.mohaned.clashoverlay.core.model

data class ElixirEstimate(val current: Float, val min: Float, val max: Float, val confidence: Float)

data class GameState(
    val matchId: String,
    val running: Boolean = false,
    val opponentDeck: List<TrackedCard> = emptyList(),
    val opponentHand: List<TrackedCard> = emptyList(),
    val predictedNextCardId: String? = null,
    val cycleIndex: Int = 0,
    val cycleConfidence: Float = 0f,
    val elixir: ElixirEstimate = ElixirEstimate(0f, 0f, 10f, 0f),
    val gamePhase: String = "UNKNOWN",
    val lastTip: String? = null,
    val fps: Float = 0f,
    val recognitionLatencyMs: Long = 0L,
    val lastUpdatedMs: Long = 0L
)
