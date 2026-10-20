package com.cbgm.sparrow.feature.payment.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class BankDetailsDto(
    val accountHolder: String,
    val iban: String
) {
    override fun toString(): String = "BankDetailsDto(redacted)"
}
