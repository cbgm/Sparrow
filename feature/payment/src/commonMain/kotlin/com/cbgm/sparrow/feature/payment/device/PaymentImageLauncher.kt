package com.cbgm.sparrow.feature.payment.device

import androidx.compose.runtime.Composable

/** Sensitive payload remains in memory; platforms share an explicit temporary image. */
data class PaymentImageRequest(
    val requestId: Int,
    val epcPayload: String,
    val accountHolder: String,
    val iban: String,
    val amount: String,
    val reference: String
) {
    override fun toString(): String = "PaymentImageRequest(redacted)"
}

@Composable
expect fun PaymentImageLauncher(request: PaymentImageRequest?, onComplete: (Boolean) -> Unit)
