package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.blob.BlobRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.InvitationRouteRemoteSource
import com.cbgm.sparrow.feature.transport.data.mailbox.DataStoreInvitationMailboxStore
import com.cbgm.sparrow.feature.transport.data.mailbox.InvitationLetterRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.mailbox.InvitationMailboxRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.mailbox.InvitationMailboxStore
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxOutgoingWireSender
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitMailboxEngine
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransferCircuits
import com.cbgm.sparrow.feature.transport.data.route.circuit.createMailboxHttpClient
import com.cbgm.sparrow.feature.transport.domain.repository.BlobRepository
import com.cbgm.sparrow.feature.transport.domain.repository.InvitationLetterRepository
import com.cbgm.sparrow.feature.transport.domain.repository.InvitationMailboxRepository
import com.cbgm.sparrow.feature.transport.domain.repository.MailboxRepository
import com.cbgm.sparrow.feature.transport.domain.repository.NodeEndpointRepository
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator
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

        single<InvitationMailboxRepository> {
            InvitationMailboxRepositoryImpl(
                nodeEndpointResolver = get<NodeEndpointRepository>(),
                routingIdRepository = get<RoutingIdRepository>(),
                mailboxRepository = get<MailboxRepository>(),
                store = get<InvitationMailboxStore>()
            )
        }

        single<InvitationLetterRepository> {
            InvitationLetterRepositoryImpl(
                routes = get<InvitationRouteRemoteSource>(),
                mailboxRepository = get<MailboxRepository>(),
                signingKeyPairSource = get<LocalSigningKeyPairSource>(),
                encryptionKeyPairSource = get<LocalEncryptionKeyPairSource>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                transportCipher = get<TransportMessageCipher>(),
                payloadCodec = get<TransportPayloadCodec>(),
                routingIdGenerator = get<RoutingIdGenerator>(),
                routingIdRepository = get<RoutingIdRepository>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<MailboxRepository> {
            // Mailbox requests go through the circuit, so the mailbox node does not see the device's address.
            MailboxRepositoryImpl(
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

        single<BlobRepository> {
            // Every file transfer gets a circuit of its own, so the blob node does not see the device's address.
            BlobRepositoryImpl(
                transfers =
                    FileTransferCircuits(
                        circuits = get<CircuitRunner>(),
                        preferences = get(),
                        directClient = get<HttpClient>()
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                creationWork = MailboxCreationWorkSolver(get<CryptoHash>()),
                cryptoHash = get<CryptoHash>(),
                nodeEndpointResolver = get<NodeEndpointRepository>(),
                routingIdRepository = get<RoutingIdRepository>(),
                randomBytes = get<SecureRandomGenerator>().let { random -> { size -> random.generateBytes(size).getOrThrow() } }
            )
        }

        single<OutgoingWireSender> {
            MailboxOutgoingWireSender(
                routingIdRepository = get<RoutingIdRepository>(),
                transportConfig = get<TransportConfig>(),
                mailboxRouteRepository = get(),
                mailboxRepository = get<MailboxRepository>(),
                invitationLetters = get<InvitationLetterRepository>(),
                aliasBinding = get<AliasClaimRegistrar>()
            )
        }
    }
