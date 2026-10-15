package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.PaymentTransferDto
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus

internal fun PaymentTransferDto.toPaymentTransfer(): PaymentTransfer =
    PaymentTransfer(id, transferId, boardId, fromMemberId, toMemberId, amountMinor, currencyCode, reference, PaymentTransferStatus.valueOf(status), occurredAtEpochMilliseconds)
        .also(PaymentTransferPolicy::requireValid)

internal fun PaymentTransfer.toPaymentTransferDto(): PaymentTransferDto =
    PaymentTransferDto(id, transferId, boardId, fromMemberId, toMemberId, amountMinor, currencyCode, reference, status.name, occurredAtEpochMilliseconds)
