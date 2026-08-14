package com.cbgm.sparrow.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(
        value: String?
    ): ProvidedValue<*>
}
