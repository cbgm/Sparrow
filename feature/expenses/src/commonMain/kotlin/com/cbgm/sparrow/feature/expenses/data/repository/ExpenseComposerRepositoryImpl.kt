package com.cbgm.sparrow.feature.expenses.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

internal class ExpenseComposerRepositoryImpl : ExpenseComposerRepository {
    private val groups = MutableStateFlow<Map<String, ExpenseGroup?>>(emptyMap())
    private val requests = Channel<ExpenseRequest>(Channel.UNLIMITED)

    override fun observeGroup(groupId: String): Flow<ExpenseGroup?> =
        groups
            .filter { groupId in it }
            .map { it[groupId] }
            .distinctUntilChanged()

    override fun publishGroup(groupId: String, group: ExpenseGroup?) {
        groups.update { it + (groupId to group) }
    }

    override fun clearGroup(groupId: String) {
        groups.update { it - groupId }
    }

    override fun observeRequests(): Flow<ExpenseRequest> = requests.receiveAsFlow()

    override suspend fun submit(request: ExpenseRequest): Result<Unit> =
        safeSuspendCall { requests.send(request) }
}
