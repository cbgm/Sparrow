package com.cbgm.sparrow.feature.payment.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.payment.presentation.component.PaymentBankDetailsCard
import com.cbgm.sparrow.feature.payment.presentation.component.PaymentReviewCard
import com.cbgm.sparrow.feature.payment.presentation.component.PaymentSummaryCard
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentStatusUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_payment_back
import com.cbgm.sparrow.resources.feature_payment_confirm
import com.cbgm.sparrow.resources.feature_payment_confirm_hint
import com.cbgm.sparrow.resources.feature_payment_edit
import com.cbgm.sparrow.resources.feature_payment_forget
import com.cbgm.sparrow.resources.feature_payment_manual_hint
import com.cbgm.sparrow.resources.feature_payment_mark_sent
import com.cbgm.sparrow.resources.feature_payment_review
import com.cbgm.sparrow.resources.feature_payment_share
import com.cbgm.sparrow.resources.feature_payment_share_hint
import com.cbgm.sparrow.resources.feature_payment_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaymentScreen(state: PaymentUiState, onEvent: (PaymentUiEvent) -> Unit) {
    SparrowLazyScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        stringResource(Res.string.feature_payment_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(PaymentUiEvent.Back) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(Res.string.feature_payment_back)
                        )
                    }
                },
                actions = {
                    if (state.isBusy) CircularProgressIndicator(Modifier.padding(MaterialTheme.spacing.base))
                }
            )
        }
    ) { innerPadding, listState ->
        if (state.isLoading) {
            Loading()
        } else {
            Content(
                state = state,
                listState = listState,
                innerPadding = innerPadding,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun Content(
    state: PaymentUiState,
    onEvent: (PaymentUiEvent) -> Unit,
    listState: LazyListState,
    innerPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.screenPadding),
        contentPadding = innerPadding,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item(key = "summary") { PaymentSummaryCard(state) }
        if (state.status == PaymentStatusUi.UNPAID && state.isPayer && state.canPay) {
            if (state.isReviewing) {
                item(key = "review") { PaymentReviewCard(state) }
                item(key = "share-hint") {
                    Text(
                        stringResource(Res.string.feature_payment_share_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item(key = "share") {
                    SparrowApprovalButton(
                        text = stringResource(Res.string.feature_payment_share),
                        onClick = { onEvent(PaymentUiEvent.Share) },
                        enabled = !state.isBusy && state.imageRequest == null
                    )
                }
                item(key = "mark-sent") {
                    SparrowOutlinedButton(
                        text = stringResource(Res.string.feature_payment_mark_sent),
                        onClick = { onEvent(PaymentUiEvent.MarkSent) },
                        enabled = !state.isBusy
                    )
                }
                item(key = "edit") {
                    SparrowOutlinedButton(
                        text = stringResource(Res.string.feature_payment_edit),
                        onClick = { onEvent(PaymentUiEvent.Edit) },
                        enabled = !state.isBusy
                    )
                }
            } else {
                item(key = "bank-details") { PaymentBankDetailsCard(state, onEvent) }
                item(key = "continue") {
                    SparrowApprovalButton(
                        text = stringResource(Res.string.feature_payment_review),
                        onClick = { onEvent(PaymentUiEvent.Review) },
                        enabled = !state.isBusy && state.accountHolder.isNotBlank() && state.iban.isNotBlank()
                    )
                }
            }
        }
        if (state.status == PaymentStatusUi.SENT && state.isRecipient) {
            item(key = "confirm-hint") {
                Text(
                    stringResource(Res.string.feature_payment_confirm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "confirm") {
                SparrowApprovalButton(
                    text = stringResource(Res.string.feature_payment_confirm),
                    onClick = { onEvent(PaymentUiEvent.ConfirmReceipt) },
                    enabled = !state.isBusy
                )
            }
        }
        if (state.isPayer && state.hasSavedDetails) {
            item(key = "forget") {
                SparrowOutlinedButton(
                    text = stringResource(Res.string.feature_payment_forget),
                    onClick = { onEvent(PaymentUiEvent.ForgetDetails) },
                    enabled = !state.isBusy
                )
            }
        }
        item(key = "manual-hint") {
            Text(
                stringResource(Res.string.feature_payment_manual_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.Payment.loadingIndicatorSize),
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
        )
    }
}

@Preview
@Composable
private fun PaymentScreenPreview() {
    SparrowTheme {
        PaymentScreen(
            PaymentUiState(
                isLoading = false,
                isPayer = true,
                canPay = true,
                payerLabel = "Alex",
                recipientLabel = "Sam",
                amountLabel = "24.50 EUR",
                reference = "SPARROW-preview"
            ),
            {}
        )
    }
}

@Preview
@Composable
private fun PaymentReviewPreview() {
    SparrowTheme {
        PaymentScreen(
            PaymentUiState(
                isLoading = false,
                isPayer = true,
                canPay = true,
                isReviewing = true,
                payerLabel = "Alex",
                recipientLabel = "Sam",
                amountLabel = "24.50 EUR",
                amount = "24.50",
                reference = "SPARROW-preview",
                accountHolder = "Sam Example",
                iban = "DE89370400440532013000"
            ),
            {}
        )
    }
}
