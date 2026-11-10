package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_continue_action
import com.cbgm.sparrow.resources.feature_receiving_always
import com.cbgm.sparrow.resources.feature_receiving_always_consequence
import com.cbgm.sparrow.resources.feature_receiving_battery_declined
import com.cbgm.sparrow.resources.feature_receiving_description
import com.cbgm.sparrow.resources.feature_receiving_ios_note
import com.cbgm.sparrow.resources.feature_receiving_title
import com.cbgm.sparrow.resources.feature_receiving_while_open
import com.cbgm.sparrow.resources.feature_receiving_while_open_consequence
import com.cbgm.sparrow.resources.feature_receiving_why
import com.cbgm.sparrow.resources.feature_receiving_why_text
import org.jetbrains.compose.resources.stringResource

/**
 * "Receiving messages": how messages reach the device while Sparrow is closed. Two choices with their consequence on
 * one line each; "check now and then" is offered in the settings only. On iOS only a note.
 */
@Composable
fun ReceivingPage(
    state: OnboardingUiState,
    onChoose: (ConnectionMode) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.feature_receiving_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (state.connectsWhileClosed) {
            Choices(state = state, onChoose = onChoose)
        } else {
            Description(text = stringResource(Res.string.feature_receiving_ios_note))
        }
        SparrowApprovalButton(
            onClick = onNext,
            enabled = state.connectionMode != null || !state.connectsWhileClosed,
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small),
            text = stringResource(Res.string.base_continue_action)
        )
    }
}

@Composable
private fun Choices(
    state: OnboardingUiState,
    onChoose: (ConnectionMode) -> Unit
) {
    Description(text = stringResource(Res.string.feature_receiving_description))
    ChoiceCard(
        title = stringResource(Res.string.feature_receiving_always),
        consequence = stringResource(Res.string.feature_receiving_always_consequence),
        selected = state.connectionMode == ConnectionMode.ALWAYS_CONNECTED,
        onClick = { onChoose(ConnectionMode.ALWAYS_CONNECTED) }
    )
    ChoiceCard(
        title = stringResource(Res.string.feature_receiving_while_open),
        consequence = stringResource(Res.string.feature_receiving_while_open_consequence),
        selected = state.connectionMode == ConnectionMode.WHILE_OPEN,
        onClick = { onChoose(ConnectionMode.WHILE_OPEN) }
    )
    if (state.batteryExemptionDeclined) {
        Text(
            text = stringResource(Res.string.feature_receiving_battery_declined),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    WhyIsThat()
}

@Composable
private fun ChoiceCard(
    title: String,
    consequence: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(Dimens.Base.borderStrokeWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = consequence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WhyIsThat() {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(text = stringResource(Res.string.feature_receiving_why))
    }
    AnimatedVisibility(visible = expanded) {
        Description(text = stringResource(Res.string.feature_receiving_why_text))
    }
}

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun ReceivingPagePreview() {
    SparrowTheme {
        ReceivingPage(
            state = OnboardingUiState(page = OnboardingPage.RECEIVING, connectionMode = ConnectionMode.ALWAYS_CONNECTED),
            onChoose = {},
            onNext = {}
        )
    }
}
