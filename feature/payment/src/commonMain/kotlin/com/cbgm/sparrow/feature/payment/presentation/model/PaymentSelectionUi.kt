package com.cbgm.sparrow.feature.payment.presentation.model

internal data class PaymentSelectionUi(
    val groupId: String,
    val boardId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val transferId: String
)
