package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository

/** The HTTP address of the node the app is connected to, for requests the node makes on the user's behalf. */
class GetCurrentNodeBaseUrlUseCase(
    private val repository: TransportDiagnosticsRepository
) {
    operator fun invoke(): String? = repository.diagnostics.value.currentNodeBaseUrl
}
