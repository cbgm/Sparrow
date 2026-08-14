package com.cbgm.sparrow.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key

@Composable
fun AppLocaleEnvironment(
    content: @Composable () -> Unit
) {
    val language = currentAppLanguage

    CompositionLocalProvider(
        LocalAppLocale provides language.languageTag
    ) {
        key(language.languageTag) {
            content()
        }
    }
}
