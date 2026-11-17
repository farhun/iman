package com.imn.iman.data.repository

import java.util.Locale

private const val MUSLIM_WORLD_LEAGUE = "MUSLIM_WORLD_LEAGUE"
private val ISO_COUNTRY_CODES = Locale.getISOCountries().toSet()

private val FULL_CONVENTION_LIST = listOf(
    MUSLIM_WORLD_LEAGUE,
    "SHIA_ITHNA_ASHARI",
    "KARACHI",
    "NORTH_AMERICA",
    "UMM_AL_QURA",
    "EGYPTIAN",
    "TEHRAN",
    "GULF_REGION",
    "KUWAIT",
    "QATAR",
    "SINGAPORE",
    "FRANCE_UOIF",
    "TURKEY",
    "RUSSIA",
    "MOON_SIGHTING_COMMITTEE",
    "DUBAI",
    "MALAYSIA",
    "TUNISIA",
    "ALGERIA",
    "INDONESIA",
    "MOROCCO",
    "PORTUGAL",
    "JORDAN",
    "FRANCE_15",
    "FRANCE_18"
)

fun isRecognizedCountryCode(code: String?): Boolean {
    val normalized = code?.trim()?.uppercase(Locale.ROOT) ?: return false
    return normalized in ISO_COUNTRY_CODES
}

fun conventionsForCountry(code: String?): List<String> {
    val countryCode = code?.trim()?.uppercase(Locale.ROOT)
    if (countryCode == null || countryCode !in ISO_COUNTRY_CODES) return FULL_CONVENTION_LIST

    val countryConventions = when (countryCode) {
        "ID" -> listOf("INDONESIA")
        "MY" -> listOf("MALAYSIA")
        "SG" -> listOf("SINGAPORE")
        "PK", "BD", "IN", "AF" -> listOf("KARACHI")
        "SA" -> listOf("UMM_AL_QURA")
        "AE" -> listOf("DUBAI")
        "QA" -> listOf("QATAR")
        "KW" -> listOf("KUWAIT")
        "BH", "OM" -> listOf("GULF_REGION")
        "EG", "SD", "LY", "SY", "LB" -> listOf("EGYPTIAN")
        "JO" -> listOf("JORDAN")
        "TR" -> listOf("TURKEY")
        "RU" -> listOf("RUSSIA")
        "IR" -> listOf("TEHRAN", "SHIA_ITHNA_ASHARI")
        "MA" -> listOf("MOROCCO")
        "TN" -> listOf("TUNISIA")
        "DZ" -> listOf("ALGERIA")
        "PT" -> listOf("PORTUGAL")
        "FR" -> listOf("FRANCE_UOIF", "FRANCE_15", "FRANCE_18")
        "US", "CA" -> listOf("NORTH_AMERICA", "MOON_SIGHTING_COMMITTEE")
        "GB" -> listOf("MOON_SIGHTING_COMMITTEE")
        else -> emptyList()
    }
    return listOf(MUSLIM_WORLD_LEAGUE) + countryConventions
}
