package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneConfigurationRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneDirectoryRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneEndpointStore
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneHealthRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneStatusLocalDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneStatusRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.controlplane.HttpNodeControlPlaneDirectoryRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.InvitationRouteRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.KEY_TRUSTED_DIRECTORY_PUBLIC_KEY
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDirectoryRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.controlplane.PhoneAttestationRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.controlplane.SignedControlPlaneDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.controlplane.SignedDirectoryControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.route.relay.ControlPlaneRelayPolicy
import com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayClient
import com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayEngine
import com.cbgm.sparrow.feature.transport.data.route.relay.createControlPlaneHttpClient
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneStatusRepository
import com.cbgm.sparrow.feature.transport.domain.repository.InvitationMailboxRepository
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The Control Planes: their addresses, health, directory and the requests that go to them through relays. */
internal val controlPlaneModule =
    module {
        single { ControlPlaneStatusLocalDataSource() }

        single<ControlPlaneConfigurationRepositoryImpl> {
            ControlPlaneConfigurationRepositoryImpl(
                dataStore = get(),
                nodeDirectoryCache = get(),
                endpointStatuses = get<ControlPlaneStatusLocalDataSource>()
            )
        }

        single<ControlPlaneEndpointStore> {
            get<ControlPlaneConfigurationRepositoryImpl>()
        }

        single<ControlPlaneConfigurationRepository> {
            get<ControlPlaneConfigurationRepositoryImpl>()
        }

        single {
            ControlPlaneRelayPolicy(
                transportConfig = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                cache = get<NodeDirectoryCache>()
            )
        }

        // Requests to Control Planes go through two relays, so no Control Plane sees the app's address.
        single<HttpClient>(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)) {
            val policy = get<ControlPlaneRelayPolicy>()
            createControlPlaneHttpClient(
                engine =
                    OnionRelayEngine(
                        onion = get<OnionRelayClient>(),
                        directClient = get<HttpClient>().config { expectSuccess = false },
                        requirement = policy::requirement
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            AliasClaimRegistrar(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                relayPolicy = get<ControlPlaneRelayPolicy>(),
                directoryVerifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>(),
                signingKeyPairSource = get<LocalSigningKeyPairSource>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                routingIdRepository = get<RoutingIdRepository>(),
                localPhoneNumberSource = get<LocalPhoneNumberSource>(),
                phoneAttestationRemoteDataSource = get<PhoneAttestationRemoteDataSource>(),
                invitationMailbox = get<InvitationMailboxRepository>(),
                encryptionKeyPairSource = get<LocalEncryptionKeyPairSource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            InvitationRouteRemoteDataSource(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                work = MailboxCreationWorkSolver(get<CryptoHash>()),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            PhoneAttestationRemoteDataSource(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                pinnedDirectoryKey = { get<SparrowDataStore>().getString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY) },
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            ControlPlaneRequestRouter(
                configuration = get<ControlPlaneEndpointStore>(),
                statusStore = get<ControlPlaneStatusRepository>()
            )
        }

        single<ControlPlaneStatusRepository> {
            ControlPlaneStatusRepositoryImpl(
                localDataSource = get<ControlPlaneStatusLocalDataSource>(),
                healthRemoteDataSource =
                    ControlPlaneHealthRemoteDataSource(
                        httpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER))
                    )
            )
        }

        single {
            SignedControlPlaneDirectoryVerifier(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                hash = get<CryptoHash>()
            )
        }

        single<ControlPlaneDirectoryRepository> {
            ControlPlaneDirectoryRepositoryImpl(
                httpClient = get<HttpClient>(),
                configuration = get<ControlPlaneEndpointStore>(),
                verifier = get<SignedControlPlaneDirectoryVerifier>(),
                dataStore = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<NodeControlPlaneDirectoryRemoteDataSource> {
            HttpNodeControlPlaneDirectoryRemoteDataSource(
                httpClient = get<HttpClient>()
            )
        }

        single<ControlPlaneCandidateVerifier> {
            SignedDirectoryControlPlaneCandidateVerifier(
                nodeDirectoryRemoteDataSource = get<NodeDirectoryRemoteDataSource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                verifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            NodeControlPlaneDiscoverySynchronizer(
                source = get<NodeControlPlaneDirectoryRemoteDataSource>(),
                candidateVerifier = get<ControlPlaneCandidateVerifier>(),
                configuration = get<ControlPlaneEndpointStore>()
            )
        }
    }
