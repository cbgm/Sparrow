package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.discovery.NodeDirectorySource
import com.cbgm.securechat.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.securechat.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

interface ControlPlaneCandidateVerifier {
    suspend fun verify(baseUrl: String): Result<Unit>
}

class SignedDirectoryControlPlaneCandidateVerifier(
    private val nodeDirectorySource: NodeDirectorySource,
    private val json: Json,
    private val verifier: NodeDirectoryVerifier,
    private val relayTransportConfig: RelayTransportConfig
) : ControlPlaneCandidateVerifier {
    override suspend fun verify(baseUrl: String): Result<Unit> =
        runCatching {
            withTimeout(VERIFICATION_TIMEOUT_MILLISECONDS) {
                verifySignedDirectory(baseUrl)
            }
        }

    private suspend fun verifySignedDirectory(baseUrl: String) {
        val trustedRootNodeId =
            requireNotNull(relayTransportConfig.trustedRegistryRootNodeId) {
                "Control-plane discovery requires a pinned registry root"
            }
        val encodedDirectory = nodeDirectorySource.fetch(baseUrl).getOrThrow()
        val signedDirectory = json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        verifier
            .verify(
                signedDirectory = signedDirectory,
                trustedRootNodeId = trustedRootNodeId,
                supportedProtocolVersion = relayTransportConfig.supportedProtocolVersion,
                nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            ).getOrThrow()
    }

    private companion object {
        const val VERIFICATION_TIMEOUT_MILLISECONDS = 5_000L
    }
}
