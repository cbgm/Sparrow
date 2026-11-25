package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingChoiceRow
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic.ThemeGraphic
import com.cbgm.sparrow.feature.settings.presentation.overview.components.themeModeDescription
import com.cbgm.sparrow.feature.settings.presentation.overview.components.themeModeTitle
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_theme_description
import com.cbgm.sparrow.resources.feature_onboarding_theme_title
import org.jetbrains.compose.resources.stringResource

/** "Light or dark?": the app switches at once when an option is tapped. */
@Composable
fun ThemePage(
    mode: AppThemeMode,
    active: Boolean,
    onChoose: (AppThemeMode) -> Unit
) {
    val clock = rememberPageClock(active)
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.themeGraphicHeight,
        graphic = { ThemeGraphic(mode = mode, modifier = Modifier.fillMaxSize()) }
    ) {
        FadingText(
            text = stringResource(Res.string.feature_onboarding_theme_title),
            clock = clock,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        FadingText(
            text = stringResource(Res.string.feature_onboarding_theme_description),
            clock = clock,
            startMillis = DESCRIPTION_START_MILLISECONDS,
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        Column(modifier = Modifier.graphicsLayer { alpha = clock.progress(CHOICES_START_MILLISECONDS, CHOICES_MILLISECONDS) }) {
            AppThemeMode.entries.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(thickness = Dimens.OnboardingScreen.lineWidth, color = MaterialTheme.colorScheme.divider)
                }
                OnboardingChoiceRow(
                    title = themeModeTitle(option),
                    consequence = themeModeDescription(option),
                    selected = option == mode,
                    onClick = { onChoose(option) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ThemePagePreview() {
    SparrowTheme {
        ThemePage(mode = AppThemeMode.SYSTEM, active = true, onChoose = {})
    }
}

private const val DESCRIPTION_START_MILLISECONDS = 500L
private const val CHOICES_START_MILLISECONDS = 1_100L
private const val CHOICES_MILLISECONDS = 800L
