package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.model.PaymentMember
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSettlement

internal fun ExpenseOverview.toPaymentContext(): PaymentContext = PaymentContext(
    boardId = board.id,
    currencyCode = board.currencyCode,
    localMemberId = localMemberId,
    members = members.map { PaymentMember(it.id, it.displayName) },
    settlements = settlements.map {
        PaymentSettlement(it.fromMemberId, it.toMemberId, it.amountMinor)
    },
    transfers = transfers
)
