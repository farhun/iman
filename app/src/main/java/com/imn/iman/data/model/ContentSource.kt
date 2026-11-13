package com.imn.iman.data.model

enum class ContentSource(
    val displayName: String,
    val shortLabel: String,
    val supportsAudio: Boolean = false,
) {
    QF(
        displayName = "Quran Foundation",
        shortLabel = "QF",
        supportsAudio = true,
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
