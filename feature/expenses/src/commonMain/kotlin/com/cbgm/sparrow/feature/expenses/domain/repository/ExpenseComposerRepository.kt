package com.cbgm.sparrow.feature.expenses.domain.repository

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import kotlinx.coroutines.flow.Flow

interface ExpenseComposerRepository {
    fun observeGroup(groupId: String): Flow<ExpenseGroup?>

    fun publishGroup(groupId: String, group: ExpenseGroup?): Result<Unit>

    fun clearGroup(groupId: String): Result<Unit>

    fun observeRequests(): Flow<ExpenseRequest>

    suspend fun submit(request: ExpenseRequest): Result<Unit>
}
