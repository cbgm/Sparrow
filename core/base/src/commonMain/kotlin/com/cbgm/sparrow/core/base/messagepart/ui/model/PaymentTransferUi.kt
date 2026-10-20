package com.cbgm.sparrow.core.base.messagepart.ui.model

data class PaymentTransferUi(
    override val id: String,
    val amountMinor: Long,
    val currencyCode: String,
    val isReceiptConfirmed: Boolean,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi
