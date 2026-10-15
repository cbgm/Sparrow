package com.cbgm.sparrow.feature.payment.domain

import com.cbgm.sparrow.feature.payment.domain.model.BankDetails
import com.cbgm.sparrow.feature.payment.domain.model.SepaPayment

/** EPC069-12 v3.1: version 002, UTF-8, SCT; BIC omitted for EEA transfers. */
object SepaPaymentPolicy {
    private val ibanLengths = mapOf(
        "AT" to 20,
        "BE" to 16,
        "BG" to 22,
        "HR" to 21,
        "CY" to 28,
        "CZ" to 24,
        "DE" to 22,
        "DK" to 18,
        "EE" to 20,
        "FI" to 18,
        "FR" to 27,
        "GR" to 27,
        "HU" to 28,
        "IS" to 26,
        "IE" to 22,
        "IT" to 27,
        "LV" to 21,
        "LI" to 21,
        "LT" to 20,
        "LU" to 20,
        "MT" to 31,
        "NL" to 18,
        "NO" to 15,
        "PL" to 28,
        "PT" to 25,
        "RO" to 24,
        "SK" to 24,
        "SI" to 19,
        "ES" to 24,
        "SE" to 24
    )

    fun normalize(details: BankDetails): BankDetails {
        val holder = details.accountHolder.trim()
        val iban = details.iban.filterNot(Char::isWhitespace).uppercase()
        require(holder.isNotBlank() && holder.length <= 70 && holder.none { it.code < 32 || it.code == 127 }) {
            "Enter an account-holder name of at most 70 characters"
        }
        require(iban.length == ibanLengths[iban.take(2)] && Regex("[A-Z]{2}[0-9]{2}[A-Z0-9]+").matches(iban)) {
            "Enter a supported EEA IBAN"
        }
        var remainder = 0
        (iban.drop(4) + iban.take(4)).forEach { char ->
            if (char in '0'..'9') {
                remainder = (remainder * 10 + (char - '0')) % 97
            } else {
                remainder = (remainder * 100 + (char - 'A' + 10)) % 97
            }
        }
        require(remainder == 1) { "Invalid IBAN checksum" }
        return BankDetails(holder, iban)
    }

    fun amount(amountMinor: Long): String =
        "${amountMinor / 100}.${(amountMinor % 100).toString().padStart(2, '0')}"

    fun epcPayload(payment: SepaPayment): String {
        val bank = normalize(payment.bankDetails)
        require(payment.amountMinor in 1L..99_999_999_999L) { "Invalid SEPA amount" }
        require(payment.reference.matches(Regex("SPARROW-[A-Za-z0-9-]{1,60}"))) { "Invalid settlement reference" }
        return listOf(
            "BCD",
            "002",
            "1",
            "SCT",
            "",
            bank.accountHolder,
            bank.iban,
            "EUR${amount(payment.amountMinor)}",
            "",
            "",
            payment.reference
        ).joinToString("\n").also { require(it.encodeToByteArray().size <= 331) { "Payment QR data is too long" } }
    }
}
