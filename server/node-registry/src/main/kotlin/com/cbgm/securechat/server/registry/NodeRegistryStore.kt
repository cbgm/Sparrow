package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.ReplayProtection
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_HEARTBEAT_GRACE_MILLISECONDS = 90_000L
private const val DEFAULT_REPLAY_RETENTION_MILLISECONDS = 5L * 60L * 1_000L

interface NodeRegistryStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun register(descriptor: SecureChatNodeDescriptor): RegistrationResult

    suspend fun heartbeat(heartbeat: NodeHeartbeatRequest): RegistrationResult

    suspend fun healthyNodes(): List<SecureChatNodeDescriptor>

    suspend fun findHealthy(nodeId: String): SecureChatNodeDescriptor?
}

class NodeRegistryStore(
    private val supportedProtocolVersions: Set<Int> = setOf(1),
    private val heartbeatGraceMilliseconds: Long = DEFAULT_HEARTBEAT_GRACE_MILLISECONDS,
    replayRetentionMilliseconds: Long = DEFAULT_REPLAY_RETENTION_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : NodeRegistryStorage {
    private data class RegisteredNode(
        val descriptor: SecureChatNodeDescriptor,
        val lastHeartbeatAtEpochMilliseconds: Long,
        val activeConnections: Int?
    )

    private val nodes = ConcurrentHashMap<String, RegisteredNode>()
    private val replayProtection =
        ReplayProtection(
            retentionMilliseconds = replayRetentionMilliseconds,
            now = now
        )

    override val persistenceMode: String = "memory"

    override suspend fun register(descriptor: SecureChatNodeDescriptor): RegistrationResult {
        val currentTime = now()
        val rejection =
            validateNodeDescriptor(
                descriptor = descriptor,
                supportedProtocolVersions = supportedProtocolVersions,
                currentTime = currentTime
            )

        return if (rejection == null) {
            val previousLoad = nodes[descriptor.nodeId]?.activeConnections
            nodes[descriptor.nodeId] =
                RegisteredNode(
                    descriptor = descriptor.copy(activeConnections = null),
                    lastHeartbeatAtEpochMilliseconds = currentTime,
                    activeConnections = previousLoad
                )
            RegistrationResult.Accepted
        } else {
            rejection
        }
    }

    override suspend fun heartbeat(heartbeat: NodeHeartbeatRequest): RegistrationResult {
        val registered = nodes[heartbeat.nodeId]
        return when {
            registered == null ->
                RegistrationResult.Rejected("NODE_NOT_REGISTERED")

            !replayProtection.accept(
                scope = heartbeat.nodeId,
                nonce = heartbeat.nonce,
                timestampEpochMilliseconds = heartbeat.timestampEpochMilliseconds
            ) ->
                RegistrationResult.Rejected("STALE_OR_REPLAYED_HEARTBEAT")

            !ProtocolSignatures.verifyHeartbeat(heartbeat, registered.descriptor) ->
                RegistrationResult.Rejected("INVALID_SIGNATURE")

            else -> {
                nodes[heartbeat.nodeId] =
                    registered.copy(
                        lastHeartbeatAtEpochMilliseconds = now(),
                        activeConnections = heartbeat.activeConnections ?: registered.activeConnections
                    )
                RegistrationResult.Accepted
            }
        }
    }

    override suspend fun healthyNodes(): List<SecureChatNodeDescriptor> {
        val currentTime = now()
        return nodes.values
            .asSequence()
            .filter { it.descriptor.validUntilEpochMilliseconds > currentTime }
            .filter {
                currentTime - it.lastHeartbeatAtEpochMilliseconds <=
                    heartbeatGraceMilliseconds
            }.map { registered ->
                registered.descriptor.copy(
                    activeConnections = registered.activeConnections
                )
            }
            .sortedBy(SecureChatNodeDescriptor::nodeId)
            .toList()
    }

    override suspend fun findHealthy(nodeId: String): SecureChatNodeDescriptor? =
        healthyNodes().firstOrNull { descriptor ->
            descriptor.nodeId == nodeId
        }

    override fun close() = Unit
}

internal fun validateNodeDescriptor(
    descriptor: SecureChatNodeDescriptor,
    supportedProtocolVersions: Set<Int>,
    currentTime: Long
): RegistrationResult.Rejected? =
    when {
        !ProtocolSignatures.verifyDescriptor(descriptor) ->
            RegistrationResult.Rejected("INVALID_SIGNATURE")

        descriptor.validUntilEpochMilliseconds <= currentTime ->
            RegistrationResult.Rejected("DESCRIPTOR_EXPIRED")

        descriptor.protocolVersions.intersect(supportedProtocolVersions).isEmpty() ->
            RegistrationResult.Rejected("INCOMPATIBLE_PROTOCOL")

        else -> null
    }

internal fun isHeartbeatFresh(
    heartbeat: NodeHeartbeatRequest,
    replayRetentionMilliseconds: Long,
    currentTime: Long
): Boolean =
    heartbeat.nonce.isNotBlank() &&
        kotlin.math.abs(currentTime - heartbeat.timestampEpochMilliseconds) <=
        replayRetentionMilliseconds

sealed interface RegistrationResult {
    data object Accepted : RegistrationResult

    data class Rejected(
        val code: String
    ) : RegistrationResult
}
