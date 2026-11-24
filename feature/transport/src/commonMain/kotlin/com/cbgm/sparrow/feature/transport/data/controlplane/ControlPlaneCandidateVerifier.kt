package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.directory.SignedNodeDirectory
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

internal interface ControlPlaneCandidateVerifier {
    suspend fun verify(baseUrl: String): Result<Unit>
}

internal class SignedDirectoryControlPlaneCandidateVerifier(
    private val nodeDirectorySource: NodeDirectorySource,
    private val json: Json,
    private val verifier: NodeDirectoryVerifier,
    private val transportConfig: TransportConfig
) : ControlPlaneCandidateVerifier {
    override suspend fun verify(baseUrl: String): Result<Unit> =
        runCatching {
            withTimeout(VERIFICATION_TIMEOUT_MILLISECONDS.milliseconds) {
                verifySignedDirectory(baseUrl)
            }
        }

    private suspend fun verifySignedDirectory(baseUrl: String) {
        val encodedDirectory = nodeDirectorySource.fetch(baseUrl).getOrThrow()
        val signedDirectory = json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        val trustedRootNodeId =
            transportConfig.trustedRegistryRootNodeId
                ?: verifier.rootNodeId(signedDirectory).getOrThrow()
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
