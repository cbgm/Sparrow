package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointRemoteDataSource
import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.rule.isRecoverableConnectivityFailure
import com.cbgm.sparrow.feature.transport.domain.rule.isUnavailableNodeDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Keeps the signed node directory and the known Control Planes fresh around the home node: right after connecting,
 * in the background while connecting from the cached directory, and on a timer while connected.
 */
internal class NodeDirectoryRefresh(
    private val nodeEndpointDataSource: NodeEndpointRemoteDataSource,
    private val controlPlaneDiscoverySynchronizer: NodeControlPlaneDiscoverySynchronizer,
    private val transportConfig: TransportConfig,
    private val diagnostics: TransportDiagnosticsRepositoryImpl
) {
    private val logger = SparrowLog.withTag("NodeDirectoryRefresh")
    private var backgroundJob: Job? = null

    /**
     * Fetches the fresh directory without blocking the connection and updates the known nodes. The fetched directory
     * is cached, so a retry after a failed attempt already uses it.
     */
    fun refreshInBackground(
        scope: CoroutineScope,
        routingId: String
    ) {
        if (backgroundJob?.isActive == true) return
        backgroundJob =
            scope.launch {
                nodeEndpointDataSource.resolve(routingId).fold(
                    onSuccess = diagnostics::resolved,
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        logger.debug { "Node directory refresh failed: ${error.message}" }
                    }
                )
            }
    }

    fun cancel() {
        backgroundJob?.cancel()
    }

    /** Asks the node at [websocketUrl] which Control Planes it knows. */
    suspend fun discoverControlPlanes(websocketUrl: String): Result<Int> =
        controlPlaneDiscoverySynchronizer
            .refreshFromNode(websocketUrl)
            .onFailure { error ->
                if (error is CancellationException) throw error
                if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                    logger.debug { "Control-plane discovery unavailable: ${error.message}" }
                } else {
                    logger.error(error) { "Control-plane discovery failed" }
                }
            }

    /**
     * Until cancelled: the directory every [TransportConfig.directoryRefreshIntervalMilliseconds], handing each fresh
     * one to [onDirectory], and the Control Planes through [endpoint] every [CONTROL_PLANE_DISCOVERY_REFRESH_MILLISECONDS].
     */
    suspend fun whileConnected(
        routingId: String,
        endpoint: NodeEndpoint,
        onDirectory: suspend (List<NodeEndpoint>) -> Unit
    ): Nothing =
        coroutineScope {
            launch {
                while (isActive) {
                    delay(transportConfig.directoryRefreshIntervalMilliseconds.milliseconds)
                    nodeEndpointDataSource.resolve(routingId).fold(
                        onSuccess = { endpoints ->
                            diagnostics.resolved(endpoints)
                            onDirectory(endpoints)
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                                logger.debug { "Signed node directory refresh unavailable: ${error.message}" }
                            } else {
                                // Discovery is optional while the existing connection is active.
                                // Keep the verified cached route; never publish a transient
                                // directory refresh failure as an app-wide transport error.
                                logger.warn { "Signed node directory refresh rejected: ${error.message}" }
                            }
                        }
                    )
                }
            }
            launch {
                while (isActive) {
                    delay(CONTROL_PLANE_DISCOVERY_REFRESH_MILLISECONDS.milliseconds)
                    discoverControlPlanes(endpoint.websocketUrl)
                }
            }
            awaitCancellation()
        }

    private companion object {
        const val CONTROL_PLANE_DISCOVERY_REFRESH_MILLISECONDS = 300_000L
    }
}
