package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.circuit.DecoyCountStore

/** Saves how many decoy envelopes go out with each send; 0 switches them off. */
class SetDecoyCountUseCase(
    private val decoyCountStore: DecoyCountStore
) {
    suspend operator fun invoke(decoyCount: Int): Result<Unit> = safeSuspendCall { decoyCountStore.set(decoyCount) }
}
