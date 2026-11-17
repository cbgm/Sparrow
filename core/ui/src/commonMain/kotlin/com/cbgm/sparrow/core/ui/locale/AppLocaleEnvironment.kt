package com.cbgm.sparrow.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key

@Composable
fun AppLocaleEnvironment(
    content: @Composable () -> Unit
) {
    val language by currentAppLanguage.collectAsState()

    CompositionLocalProvider(
        LocalAppLocale provides language.languageTag
    ) {
        key(language.languageTag) {
            content()
        }
    }
}
