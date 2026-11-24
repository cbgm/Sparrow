package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.blob.CircuitBlobGateway
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.InvitationRouteRemoteSource
import com.cbgm.sparrow.feature.transport.data.mailbox.DataStoreInvitationMailboxStore
import com.cbgm.sparrow.feature.transport.data.mailbox.HttpMailboxGateway
import com.cbgm.sparrow.feature.transport.data.mailbox.InvitationMailboxRenewal
import com.cbgm.sparrow.feature.transport.data.mailbox.InvitationMailboxStore
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxOutgoingWireSender
import com.cbgm.sparrow.feature.transport.data.mailbox.SignedInvitationLetters
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitMailboxEngine
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransferCircuits
import com.cbgm.sparrow.feature.transport.data.route.circuit.createMailboxHttpClient
import com.cbgm.sparrow.feature.transport.domain.gateway.BlobGateway
import com.cbgm.sparrow.feature.transport.domain.gateway.InvitationLetters
import com.cbgm.sparrow.feature.transport.domain.gateway.InvitationMailbox
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalBootstrapRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.MailboxGateway
import com.cbgm.sparrow.feature.transport.domain.gateway.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.domain.gateway.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import com.cbgm.sparrow.protocol.transport.OutgoingWireSender
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Mailboxes, invitation letters and file transfers, all through the circuits. */
internal val mailboxModule =
    module {
        single<InvitationMailboxStore> {
            DataStoreInvitationMailboxStore(dataStore = get(), json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)))
        }

        single<InvitationMailbox> {
            InvitationMailboxRenewal(
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                mailboxGateway = get<MailboxGateway>(),
                store = get<InvitationMailboxStore>()
            )
        }

        single<InvitationLetters> {
            SignedInvitationLetters(
                routes = get<InvitationRouteRemoteSource>(),
                mailboxGateway = get<MailboxGateway>(),
                signingKeyPairSource = get<LocalSigningKeyPairSource>(),
                encryptionKeyPairSource = get<LocalEncryptionKeyPairSource>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                transportCipher = get<TransportMessageCipher>(),
                payloadCodec = get<TransportPayloadCodec>(),
                routingIdGenerator = get<RoutingIdGenerator>(),
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                localBootstrapRoutingIdSource = get<LocalBootstrapRoutingIdSource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<MailboxGateway> {
            // Mailbox requests go through the circuit, so the mailbox node does not see the device's address.
            HttpMailboxGateway(
                httpClient =
                    createMailboxHttpClient(
                        engine =
                            CircuitMailboxEngine(
                                circuits = get<CircuitRunner>(),
                                preferences = get(),
                                directClient = get<HttpClient>()
                            ),
                        json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                creationWork = MailboxCreationWorkSolver(get<CryptoHash>()),
                circuitSlot = get<CircuitRunner>()::slotOf
            )
        }

        single<BlobGateway> {
            // Every file transfer gets a circuit of its own, so the blob node does not see the device's address.
            CircuitBlobGateway(
                transfers =
                    FileTransferCircuits(
                        circuits = get<CircuitRunner>(),
                        preferences = get(),
                        directClient = get<HttpClient>()
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                creationWork = MailboxCreationWorkSolver(get<CryptoHash>()),
                cryptoHash = get<CryptoHash>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                randomBytes = get<SecureRandomGenerator>().let { random -> { size -> random.generateBytes(size).getOrThrow() } }
            )
        }

        single<OutgoingWireSender> {
            MailboxOutgoingWireSender(
                localRoutingIdSource = get<LocalRoutingIdSource>(),
                localBootstrapRoutingIdSource = get<LocalBootstrapRoutingIdSource>(),
                transportConfig = get<TransportConfig>(),
                mailboxRouteRepository = get(),
                mailboxGateway = get<MailboxGateway>(),
                invitationLetters = get<InvitationLetters>(),
                aliasBinding = get<AliasClaimRegistrar>()
            )
        }
    }
