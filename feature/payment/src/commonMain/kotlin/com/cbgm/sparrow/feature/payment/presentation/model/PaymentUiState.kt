package com.cbgm.sparrow.feature.payment.presentation.model

import com.cbgm.sparrow.feature.payment.device.PaymentImageRequest

enum class PaymentStatusUi { UNPAID, SENT, RECEIVED }

enum class PaymentErrorUi { LOAD, INVALID_DETAILS, SAVE, SEND, SHARE, STALE }

data class PaymentUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isPayer: Boolean = false,
    val isRecipient: Boolean = false,
    val canPay: Boolean = false,
    val recipientLabel: String = "",
    val payerLabel: String = "",
    val amountLabel: String = "",
    val amount: String = "",
    val reference: String = "",
    val accountHolder: String = "",
    val iban: String = "",
    val rememberDetails: Boolean = false,
    val hasSavedDetails: Boolean = false,
    val isReviewing: Boolean = false,
    val status: PaymentStatusUi = PaymentStatusUi.UNPAID,
    val error: PaymentErrorUi? = null,
    val imageRequest: PaymentImageRequest? = null
) {
    override fun toString(): String = "PaymentUiState(redacted)"
}
