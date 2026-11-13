package com.imn.iman.data.repository

import android.content.Context
import com.imn.iman.data.model.TranslationMeta
import kotlinx.serialization.json.*

object CatalogRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var translationCache: List<TranslationMeta>? = null

    fun translations(context: Context): List<TranslationMeta> {
        translationCache?.let { return it }
        return try {
            val raw = context.assets.open("catalogs/fawaz_translations.json").bufferedReader().use { it.readText() }
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.map { TranslationMeta.fromFawazJson(it.jsonObject) }
                .filter { it.id.isNotBlank() }
                .also { translationCache = it }
        } catch (_: Exception) { emptyList() }
    }

    fun clearCache() {
        translationCache = null
    }
}
