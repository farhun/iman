package com.kaizen.khushu.data.repository

import android.content.Context
import com.kaizen.khushu.data.model.ContentSource


import com.kaizen.khushu.data.model.TranslationMeta
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

object CatalogRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val translationCache = ConcurrentHashMap<ContentSource, List<TranslationMeta>>()

    fun translations(context: Context, source: ContentSource): List<TranslationMeta> {
        translationCache[source]?.let { return it }
        val file = when (source) {
            ContentSource.QF -> "catalogs/qf_translations.json"
            ContentSource.FAWAZ -> "catalogs/fawaz_translations.json"
            ContentSource.QURANENC -> "catalogs/quranenc_translations.json"
            else -> return emptyList()
        }
        return try {
            val raw = context.assets.open(file).bufferedReader().use { it.readText() }
            val arr = json.parseToJsonElement(raw).jsonArray
            val list = when (source) {
                ContentSource.QF -> arr.map { TranslationMeta.fromQfJson(it.jsonObject, source) }
                ContentSource.FAWAZ -> arr.map { TranslationMeta.fromFawazJson(it.jsonObject, source) }
                ContentSource.QURANENC -> arr.map { TranslationMeta.fromQuranEncJson(it.jsonObject, source) }
                else -> emptyList()
            }.filter { it.id.isNotBlank() }
            translationCache[source] = list
            list
        } catch (_: Exception) { emptyList() }
    }


    fun clearCache() {
        translationCache.clear()
    }
}
