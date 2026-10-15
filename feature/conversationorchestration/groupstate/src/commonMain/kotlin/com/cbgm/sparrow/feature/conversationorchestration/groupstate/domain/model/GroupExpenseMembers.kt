package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard

/** The open expense board and the expense member IDs that may currently take part in it. */
data class GroupExpenseMembers(
    val board: ExpenseBoard,
    val memberIds: Set<String>,
    val localMemberId: String,
    val isLocalAdmin: Boolean
)
