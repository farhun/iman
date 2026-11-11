package com.kaizen.khushu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
@Suppress("UNUSED_PARAMETER")
fun rememberArabicScriptFontFamily(script: String, availableScripts: Set<String>): FontFamily =
    when (script) {
        "indopak" -> QuranIndoPak
        "uthmani" -> AmiriQuran
        else -> AmiriQuran
    }
