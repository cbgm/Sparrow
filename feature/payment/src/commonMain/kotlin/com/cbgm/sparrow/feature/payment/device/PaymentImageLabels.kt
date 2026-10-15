package com.cbgm.sparrow.feature.payment.device

/** Display labels follow the active Compose resource locale, including shared images. */
data class PaymentImageLabels(
    val title: String,
    val recipient: String,
    val iban: String,
    val amount: String,
    val reference: String
)
