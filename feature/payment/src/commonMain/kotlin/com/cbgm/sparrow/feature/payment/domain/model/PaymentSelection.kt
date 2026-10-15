package com.cbgm.sparrow.feature.payment.domain.model

data class PaymentSelection(
    val groupId: String,
    val boardId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val transferId: String
)
