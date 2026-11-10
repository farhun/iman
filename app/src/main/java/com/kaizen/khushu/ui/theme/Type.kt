package com.kaizen.khushu.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import com.kaizen.khushu.R
import androidx.compose.ui.text.font.Font


val QuranIndoPak = FontFamily(Font(R.font.quran_neo_arabic))
val AmiriQuran = FontFamily(Font(R.font.amiri_quran))
val PlusJakartaSans = FontFamily(
    Font(R.font.plusjakartasans_regular, weight = FontWeight.Normal),
    Font(R.font.plusjakartasans_medium, weight = FontWeight.Medium),
    Font(R.font.plusjakartasans_semibold, weight = FontWeight.SemiBold),
    Font(R.font.plusjakartasans_bold, weight = FontWeight.Bold),
)

val Typography = createKhushuTypography(
    plusJakartaSans = PlusJakartaSans,
    quranIndopak = AmiriQuran,
)
