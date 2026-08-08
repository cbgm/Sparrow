package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DefaultNodeEndpointResolver(
    private val source: NodeDirectorySource,
    private val json: Json,
    private val cache: NodeDirectoryCache,
    private val verifier: NodeDirectoryVerifier,
    private val config: RelayTransportConfig,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) : NodeEndpointResolver {
    private val logger = SecureChatLog.withTag("DefaultNodeEndpointResolver")
    private val resolutionMutex = Mutex()

    override suspend fun resolve(localRelayId: String): Result<List<NodeEndpoint>> =
        runCatching {
            require(localRelayId.isNotBlank()) {
                "Local relay ID must not be blank"
            }

            resolutionMutex.withLock {
                resolveLocked(localRelayId)
            }
        }

    private suspend fun resolveLocked(localRelayId: String): List<NodeEndpoint> =
        resolveRegistry(
            registryBaseUrl = config.nodeRegistryBaseUrl,
            localRelayId = localRelayId
        )

    private suspend fun resolveRegistry(
        registryBaseUrl: String,
        localRelayId: String
    ): List<NodeEndpoint> {
        val cached = cache.read()
        val cachedDirectory = cached?.decode()
        val currentTime = now()
        val trustedAuthorityNodeId =
            config.trustedRegistryAuthorityNodeId ?: cached?.trustedAuthorityNodeId

        if (isReusable(cachedDirectory, trustedAuthorityNodeId, currentTime)) {
            return checkNotNull(cachedDirectory).endpointsFor(localRelayId)
        }

        val selectedDirectory =
            fetchAndCache(
                registryBaseUrl = registryBaseUrl,
                cached = cached,
                currentTime = currentTime
            ).getOrElse { remoteError ->
                cachedFallback(
                    cachedDirectory = cachedDirectory,
                    trustedAuthorityNodeId = trustedAuthorityNodeId,
                    currentTime = currentTime,
                    remoteError = remoteError
                )
            }
        return selectedDirectory.endpointsFor(localRelayId)
    }

    private suspend fun isReusable(
        cachedDirectory: SignedNodeDirectory?,
        trustedAuthorityNodeId: String?,
        currentTime: Long
    ): Boolean {
        if (cachedDirectory == null || trustedAuthorityNodeId == null) {
            return false
        }
        val cacheAge = currentTime - cachedDirectory.directory.generatedAtEpochMilliseconds
        return cacheAge < config.directoryRefreshIntervalMilliseconds &&
            verifier
                .verify(
                    signedDirectory = cachedDirectory,
                    trustedAuthorityNodeId = trustedAuthorityNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).isSuccess
    }

    private suspend fun fetchAndCache(
        registryBaseUrl: String,
        cached: CachedNodeDirectory?,
        currentTime: Long
    ): Result<SignedNodeDirectory> =
        fetchDirectory(registryBaseUrl).mapCatching { remoteDirectory ->
            val authorityNodeId = trustedAuthority(remoteDirectory, cached)
            verifier
                .verify(
                    signedDirectory = remoteDirectory,
                    trustedAuthorityNodeId = authorityNodeId,
                    supportedProtocolVersion = config.supportedProtocolVersion,
                    nowEpochMilliseconds = currentTime
                ).getOrThrow()
            cacheVerifiedDirectory(remoteDirectory, authorityNodeId)
            remoteDirectory
        }

    private fun trustedAuthority(
        remoteDirectory: SignedNodeDirectory,
        cached: CachedNodeDirectory?
    ): String =
        config.trustedRegistryAuthorityNodeId
            ?: cached?.trustedAuthorityNodeId
            ?: verifier.authorityNodeId(remoteDirectory).getOrThrow().also { authorityNodeId ->
                logger.warn {
                    "Trusting registry authority $authorityNodeId on first use; " +
                        "configure its node ID for production"
                }
            }

    private suspend fun cacheVerifiedDirectory(
        remoteDirectory: SignedNodeDirectory,
        authorityNodeId: String
    ) {
        runCatching {
            cache.write(
                CachedNodeDirectory(
                    encodedDirectory = json.encodeToString(remoteDirectory),
                    trustedAuthorityNodeId = authorityNodeId
                )
            )
        }.onFailure { error ->
            logger.warn {
                "Signed node directory could not be cached: ${error.message ?: "unknown error"}"
            }
        }
    }

    private suspend fun cachedFallback(
        cachedDirectory: SignedNodeDirectory?,
        trustedAuthorityNodeId: String?,
        currentTime: Long,
        remoteError: Throwable
    ): SignedNodeDirectory {
        val fallbackDirectory = cachedDirectory ?: throw remoteError
        val fallbackAuthorityNodeId = trustedAuthorityNodeId ?: throw remoteError
        val cacheExpiry =
            fallbackDirectory.directory.validUntilEpochMilliseconds +
                config.cachedDirectoryGraceMilliseconds

        verifier
            .verify(
                signedDirectory = fallbackDirectory,
                trustedAuthorityNodeId = fallbackAuthorityNodeId,
                supportedProtocolVersion = config.supportedProtocolVersion,
                nowEpochMilliseconds = currentTime,
                allowDirectoryExpiredUntilEpochMilliseconds = cacheExpiry
            ).getOrElse { throw remoteError }

        logger.warn {
            "Registry unavailable; reconnecting with the last valid signed directory"
        }
        return fallbackDirectory
    }

    private suspend fun fetchDirectory(registryBaseUrl: String): Result<SignedNodeDirectory> =
        source.fetch(registryBaseUrl).mapCatching { encodedDirectory ->
            json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        }

    private fun CachedNodeDirectory.decode(): SignedNodeDirectory? =
        runCatching {
            json.decodeFromString<SignedNodeDirectory>(encodedDirectory)
        }.getOrNull()

    private fun SignedNodeDirectory.endpointsFor(localRelayId: String): List<NodeEndpoint> {
        val endpoints =
            directory.nodes
                .filter { descriptor ->
                    config.supportedProtocolVersion in descriptor.protocolVersions &&
                        NodeCapability.GATEWAY in descriptor.capabilities
                }.map { descriptor ->
                    NodeEndpoint(
                        nodeId = descriptor.nodeId,
                        websocketUrl = descriptor.clientEndpoint,
                        mailboxRouteEndpoint = descriptor.mailboxEndpoint,
                        mailboxAccessEndpoint =
                            descriptor.mailboxEndpoint.clientAccessibleFrom(descriptor.clientEndpoint)
                    )
                }.distinctBy(NodeEndpoint::nodeId)
                .sortedBy(NodeEndpoint::nodeId)

        check(endpoints.isNotEmpty()) {
            "Node directory does not contain a compatible gateway"
        }

        val startIndex = (localRelayId.hashCode() and Int.MAX_VALUE) % endpoints.size
        return endpoints.drop(startIndex) + endpoints.take(startIndex)
    }
}

private fun String.clientAccessibleFrom(clientEndpoint: String): String {
    val internalHost = substringAfter("://").substringBefore('/').substringBefore(':')
    if (internalHost !in setOf("localhost", "mailbox", "mailbox-b")) return this
    val clientAuthority = clientEndpoint.substringAfter("://").substringBefore('/')
    val clientHost = clientAuthority.substringBefore(':')
    val gatewayPort = clientAuthority.substringAfter(':', "8094").toIntOrNull() ?: 8094
    val mailboxPort = gatewayPort - if (gatewayPort >= 8_200) 102 else 2
    return "http://$clientHost:$mailboxPort"
}
