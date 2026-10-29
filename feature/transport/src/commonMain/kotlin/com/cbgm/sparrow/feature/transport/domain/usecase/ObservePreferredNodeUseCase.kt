package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import kotlinx.coroutines.flow.Flow

/** The favourite gateway's node ID, or null for automatic selection. */
class ObservePreferredNodeUseCase(
    private val preferredNodeStore: PreferredNodeStore
) {
    operator fun invoke(): Flow<String?> = preferredNodeStore.observe()
}
