package com.cbgm.sparrow.feature.payment.presentation.mapper

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.feature.payment.domain.model.PaymentSelection
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentSelectionUi

internal fun SavedStateHandle.toPaymentSelectionUi(): PaymentSelectionUi = PaymentSelectionUi(
    groupId = requireRouteArgument<String>(AppRoute.Payment::groupId.name),
    boardId = requireRouteArgument<String>(AppRoute.Payment::boardId.name),
    fromMemberId = requireRouteArgument<String>(AppRoute.Payment::fromMemberId.name),
    toMemberId = requireRouteArgument<String>(AppRoute.Payment::toMemberId.name),
    amountMinor = requireRouteArgument<Long>(AppRoute.Payment::amountMinor.name),
    transferId = get<String>(AppRoute.Payment::transferId.name)
        ?: get<String>(DRAFT_ID_KEY)
        ?: IdGenerator.generate(prefix = "payment").also { this[DRAFT_ID_KEY] = it }
)

internal fun PaymentSelectionUi.toDomain(): PaymentSelection = PaymentSelection(
    groupId = groupId,
    boardId = boardId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountMinor = amountMinor,
    transferId = transferId
)

private const val DRAFT_ID_KEY = "paymentDraftId"
