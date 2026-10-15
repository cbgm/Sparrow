package com.cbgm.sparrow.feature.payment.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus

data class PaymentSession(
    val selection: PaymentSelection,
    val bankDetailsKey: String,
    val payerName: String,
    val recipientName: String,
    val currencyCode: String,
    val reference: String,
    val isPayer: Boolean,
    val isRecipient: Boolean,
    val canPay: Boolean,
    val sentTransfer: PaymentTransfer?,
    val status: PaymentTransferStatus?
) {
    val canConfirmReceipt: Boolean
        get() = isRecipient && status == PaymentTransferStatus.SENT
}
