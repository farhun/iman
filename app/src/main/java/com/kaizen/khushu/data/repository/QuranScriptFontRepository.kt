package com.kaizen.khushu.data.repository

import android.content.Context

object QuranScriptFontRepository {
    private val bundledScripts = setOf("uthmani", "indopak", "uthmani_simple", "imlaei")

    fun bundledScripts(): Set<String> = bundledScripts

    fun availableScripts(context: Context): Set<String> = bundledScripts

    fun isDownloaded(context: Context, script: String): Boolean =
        bundledScripts.contains(script)
}
