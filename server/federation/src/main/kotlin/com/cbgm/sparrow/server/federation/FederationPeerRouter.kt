package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor

internal class FederationPeerRouter(
    private val localNodeId: String,
    private val peerNodeDirectory: PeerNodeDirectory,
    private val remoteRouteResolver: RemoteRouteResolver,
    private val remoteFederation: RemoteFederationClient,
    private val remoteTypingFederation: RemoteTypingFederationClient
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

    suspend fun routeTyping(event: FederatedTypingEvent): Boolean {
        for (descriptor in peerDescriptors()) {
            val canonicalRoutingId =
                runCatching {
                    remoteRouteResolver.resolve(
                        descriptor = descriptor,
                        routingId = event.recipientRoutingId
                    )
                }.getOrNull()
                    ?: continue

            val delivered =
                runCatching {
                    remoteTypingFederation.deliver(
                        descriptor = descriptor,
                        event = event.copy(recipientRoutingId = canonicalRoutingId)
                    )
                }.getOrDefault(false)

            if (delivered) {
                return true
            }
        }
        return false
    }

    private suspend fun peerDescriptors(): List<SparrowNodeDescriptor> =
        runCatching {
            peerNodeDirectory
                .peers()
                .filterNot { descriptor -> descriptor.nodeId == localNodeId }
        }.getOrDefault(emptyList())
}
