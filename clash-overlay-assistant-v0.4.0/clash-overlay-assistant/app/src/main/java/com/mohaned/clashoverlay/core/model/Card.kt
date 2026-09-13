package com.mohaned.clashoverlay.core.model

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, CHAMPION }
enum class CardType { TROOP, BUILDING, SPELL }
enum class SpecialForm { NONE, EVOLUTION, HERO }

data class CardDefinition(
    val id: String,
    val name: String,
    val localizedNames: Map<String, String> = emptyMap(),
    val elixir: Int,
    val rarity: Rarity,
    val type: CardType,
    val champion: Boolean = false,
    val hero: Boolean = false,
    val evolution: Boolean = false,
    val imageAsset: String = "",
    val recognitionTemplates: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val variantIds: List<String> = emptyList()
)

data class CardObservation(
    val cardId: String,
    val confidence: Float,
    val timestampMs: Long,
    val source: String = "vision",
    val roiId: String = "",
    val specialForm: SpecialForm = SpecialForm.NONE
)

data class TrackedCard(
    val cardId: String,
    val confidence: Float,
    val confirmed: Boolean,
    val lastSeenMs: Long,
    val specialForm: SpecialForm = SpecialForm.NONE
)
