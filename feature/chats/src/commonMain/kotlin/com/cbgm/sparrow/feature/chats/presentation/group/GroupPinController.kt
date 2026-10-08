package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.feature.chats.domain.usecase.group.ActivateGroupExpensesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CloseGroupExpensesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.PinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.UnpinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GroupPinController(
    private val pinMessage: PinGroupMessageUseCase,
    private val unpinMessage: UnpinGroupMessageUseCase,
    private val activateExpenses: ActivateGroupExpensesUseCase,
    private val closeExpenses: CloseGroupExpensesUseCase
) {
    private lateinit var scope: CoroutineScope
    private lateinit var errors: ConversationErrors
    private lateinit var groupId: String

    internal fun bind(scope: CoroutineScope, groupId: String, errors: ConversationErrors) {
        this.scope = scope
        this.groupId = groupId
        this.errors = errors
    }

    fun pin(messageId: String) = execute("Message could not be pinned") {
        pinMessage(groupId, messageId)
    }

    fun unpin() = execute("Pinned message could not be removed") {
        unpinMessage(groupId)
    }

    fun activateExpenseBoard() = execute("Could not activate group expenses") {
        activateExpenses(groupId, "EUR")
    }

    fun closeExpenseBoard() = execute("Could not close group expenses") {
        closeExpenses(groupId)
    }

    private fun execute(fallback: String, action: suspend () -> Result<*>) {
        scope.launch { action().onFailure { errors.report(it.message ?: fallback) } }
    }
}
