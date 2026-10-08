package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard

data class GroupExpenseCreationContext(
    val board: ExpenseBoard,
    val localMemberId: String,
    val members: List<GroupExpenseMember>
)

data class GroupExpenseMember(
    val id: String,
    val displayName: String,
    val isLocal: Boolean
)
