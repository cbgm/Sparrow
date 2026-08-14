package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

interface ControlPlaneCandidateVerifier {
    suspend fun verify(baseUrl: String): Result<Unit>
}

class SignedDirectoryControlPlaneCandidateVerifier(
    private val nodeDirectorySource: NodeDirectorySource,
    private val json: Json,
    private val verifier: NodeDirectoryVerifier,
    private val transportConfig: TransportConfig
) : ControlPlaneCandidateVerifier {
    override suspend fun verify(baseUrl: String): Result<Unit> =
        runCatching {
            withTimeout(VERIFICATION_TIMEOUT_MILLISECONDS) {
                verifySignedDirectory(baseUrl)
            }
        }

    private suspend fun verifySignedDirectory(baseUrl: String) {
        val trustedRootNodeId =
            requireNotNull(transportConfig.trustedRegistryRootNodeId) {
                "Control-plane discovery requires a pinned registry root"
            }
        val encodedDirectory = nodeDirectorySource.fetch(baseUrl).getOrThrow()
        val signedDirectory = json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        verifier
            .verify(
                signedDirectory = signedDirectory,
                trustedRootNodeId = trustedRootNodeId,
                supportedProtocolVersion = transportConfig.supportedProtocolVersion,
                nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            ).getOrThrow()
    }

    private companion object {
        const val VERIFICATION_TIMEOUT_MILLISECONDS = 5_000L
    }
}
