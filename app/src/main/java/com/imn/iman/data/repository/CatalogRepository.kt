package com.imn.iman.data.repository

import android.content.Context
import com.imn.iman.data.model.TranslationMeta
import kotlinx.serialization.json.*

object CatalogRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var translationCache: List<TranslationMeta>? = null

    fun translations(context: Context): List<TranslationMeta> {
        val downloaded = translationCache ?: try {
            val raw = context.assets.open("catalogs/fawaz_translations.json").bufferedReader().use { it.readText() }
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.map { TranslationMeta.fromFawazJson(it.jsonObject) }
                .filter { it.id.isNotBlank() }
                .also { translationCache = it }
        } catch (_: Exception) {
            emptyList()
        }
        return bundledTranslations() + downloaded
    }

    private fun bundledTranslations(): List<TranslationMeta> = listOf(
        TranslationMeta(
            id = TranslationRepository.QURANENC_ENGLISH_ID,
            langCode = "en",
            langName = "English",
            translatorName = "English",
            isRtl = false,
            downloadUrl = "",
            sizeKb = 0,
            isBundled = true,
            sourcePublisher = "QuranEnc.com",
            sourceVersion = "2025-06-24 16:37:27 (v1.1.2-csv.1)",
        ),
        TranslationMeta(
            id = TranslationRepository.QURANENC_INDONESIAN_ID,
            langCode = "in",
            langName = "Bahasa Indonesia",
            translatorName = "Bahasa Indonesia",
            isRtl = false,
            downloadUrl = "",
            sizeKb = 0,
            isBundled = true,
            sourcePublisher = "QuranEnc.com",
            sourceVersion = "2025-06-23 10:58:32 (v1.0.1-csv.1)",
        ),
    )

    fun clearCache() {
        translationCache = null
    }
}
