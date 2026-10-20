package com.cbgm.sparrow.feature.payment.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer

/** Published by the hosting conversation; contains no bank details. */
data class PaymentContext(
    val boardId: String,
    val currencyCode: String,
    val localMemberId: String,
    val members: List<PaymentMember>,
    val settlements: List<PaymentSettlement>,
    val transfers: List<PaymentTransfer>
)

data class PaymentMember(
    val id: String,
    val displayName: String
)

data class PaymentSettlement(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long
)
