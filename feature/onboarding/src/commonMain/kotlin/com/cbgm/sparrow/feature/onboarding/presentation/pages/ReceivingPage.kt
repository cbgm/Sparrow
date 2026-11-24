package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowLink
import com.cbgm.sparrow.core.ui.device.background.BackgroundRestrictionHelp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingChoiceRow
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic.ReceivingGraphic
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_onion_relay
import com.cbgm.sparrow.resources.feature_onboarding_onion_you
import com.cbgm.sparrow.resources.feature_onboarding_receiving_question
import com.cbgm.sparrow.resources.feature_onboarding_receiving_title
import com.cbgm.sparrow.resources.feature_receiving_always
import com.cbgm.sparrow.resources.feature_receiving_always_consequence
import com.cbgm.sparrow.resources.feature_receiving_battery_declined
import com.cbgm.sparrow.resources.feature_receiving_battery_hint
import com.cbgm.sparrow.resources.feature_receiving_ios_note
import com.cbgm.sparrow.resources.feature_receiving_while_open
import com.cbgm.sparrow.resources.feature_receiving_while_open_consequence
import com.cbgm.sparrow.resources.feature_receiving_why
import com.cbgm.sparrow.resources.feature_receiving_why_text
import org.jetbrains.compose.resources.stringResource

/**
 * "While Sparrow is closed…": always connected (preselected) or only while open; the graphic shows what the choice
 * means. Swiping on saves the preselected choice. "Check now and then" is offered in the settings only; iOS shows a note.
 */
@Composable
fun ReceivingPage(
    state: OnboardingUiState,
    active: Boolean,
    onChoose: (ConnectionMode) -> Unit
) {
    val clock = rememberPageClock(active)
    val alwaysConnected = state.connectsWhileClosed && state.connectionMode == ConnectionMode.ALWAYS_CONNECTED
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.receivingGraphicHeight,
        graphic = {
            ReceivingGraphic(
                alwaysConnected = alwaysConnected,
                youLabel = stringResource(Res.string.feature_onboarding_onion_you),
                relayLabel = stringResource(Res.string.feature_onboarding_onion_relay),
                modifier = Modifier.fillMaxSize()
            )
        }
    ) {
        FadingText(
            text = stringResource(Res.string.feature_onboarding_receiving_title),
            clock = clock,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        FadingText(
            text = stringResource(if (state.connectsWhileClosed) Res.string.feature_onboarding_receiving_question else Res.string.feature_receiving_ios_note),
            clock = clock,
            startMillis = QUESTION_START_MILLISECONDS,
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.connectsWhileClosed) {
            Spacer(Modifier.height(MaterialTheme.spacing.large))
            Choices(state = state, clock = clock, onChoose = onChoose)
        }
    }
}

@Composable
private fun Choices(
    state: OnboardingUiState,
    clock: State<Long>,
    onChoose: (ConnectionMode) -> Unit
) {
    Column(modifier = Modifier.graphicsLayer { alpha = clock.progress(CHOICES_START_MILLISECONDS, CHOICES_MILLISECONDS) }) {
        OnboardingChoiceRow(
            title = stringResource(Res.string.feature_receiving_always),
            consequence = stringResource(Res.string.feature_receiving_always_consequence),
            selected = state.connectionMode == ConnectionMode.ALWAYS_CONNECTED,
            onClick = { onChoose(ConnectionMode.ALWAYS_CONNECTED) }
        )
        HorizontalDivider(thickness = Dimens.OnboardingScreen.lineWidth, color = MaterialTheme.colorScheme.divider)
        OnboardingChoiceRow(
            title = stringResource(Res.string.feature_receiving_while_open),
            consequence = stringResource(Res.string.feature_receiving_while_open_consequence),
            selected = state.connectionMode == ConnectionMode.WHILE_OPEN,
            onClick = { onChoose(ConnectionMode.WHILE_OPEN) }
        )
        Column(
            modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            if (state.connectionMode == ConnectionMode.ALWAYS_CONNECTED) {
                Note(text = stringResource(Res.string.feature_receiving_battery_hint))
                BackgroundRestrictionHelp()
            }
            if (state.batteryExemptionDeclined) {
                Note(text = stringResource(Res.string.feature_receiving_battery_declined), color = MaterialTheme.colorScheme.error)
            }
            WhyIsThat()
        }
    }
}

@Composable
private fun WhyIsThat() {
    var expanded by remember { mutableStateOf(false) }
    Column {
        SparrowLink(text = stringResource(Res.string.feature_receiving_why), onClick = { expanded = !expanded })
        AnimatedVisibility(visible = expanded) {
            Note(text = stringResource(Res.string.feature_receiving_why_text), modifier = Modifier.padding(top = MaterialTheme.spacing.small))
        }
    }
}

@Composable
private fun Note(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline
) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

@Preview
@Composable
private fun ReceivingPagePreview() {
    SparrowTheme {
        ReceivingPage(
            state = OnboardingUiState(page = OnboardingPage.RECEIVING),
            active = true,
            onChoose = {}
        )
    }
}

private const val QUESTION_START_MILLISECONDS = 500L
private const val CHOICES_START_MILLISECONDS = 1_100L
private const val CHOICES_MILLISECONDS = 800L
