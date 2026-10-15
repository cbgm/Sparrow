package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest

internal fun ExpenseRequest.toGroupMessageCommand(): GroupMessageCommand =
    when (this) {
        is ExpenseRequest.Create ->
            GroupMessageCommand.CreateExpense(
                groupId = groupId,
                boardId = boardId,
                description = description,
                amountMinor = amountMinor,
                paidByMemberId = paidByMemberId,
                allocations = allocations,
                category = category,
                receipt = receipt,
                conversion = conversion
            )
        is ExpenseRequest.Edit -> GroupMessageCommand.EditExpense(groupId, expense)
        is ExpenseRequest.Delete -> GroupMessageCommand.DeleteExpense(groupId, expense)
    }
