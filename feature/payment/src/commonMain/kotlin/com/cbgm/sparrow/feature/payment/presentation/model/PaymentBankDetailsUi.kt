package com.cbgm.sparrow.feature.payment.presentation.model

internal data class PaymentBankDetailsUi(
    val accountHolder: String,
    val iban: String
) {
    override fun toString(): String = "PaymentBankDetailsUi(redacted)"
}
