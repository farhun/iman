package com.imn.iman.ui.theme

import androidx.compose.ui.graphics.Color

object ImanColors {
    // Card Accents (Shared across Learn and Tasbeeh)
    val SalahBasics = Color(0xFF3B4A6B)
    val Fatiha = Color(0xFF4A3B6B)
    val PrayerTimes = Color(0xFF2E5A4E) // Deep Emerald
    val Wudu = Color(0xFF8B5A4B) // Burnished Terracotta
    val Qibla = Color(0xFF3B6B6B)
    val Friday = Color(0xFF6B4C7A) // Muted Amethyst
    
    val MorningAdhkar = Color(0xFF4A5568) // Steel Slate
    val EveningAdhkar = Color(0xFFD4AF37) // Metallic Gold
    val AfterSalahDua = Color(0xFF3B4D61) // Twilight Blue

    // Collection Accents (Tasbeeh & Learn Lists)
    val Palette = listOf(
        Color(0xFFD4AF37), // Metallic Gold
        Color(0xFF2E5A4E), // Deep Emerald
        Color(0xFF4A5568), // Steel Slate
        Color(0xFF6B4C7A), // Muted Amethyst
        Color(0xFF8B5A4B), // Burnished Terracotta
        Color(0xFF3B4D61), // Twilight Blue
        Color(0xFF3B4A6B), // Deep Blue
        Color(0xFF4A3B6B), // Deep Purple
    )

    fun getAccent(index: Int): Color = Palette[index % Palette.size]
}

val prayerCardPalette = listOf(
    "Salah Basics" to ImanColors.SalahBasics,
    "Surah Al-Fatiha" to ImanColors.Fatiha,
    "Prayer Times" to ImanColors.PrayerTimes,
    "Wudu Guide" to ImanColors.Wudu,
    "Qibla Direction" to ImanColors.Qibla,
    "Friday Prayer" to ImanColors.Friday,
)

val duaCardPalette = listOf(
    "Morning Adhkar" to ImanColors.MorningAdhkar,
    "Evening Adhkar" to ImanColors.EveningAdhkar,
    "Dua After Salah" to ImanColors.AfterSalahDua,
)
