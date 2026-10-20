package com.cbgm.sparrow.feature.payment.presentation.model

internal data class PaymentSessionUi(
    val bankDetailsKey: String,
    val isPayer: Boolean,
    val isRecipient: Boolean,
    val canPay: Boolean,
    val payerLabel: String,
    val recipientLabel: String,
    val amount: String,
    val amountLabel: String,
    val reference: String,
    val status: PaymentStatusUi
)
