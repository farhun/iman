package com.imn.iman.i18n

data class AppLanguage(val tag: String, val nativeName: String)

// Add a language by creating res/values-<code>/strings.xml and adding one line here.
val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en", "English"),
)
