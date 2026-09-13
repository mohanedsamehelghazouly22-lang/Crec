package com.mohaned.clashoverlay.settings

import android.content.Context

data class OverlaySettings(
    val compact: Boolean = true,
    val opacity: Float = .86f,
    val sensitivity: Float = .72f,
    val tipsEnabled: Boolean = true,
    val tipsFrequencySec: Int = 8,
    val performanceMode: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("overlay_settings", Context.MODE_PRIVATE)
    fun load() = OverlaySettings(
        compact = prefs.getBoolean("compact", true),
        opacity = prefs.getFloat("opacity", .86f),
        sensitivity = prefs.getFloat("sensitivity", .72f),
        tipsEnabled = prefs.getBoolean("tips", true),
        tipsFrequencySec = prefs.getInt("tips_frequency", 8),
        performanceMode = prefs.getBoolean("performance", true)
    )
    fun save(s: OverlaySettings) = prefs.edit()
        .putBoolean("compact", s.compact)
        .putFloat("opacity", s.opacity)
        .putFloat("sensitivity", s.sensitivity)
        .putBoolean("tips", s.tipsEnabled)
        .putInt("tips_frequency", s.tipsFrequencySec)
        .putBoolean("performance", s.performanceMode)
        .apply()
}
