package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint

class NodeControlPlaneDiscoverySynchronizer(
    private val source: NodeControlPlaneDirectorySource,
    private val candidateVerifier: ControlPlaneCandidateVerifier,
    private val configuration: ControlPlaneConfiguration
) {
    private val logger = SparrowLog.withTag("ControlPlaneDiscovery")

    suspend fun refreshFromNode(websocketUrl: String): Result<Int> =
        runCatching {
            val candidates = source.fetch(websocketUrl).getOrThrow().normalizedCandidates()
            val verified = verifyCandidates(candidates)
            check(verified.isNotEmpty()) {
                "Node did not advertise a trusted control plane"
            }
            configuration.mergeDirectory(verified).getOrThrow()
            verified.size
        }

    private suspend fun verifyCandidates(candidates: List<String>): List<String> {
        val verified = mutableListOf<String>()
        candidates.forEach { candidate ->
            candidateVerifier.verify(candidate).fold(
                onSuccess = { verified += candidate },
                onFailure = { error -> logRejectedCandidate(candidate, error) }
            )
        }
        return verified
    }

    private fun logRejectedCandidate(
        candidate: String,
        error: Throwable
    ) {
        logger.warn {
            "Ignoring untrusted control plane $candidate: " +
                (error.message ?: "verification failed")
        }
    }

    private fun List<String>.normalizedCandidates(): List<String> =
        asSequence()
            .mapNotNull(::normalizeCandidate)
            .distinct()
            .take(MAXIMUM_CONTROL_PLANES)
            .toList()

    private fun normalizeCandidate(value: String): String? =
        runCatching {
            val normalized = value.trim().trimEnd('/')
            require(
                normalized.startsWith("http://") || normalized.startsWith("https://")
            ) {
                "Control-plane URL must use http:// or https://"
            }
            ControlPlaneEndpoint(normalized).baseUrl
        }.getOrNull()

    private companion object {
        const val MAXIMUM_CONTROL_PLANES = 16
    }
}
