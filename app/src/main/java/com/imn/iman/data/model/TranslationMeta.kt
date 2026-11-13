package com.kaizen.khushu.data.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TranslationMeta(
    val id: String,
    val langCode: String,
    val langName: String,
    val translatorName: String,
    val isRtl: Boolean,
    val downloadUrl: String,
    val sizeKb: Int,
) {
    companion object {
        fun fromFawazJson(obj: JsonObject): TranslationMeta {
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val lang = obj["language"]?.jsonPrimitive?.content ?: ""
            val langIso = obj["language_iso"]?.jsonPrimitive?.content ?: lang.take(3)
            val direction = obj["direction"]?.jsonPrimitive?.content ?: "ltr"
            val url = obj["download_url"]?.jsonPrimitive?.content ?: ""
            return TranslationMeta(
                id = id,
                langCode = langIso,
                langName = lang.replaceFirstChar { it.uppercase() },
                translatorName = obj["author"]?.jsonPrimitive?.content
                    ?: obj["name"]?.jsonPrimitive?.content ?: id,
                isRtl = direction == "rtl",
                downloadUrl = url,
                sizeKb = 0,   // fawazahmed0 doesn't expose size; estimate after download
            )
        }
    }
}
