package com.cbgm.sparrow.core.ui.locale

enum class AppLanguage(
    val languageTag: String,
    val nativeName: String,
    val displayName: String
) {
    ENGLISH(
        languageTag = "en",
        nativeName = "English",
        displayName = "English"
    ),
    GERMAN(
        languageTag = "de",
        nativeName = "German",
        displayName = "Deutsch"
    )
    ;

    companion object {
        fun fromLanguageTag(
            languageTag: String?
        ): AppLanguage =
            entries.firstOrNull {
                it.languageTag == languageTag
            } ?: ENGLISH
    }
}
