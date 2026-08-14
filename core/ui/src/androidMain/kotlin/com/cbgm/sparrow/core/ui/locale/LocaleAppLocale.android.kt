package com.cbgm.sparrow.core.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null

    actual val current: String
        @Composable get() = LocalLocale.current.platformLocale.toLanguageTag()

    @Composable
    actual infix fun provides(
        value: String?
    ): ProvidedValue<*> {
        val currentConfiguration = LocalConfiguration.current

        if (defaultLocale == null) {
            defaultLocale = LocalLocale.current.platformLocale
        }

        val newLocale =
            value
                ?.let(Locale::forLanguageTag)
                ?: requireNotNull(defaultLocale)

        Locale.setDefault(newLocale)

        val newConfiguration =
            Configuration(
                currentConfiguration
            ).apply {
                setLocale(newLocale)
            }

        val resources = LocalResources.current

        resources.updateConfiguration(
            newConfiguration,
            resources.displayMetrics
        )

        return LocalConfiguration.provides(
            newConfiguration
        )
    }
}
