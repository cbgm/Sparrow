package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.BlockedContactsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveBlockedContactsContextUseCase(
    private val observeContactBlocklist: ObserveContactBlocklistUseCase
) {
    operator fun invoke(): Flow<BlockedContactsContext> =
        observeContactBlocklist().map { blocklist ->
            BlockedContactsContext(blocklist = blocklist)
        }
}
