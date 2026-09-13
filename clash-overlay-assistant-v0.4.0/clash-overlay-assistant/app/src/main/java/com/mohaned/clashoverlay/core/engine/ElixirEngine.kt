package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.ElixirEstimate
import kotlin.math.max
import kotlin.math.min

/** Opponent elixir estimator. Timing follows the standard battle rules; special modes can override the phase multiplier. */
class ElixirEngine(private val nowMs: () -> Long = { System.currentTimeMillis() }) {
    private var matchStartMs = nowMs()
    private var lastMs = matchStartMs
    private var value = 5f
    private var minValue = 5f
    private var maxValue = 5f
    private var confidence = .15f
    private var phaseOverride: Float? = null

    fun reset(start: Float = 5f) {
        matchStartMs = nowMs(); lastMs = matchStartMs
        value = start.coerceIn(0f, 10f); minValue = value; maxValue = value
        confidence = .15f; phaseOverride = null
    }

    fun setPhaseMultiplier(multiplier: Float?) { phaseOverride = multiplier?.coerceIn(1f, 3f) }

    fun advance() {
        val now = nowMs(); val dt = max(0L, now - lastMs) / 1000f
        val multiplier = phaseOverride ?: standardMultiplier((now - matchStartMs) / 1000L)
        val regen = (1f / 2.8f) * multiplier
        value = min(10f, value + dt * regen)
        minValue = min(10f, minValue + dt * regen)
        maxValue = min(10f, maxValue + dt * regen)
        lastMs = now
        confidence = min(1f, confidence + dt * .006f)
    }

    fun observeSpent(cost: Int) {
        advance(); val c = cost.coerceAtLeast(0)
        value = max(0f, value - c); minValue = max(0f, minValue - c); maxValue = max(0f, maxValue - c)
        confidence = min(1f, confidence + .08f)
    }

    fun observeUnknownSpend(costMin: Int, costMax: Int) {
        advance(); val lo = costMin.coerceAtLeast(0); val hi = max(lo, costMax)
        minValue = max(0f, minValue - hi); maxValue = max(0f, maxValue - lo)
        value = ((minValue + maxValue) / 2f).coerceIn(0f, 10f)
        confidence = max(0f, confidence - .06f)
    }

    fun estimate(): ElixirEstimate {
        advance()
        return ElixirEstimate(value.coerceIn(0f, 10f), min(minValue, maxValue), max(minValue, maxValue), confidence)
    }

    fun phaseSeconds(): Long = ((nowMs() - matchStartMs).coerceAtLeast(0L) / 1000L)

    private fun standardMultiplier(seconds: Long): Float = when {
        seconds >= 240 -> 3f
        seconds >= 120 -> 2f
        else -> 1f
    }
}
