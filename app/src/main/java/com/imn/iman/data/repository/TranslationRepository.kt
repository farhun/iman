package com.imn.iman.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object TranslationRepository {
    const val QURANENC_ENGLISH_ID = "quranenc_en"
    const val QURANENC_INDONESIAN_ID = "quranenc_id"

    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    private val json = Json { ignoreUnknownKeys = true }

    fun isDownloaded(context: Context, id: String): Boolean {
        return File(context.filesDir, "translations/$id.json").exists()
    }

    suspend fun download(context: Context, id: String, url: String, onProgress: (Float) -> Unit = {}): Boolean = try {
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "translations")
            if (!dir.exists() && !dir.mkdirs()) return@withContext false
            val destination = File(dir, "$id.json")
            val temp = File(dir, "$id.json.tmp")
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connect()
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
                    val totalSize = connection.contentLength
                    var downloaded = 0L
                    connection.inputStream.use { input ->
                        temp.outputStream().buffered().use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val bytes = input.read(buffer)
                                if (bytes < 0) break
                                output.write(buffer, 0, bytes)
                                downloaded += bytes
                                if (totalSize > 0) onProgress(downloaded.toFloat() / totalSize)
                            }
                        }
                    }
                    if (parseJson(temp.readText()).isEmpty()) return@withContext false
                    if (!temp.renameTo(destination)) return@withContext false
                    cache.remove(id)
                    true
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                false
            } finally {
                temp.delete()
            }
        }
    } catch (_: Exception) {
        false
    }

    fun load(context: Context, id: String): Map<String, String> {
        if (id.isBlank()) return emptyMap()
        cache[id]?.let { return it }
        return try {
            val content = when (id) {
                QURANENC_ENGLISH_ID -> context.assets.open("translations/en.json")
                    .bufferedReader().use { it.readText() }

                QURANENC_INDONESIAN_ID -> context.assets.open("translations/id.json")
                    .bufferedReader().use { it.readText() }

                else -> {
                    val file = File(context.filesDir, "translations/$id.json")
                    if (!file.exists()) return emptyMap()
                    file.readText()
                }
            }
            val translation = parseJson(content)
            if (translation.isEmpty()) {
                if (isBundled(id)) Log.e("TranslationRepository", "Bundled translation is empty: $id")
                return emptyMap()
            }
            cache[id] = translation
            translation
        } catch (exception: Exception) {
            if (isBundled(id)) Log.e("TranslationRepository", "Unable to load bundled translation: $id", exception)
            emptyMap()
        }
    }

    private fun isBundled(id: String): Boolean =
        id == QURANENC_ENGLISH_ID || id == QURANENC_INDONESIAN_ID

    private fun parseJson(content: String): Map<String, String> {
        val root = json.parseToJsonElement(content).jsonObject
        // Fawaz format: {"quran": [{"chapter":1,"verse":1,"text":"..."}]}
        val quran = root["quran"]?.jsonArray ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        for (item in quran) {
            val obj = item.jsonObject
            val surah = obj["chapter"]?.jsonPrimitive?.content ?: continue
            val ayah = obj["verse"]?.jsonPrimitive?.content ?: continue
            val text = obj["text"]?.jsonPrimitive?.content ?: continue
            map["$surah:$ayah"] = text
        }
        return map
    }

    fun getTranslation(map: Map<String, String>, surah: Int, ayah: Int): String? {
        return map["$surah:$ayah"]
    }

    fun getCachedMap(id: String): Map<String, String>? {
        return cache[id]
    }
}
