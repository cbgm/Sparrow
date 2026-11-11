package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor

internal class FederationPeerRouter(
    private val localNodeId: String,
    private val peerNodeDirectory: PeerNodeDirectory,
    private val remoteRouteResolver: RemoteRouteResolver,
    private val remoteFederation: RemoteFederationClient
) {
    suspend fun routeEnvelope(envelope: FederatedEnvelope): FederationAcknowledgement? {
        for (descriptor in peerDescriptors()) {
            val canonicalRoutingId =
                runCatching {
                    remoteRouteResolver.resolve(
                        descriptor = descriptor,
                        routingId = envelope.recipientDeviceRoutingId
                    )
                }.getOrNull()
                    ?: continue

            val acknowledgement =
                runCatching {
                    remoteFederation.deliver(
                        descriptor = descriptor,
                        envelope =
                            envelope.copy(
                                recipientDeviceRoutingId = canonicalRoutingId
                            )
                    )
                }.getOrNull()

            if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return acknowledgement
            }
        }
        return null
    }

    private suspend fun peerDescriptors(): List<SparrowNodeDescriptor> =
        runCatching {
            peerNodeDirectory
                .peers()
                .filterNot { descriptor -> descriptor.nodeId == localNodeId }
        }.getOrDefault(emptyList())
}
