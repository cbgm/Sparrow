package com.cbgm.sparrow.feature.payment.presentation.mapper

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.feature.payment.device.PaymentImageRequest
import com.cbgm.sparrow.feature.payment.domain.SepaPaymentPolicy
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSession
import com.cbgm.sparrow.feature.payment.domain.model.PreparedPayment
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentBankDetailsUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentReviewUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentSessionUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentStatusUi
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiState

internal fun PaymentSession.toUi(): PaymentSessionUi {
    val amount = SepaPaymentPolicy.amount(selection.amountMinor)
    return PaymentSessionUi(
        bankDetailsKey = bankDetailsKey,
        isPayer = isPayer,
        isRecipient = isRecipient,
        canPay = canPay,
        payerLabel = payerName,
        recipientLabel = recipientName,
        amount = amount,
        amountLabel = "$amount $currencyCode",
        reference = reference,
        status = when (status) {
            PaymentTransferStatus.SENT -> PaymentStatusUi.SENT
            PaymentTransferStatus.RECEIVED -> PaymentStatusUi.RECEIVED
            null -> PaymentStatusUi.UNPAID
        }
    )
}

internal fun PaymentSessionUi.toUiState(current: PaymentUiState): PaymentUiState {
    val state = current.copy(
        isLoading = false,
        isPayer = isPayer,
        isRecipient = isRecipient,
        canPay = canPay,
        payerLabel = payerLabel,
        recipientLabel = recipientLabel,
        amount = amount,
        amountLabel = amountLabel,
        reference = reference,
        status = status
    )
    return if (canPay) {
        state
    } else {
        state.copy(
            accountHolder = "",
            iban = "",
            isReviewing = false,
            imageRequest = null
        )
    }
}

internal fun PaymentUiState.unavailable(): PaymentUiState = copy(
    isLoading = false,
    isBusy = false,
    isPayer = false,
    isRecipient = false,
    canPay = false,
    accountHolder = "",
    iban = "",
    isReviewing = false,
    imageRequest = null
)

internal fun BankDetails.toUi(): PaymentBankDetailsUi = PaymentBankDetailsUi(accountHolder, iban)

internal fun PaymentUiState.toBankDetails(): BankDetails = BankDetails(accountHolder, iban)

internal fun PreparedPayment.toUi(): PaymentReviewUi = PaymentReviewUi(
    accountHolder = bankDetails.accountHolder,
    iban = bankDetails.iban,
    amount = amount,
    reference = reference,
    epcPayload = epcPayload
)

internal fun PaymentStatusUi.toTransferStatus(): PaymentTransferStatus = when (this) {
    PaymentStatusUi.SENT -> PaymentTransferStatus.SENT
    PaymentStatusUi.RECEIVED -> PaymentTransferStatus.RECEIVED
    PaymentStatusUi.UNPAID -> error("Unpaid is not a payment event")
}

internal fun PaymentReviewUi.toImageRequest(requestId: Int): PaymentImageRequest = PaymentImageRequest(
    requestId = requestId,
    epcPayload = epcPayload,
    accountHolder = accountHolder,
    iban = iban,
    amount = amount,
    reference = reference
)
