package com.cbgm.sparrow.feature.payment.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.component.SparrowSwitch
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentStatusUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_amount
import com.cbgm.sparrow.resources.feature_payment_bank_details
import com.cbgm.sparrow.resources.feature_payment_copy
import com.cbgm.sparrow.resources.feature_payment_holder
import com.cbgm.sparrow.resources.feature_payment_iban
import com.cbgm.sparrow.resources.feature_payment_participants
import com.cbgm.sparrow.resources.feature_payment_received
import com.cbgm.sparrow.resources.feature_payment_reference_label
import com.cbgm.sparrow.resources.feature_payment_remember
import com.cbgm.sparrow.resources.feature_payment_sent
import com.cbgm.sparrow.resources.feature_payment_unpaid
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaymentSummaryCard(state: PaymentUiState) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Text(
            stringResource(Res.string.feature_payment_participants, state.payerLabel, state.recipientLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            state.amountLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light
        )
        Text(
            text = stringResource(
                when (state.status) {
                    PaymentStatusUi.UNPAID -> Res.string.feature_payment_unpaid
                    PaymentStatusUi.SENT -> Res.string.feature_payment_sent
                    PaymentStatusUi.RECEIVED -> Res.string.feature_payment_received
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.status == PaymentStatusUi.RECEIVED) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
internal fun PaymentBankDetailsCard(state: PaymentUiState, onEvent: (PaymentUiEvent) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        SparrowSectionLabel(text = stringResource(Res.string.feature_payment_bank_details))
        SparrowInputField(
            value = state.accountHolder,
            onValueChange = { onEvent(PaymentUiEvent.HolderChanged(it)) },
            label = stringResource(Res.string.feature_payment_holder),
            isSingleLine = true,
            isEnabled = !state.isBusy
        )
        SparrowInputField(
            value = state.iban,
            onValueChange = { onEvent(PaymentUiEvent.IbanChanged(it)) },
            label = stringResource(Res.string.feature_payment_iban),
            isSingleLine = true,
            isEnabled = !state.isBusy
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                stringResource(Res.string.feature_payment_remember),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            SparrowSwitch(
                checked = state.rememberDetails,
                onCheckedChange = { onEvent(PaymentUiEvent.RememberChanged(it)) },
                enabled = !state.isBusy
            )
        }
    }
}

@Composable
internal fun PaymentReviewCard(state: PaymentUiState) {
    Column {
        SparrowSectionLabel(text = stringResource(Res.string.feature_payment_bank_details))
        PaymentField(stringResource(Res.string.feature_payment_holder), state.accountHolder)
        PaymentField(stringResource(Res.string.feature_payment_iban), state.iban)
        PaymentField(stringResource(Res.string.feature_payment_amount), state.amount)
        PaymentField(stringResource(Res.string.feature_payment_reference_label), state.reference)
    }
}

@Suppress("DEPRECATION")
@Composable
private fun PaymentField(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                Icon(
                    SparrowIcons.Copy,
                    stringResource(Res.string.feature_payment_copy),
                    modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.divider,
            thickness = Dimens.Base.dividerThickness
        )
    }
}
