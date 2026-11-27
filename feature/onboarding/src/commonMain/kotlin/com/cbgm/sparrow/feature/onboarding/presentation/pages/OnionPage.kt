package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic.OnionGraphic
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_onion_contact
import com.cbgm.sparrow.resources.feature_onboarding_onion_description
import com.cbgm.sparrow.resources.feature_onboarding_onion_relay
import com.cbgm.sparrow.resources.feature_onboarding_onion_title
import com.cbgm.sparrow.resources.feature_onboarding_onion_you
import org.jetbrains.compose.resources.stringResource

/** What makes Sparrow different: messages travel wrapped in layers through relays, and none of them sees both ends. */
@Composable
fun OnionPage(active: Boolean) {
    val clock = rememberPageClock(active)
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.onionGraphicHeight,
        graphic = {
            OnionGraphic(
                youLabel = stringResource(Res.string.feature_onboarding_onion_you),
                relayLabel = stringResource(Res.string.feature_onboarding_onion_relay),
                contactLabel = stringResource(Res.string.feature_onboarding_onion_contact),
                modifier = Modifier.fillMaxSize()
            )
        }
    ) {
        FadingText(
            text = stringResource(Res.string.feature_onboarding_onion_title),
            clock = clock,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground,
            highlightColor = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        FadingText(
            text = stringResource(Res.string.feature_onboarding_onion_description),
            clock = clock,
            startMillis = DESCRIPTION_START_MILLISECONDS,
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun OnionPagePreview() {
    SparrowTheme { OnionPage(active = true) }
}

private const val DESCRIPTION_START_MILLISECONDS = 700L
