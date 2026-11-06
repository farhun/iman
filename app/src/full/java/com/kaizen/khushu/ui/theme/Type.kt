package com.kaizen.khushu.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontSpec
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.kaizen.khushu.R
import androidx.compose.ui.text.font.Font

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val beVietnamProFont = GoogleFont("Be Vietnam Pro")

val QuranIndoPak = FontFamily(Font(R.font.quran_indopak))
val BeVietnamPro = FontFamily(
    GoogleFontSpec(googleFont = beVietnamProFont, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    GoogleFontSpec(googleFont = beVietnamProFont, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    GoogleFontSpec(googleFont = beVietnamProFont, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    GoogleFontSpec(googleFont = beVietnamProFont, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
)

val Typography = createKhushuTypography(
    beVietnamPro = BeVietnamPro,
    quranIndopak = QuranIndoPak,
)
