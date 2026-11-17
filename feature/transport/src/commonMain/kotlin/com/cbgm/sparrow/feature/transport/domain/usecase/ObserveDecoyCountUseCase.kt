package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.DecoyCountStore
import kotlinx.coroutines.flow.Flow

/** How many decoy envelopes the user chose to send with each send (0: off). */
class ObserveDecoyCountUseCase(
    private val decoyCountStore: DecoyCountStore
) {
    operator fun invoke(): Flow<Int> = decoyCountStore.observe()
}
