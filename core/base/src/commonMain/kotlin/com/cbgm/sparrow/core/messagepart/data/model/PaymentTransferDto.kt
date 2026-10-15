package com.cbgm.sparrow.core.messagepart.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PAYMENT_TRANSFER")
data class PaymentTransferDto(
    override val id: String,
    val transferId: String,
    val boardId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val reference: String,
    val status: String,
    val occurredAtEpochMilliseconds: Long
) : MessagePartDto
