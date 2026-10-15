package com.cbgm.sparrow.feature.payment.presentation.model

internal data class PaymentReviewUi(
    val accountHolder: String,
    val iban: String,
    val amount: String,
    val reference: String,
    val epcPayload: String
) {
    override fun toString(): String = "PaymentReviewUi(redacted)"
}
