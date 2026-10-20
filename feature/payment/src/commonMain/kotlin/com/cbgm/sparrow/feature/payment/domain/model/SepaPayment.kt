package com.cbgm.sparrow.feature.payment.domain.model

data class SepaPayment(
    val bankDetails: BankDetails,
    val amountMinor: Long,
    val reference: String
) {
    override fun toString(): String = "SepaPayment(redacted)"
}
