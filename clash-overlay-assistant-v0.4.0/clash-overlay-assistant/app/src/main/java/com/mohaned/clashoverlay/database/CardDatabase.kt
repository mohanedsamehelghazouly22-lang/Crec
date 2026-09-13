package com.mohaned.clashoverlay.database

import android.content.Context
import com.mohaned.clashoverlay.core.model.*
import org.json.JSONObject

class CardDatabase(context: Context) {
    private val cards: List<CardDefinition>
    init {
        val raw = context.assets.open("cards.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(raw).getJSONArray("cards")
        cards = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val localized = mutableMapOf<String,String>()
                o.optJSONObject("localizedNames")?.keys()?.forEach { k -> localized[k] = o.getJSONObject("localizedNames").getString(k) }
                add(CardDefinition(
                    id=o.getString("id"), name=o.getString("name"), localizedNames=localized,
                    elixir=o.getInt("elixir"), rarity=Rarity.valueOf(o.getString("rarity")),
                    type=CardType.valueOf(o.getString("type")), champion=o.optBoolean("champion"),
                    hero=o.optBoolean("hero"), evolution=o.optBoolean("evolution"),
                    imageAsset=o.optString("imageAsset"),
                    recognitionTemplates=o.optJSONArray("recognitionTemplates")?.let { a -> List(a.length()) { j -> a.getString(j) } } ?: emptyList(),
                    aliases=o.optJSONArray("aliases")?.let { a -> List(a.length()) { j -> a.getString(j) } } ?: emptyList(),
                    variantIds=o.optJSONArray("variantIds")?.let { a -> List(a.length()) { j -> a.getString(j) } } ?: emptyList()
                ))
            }
        }
    }
    fun all(): List<CardDefinition> = cards
    fun search(query: String): List<CardDefinition> = if (query.isBlank()) cards else cards.filter { c ->
        c.name.contains(query,true) || c.id.contains(query,true) || c.aliases.any { it.contains(query,true) } || c.localizedNames.values.any { it.contains(query,true) }
    }
    fun byId(id: String): CardDefinition? = cards.firstOrNull { it.id == id }
}
