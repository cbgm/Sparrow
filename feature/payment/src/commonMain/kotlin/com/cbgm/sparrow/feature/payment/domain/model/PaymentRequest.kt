package com.cbgm.sparrow.feature.payment.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer

/** Immutable request; execution state belongs to the composer repository. */
@ConsistentCopyVisibility
data class PaymentRequest internal constructor(
    val id: String,
    val groupId: String,
    val event: PaymentTransfer
)
