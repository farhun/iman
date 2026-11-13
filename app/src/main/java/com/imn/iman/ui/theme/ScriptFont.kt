package com.imn.iman.ui.theme

import androidx.compose.ui.text.font.FontFamily

fun containsArabic(text: String): Boolean = text.any {
    it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F ||
        it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF
}

fun fontForText(text: String, latin: FontFamily): FontFamily =
    if (containsArabic(text)) NotoSansArabic else latin
