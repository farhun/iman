package com.kaizen.khushu.data.model

enum class ContentSource(
    val displayName: String,
    val shortLabel: String,
    val supportsTranslations: Boolean = false,
    val supportsAudio: Boolean = false,
) {
    QF(
        displayName = "Quran Foundation",
        shortLabel = "QF",
        supportsTranslations = true,
        supportsAudio = true,
    ),
    FAWAZ(
        displayName = "fawazahmed0",
        shortLabel = "fawaz",
        supportsTranslations = true,
    ),
    QURANENC(
        displayName = "QuranEnc",
        shortLabel = "QuranEnc",
        supportsTranslations = true,
    ),
    SPA5K(
        displayName = "spa5k / GitHub",
        shortLabel = "spa5k",
    ),

    EVERYAYAH(
        displayName = "everyayah.com",
        shortLabel = "everyayah",
        supportsAudio = true,
    ),
}
