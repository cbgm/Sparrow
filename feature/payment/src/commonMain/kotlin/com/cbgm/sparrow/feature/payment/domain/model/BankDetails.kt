package com.cbgm.sparrow.feature.payment.domain.model

/** Local-only details; never a navigation argument or message payload. */
data class BankDetails(
    val accountHolder: String,
    val iban: String
) {
    override fun toString(): String = "BankDetails(redacted)"
}
