package com.cbgm.sparrow.feature.payment.domain.model

data class PreparedPayment(
    val bankDetails: BankDetails,
    val amount: String,
    val reference: String,
    val epcPayload: String
) {
    override fun toString(): String = "PreparedPayment(bankDetails=redacted)"
}
