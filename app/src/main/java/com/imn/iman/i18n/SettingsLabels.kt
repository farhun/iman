package com.imn.iman.i18n

import androidx.annotation.StringRes
import com.imn.iman.R

@StringRes
fun methodLabelRes(id: String): Int = when (id) {
    "ALGERIA" -> R.string.settings_method_algeria
    "PORTUGAL" -> R.string.settings_method_portugal
    "TURKEY" -> R.string.settings_method_turkey
    "DUBAI" -> R.string.settings_method_dubai
    "EGYPTIAN" -> R.string.settings_method_egyptian
    "FRANCE_15" -> R.string.settings_method_france_15
    "FRANCE_18" -> R.string.settings_method_france_18
    "GULF_REGION" -> R.string.settings_method_gulf_region
    "TEHRAN" -> R.string.settings_method_tehran
    "NORTH_AMERICA" -> R.string.settings_method_north_america
    "MALAYSIA" -> R.string.settings_method_malaysia
    "JORDAN" -> R.string.settings_method_jordan
    "INDONESIA" -> R.string.settings_method_indonesia
    "KUWAIT" -> R.string.settings_method_kuwait
    "SINGAPORE" -> R.string.settings_method_singapore
    "MOON_SIGHTING_COMMITTEE" -> R.string.settings_method_moon_sighting_committee
    "MOROCCO" -> R.string.settings_method_morocco
    "MUSLIM_WORLD_LEAGUE" -> R.string.settings_method_muslim_world_league
    "QATAR" -> R.string.settings_method_qatar
    "SHIA_ITHNA_ASHARI" -> R.string.settings_method_shia_ithna_ashari
    "RUSSIA" -> R.string.settings_method_russia
    "TUNISIA" -> R.string.settings_method_tunisia
    "UMM_AL_QURA" -> R.string.settings_method_umm_al_qura
    "FRANCE_UOIF" -> R.string.settings_method_france_uoif
    "KARACHI" -> R.string.settings_method_karachi
    "LOCAL" -> R.string.settings_method_local
    "API" -> R.string.settings_method_api
    else -> 0
}

@StringRes
fun madhabLabelRes(id: String): Int = when (id) {
    "SHAFI" -> R.string.settings_madhab_standard
    "HANAFI" -> R.string.settings_madhab_hanafi
    else -> 0
}

@StringRes
fun sourceTypeLabelRes(id: String): Int = when (id) {
    "LOCAL" -> R.string.settings_source_local
    "API" -> R.string.settings_source_aladhan
    else -> 0
}


@StringRes
fun soundOptionLabelRes(id: String): Int = when (id) {
    "DEFAULT" -> R.string.settings_sound_default
    "ADHAN" -> R.string.settings_sound_adhan
    "TAKBIR" -> R.string.settings_sound_takbir
    "SILENT" -> R.string.settings_option_silent
    else -> 0
}
