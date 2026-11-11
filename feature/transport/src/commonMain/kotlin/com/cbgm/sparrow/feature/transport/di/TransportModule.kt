package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.feature.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.transport.background.ConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.background.ConnectionModeStore
import com.cbgm.sparrow.feature.transport.background.DataStoreConnectionModeStore
import com.cbgm.sparrow.feature.transport.blob.BlobGateway
import com.cbgm.sparrow.feature.transport.blob.CircuitBlobGateway
import com.cbgm.sparrow.feature.transport.circuit.CircuitCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitMailboxEngine
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.DataStoreCircuitCountStore
import com.cbgm.sparrow.feature.transport.circuit.DataStoreCircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.DataStoreCircuitHopCountStore
import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.circuit.FileTransferCircuits
import com.cbgm.sparrow.feature.transport.circuit.createMailboxHttpClient
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.connection.DefaultTransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneConfigurationImpl
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.controlplane.HttpControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.controlplane.HttpControlPlaneHealthMonitor
import com.cbgm.sparrow.feature.transport.controlplane.HttpNodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.controlplane.KEY_TRUSTED_DIRECTORY_PUBLIC_KEY
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.controlplane.SignedControlPlaneDirectoryVerifier
import com.cbgm.sparrow.feature.transport.controlplane.SignedDirectoryControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.device.createPlatformHttpClient
import com.cbgm.sparrow.feature.transport.discovery.DataStoreNodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.DataStorePreferredNodeStore
import com.cbgm.sparrow.feature.transport.discovery.DefaultNodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.HttpNodeDirectorySource
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointSelector
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.CreateGatewaySessionProofUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.GetGatewayNodesUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveCircuitStatusUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObservePreferredNodeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveWaitingForRouteUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.PrepareControlPlaneUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitFallbackUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetCircuitHopCountUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetPreferredNodeUseCase
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.mailbox.HttpMailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.presence.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.presence.ClientPresenceRouteCoordinator
import com.cbgm.sparrow.feature.transport.presence.ClientRouteRegistrationFactory
import com.cbgm.sparrow.feature.transport.presence.PhoneAttestationRemoteSource
import com.cbgm.sparrow.feature.transport.relay.ControlPlaneRelayPolicy
import com.cbgm.sparrow.feature.transport.relay.OnionRelayClient
import com.cbgm.sparrow.feature.transport.relay.OnionRelayEngine
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import com.cbgm.sparrow.feature.transport.relay.createControlPlaneHttpClient
import com.cbgm.sparrow.feature.transport.routing.DefaultLocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.DefaultLocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.routing.Sha256RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.sender.WebSocketOutgoingWireSender
import com.cbgm.sparrow.feature.transport.websocket.DefaultWebSocketTransportClient
import com.cbgm.sparrow.feature.transport.websocket.GatewayPendingRequestRegistry
import com.cbgm.sparrow.feature.transport.websocket.GatewayServerMessageHandler
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.protocol.transport.OutgoingWireSender
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val transportModule =
    module {
        single {
            TransportConfig()
        }

        single<NodeDirectoryCache> {
            DataStoreNodeDirectoryCache(
                dataStore = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<ControlPlaneConfigurationImpl> {
            ControlPlaneConfigurationImpl(dataStore = get(), nodeDirectoryCache = get())
        }

        single<ControlPlaneConfiguration> {
            get<ControlPlaneConfigurationImpl>()
        }

        single<ControlPlaneStatusStore> {
            get<ControlPlaneConfigurationImpl>()
        }

        single<HttpClient> {
            createPlatformHttpClient(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<Json>(qualifier = named(GATEWAY_JSON_QUALIFIER)) {
            createGatewayJson()
        }

        single<RoutingIdGenerator> {
            Sha256RoutingIdGenerator(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single<LocalRoutingIdProvider> {
            DefaultLocalRoutingIdProvider(
                localSigningPublicKeyProvider = get<LocalSigningPublicKeyProvider>(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<LocalBootstrapRoutingIdProvider> {
            DefaultLocalBootstrapRoutingIdProvider(
                localPhoneNumberProvider = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single {
            GatewayPendingRequestRegistry()
        }

        single {
            GatewayServerMessageHandler(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                pendingRequestRegistry = get<GatewayPendingRequestRegistry>()
            )
        }

        single<WebSocketTransportClient> {
            DefaultWebSocketTransportClient(
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                presenceRouteCoordinator = get<ClientPresenceRouteCoordinator>(),
                serverMessageHandler = get<GatewayServerMessageHandler>(),
                pendingRequestRegistry = get<GatewayPendingRequestRegistry>(),
                createGatewaySessionProof = get<CreateGatewaySessionProofUseCase>()
            )
        }

        singleOf(::CreateGatewaySessionProofUseCase)

        single<ClientRouteRegistrationFactory> {
            ClientRouteRegistrationFactory(
                signingKeyPairProvider = get<LocalSigningKeyPairProvider>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            ControlPlaneRelayPolicy(
                transportConfig = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                cache = get<NodeDirectoryCache>()
            )
        }

        // One selector for the HTTP relay and the circuits, so a relay that failed for one is left out by both.
        single {
            RelaySelector(
                cache = get<NodeDirectoryCache>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                preferredNodeStore = get<PreferredNodeStore>(),
                // Read when a route is built: the connection manager itself uses the relayed client.
                connectedNodeId = { get<TransportConnectionManager>().connectedNodeId }
            )
        }

        single {
            CircuitRunner(
                httpClient = get<HttpClient>(),
                cipher = get<CircuitCipher>(),
                selector = get<RelaySelector>(),
                hopCountStore = get<CircuitHopCountStore>(),
                circuitCountStore = get<CircuitCountStore>()
            )
        }

        single<CircuitHopCountStore> { DataStoreCircuitHopCountStore(dataStore = get()) }
        single<CircuitCountStore> { DataStoreCircuitCountStore(dataStore = get()) }

        single<CircuitFallbackStore> { DataStoreCircuitFallbackStore(dataStore = get()) }
        single<ConnectionModeStore> { DataStoreConnectionModeStore(dataStore = get()) }
        single { ConnectionKeepAlive() }
        single { DirectSendingPolicy(circuits = get(), fallback = get()) }

        single {
            OnionRelayClient(
                selector = get<RelaySelector>(),
                cipher = get<SealedMessageCipher>(),
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
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
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                relayPolicy = get<ControlPlaneRelayPolicy>(),
                directoryVerifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>(),
                signingKeyPairProvider = get<LocalSigningKeyPairProvider>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                localBootstrapRoutingIdProvider = get<LocalBootstrapRoutingIdProvider>(),
                localPhoneNumberProvider = get<LocalPhoneNumberProvider>(),
                phoneAttestationRemoteSource = get<PhoneAttestationRemoteSource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            PhoneAttestationRemoteSource(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                pinnedDirectoryKey = { get<SparrowDataStore>().getString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY) },
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            ClientPresenceRouteCoordinator(
                httpClient = get<HttpClient>(),
                registrationFactory = get<ClientRouteRegistrationFactory>(),
                aliasClaimRegistrar = get<AliasClaimRegistrar>()
            )
        }

        single {
            DefaultTransportConnectionManager(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                transportConfig = get<TransportConfig>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                controlPlaneDiscoverySynchronizer = get<NodeControlPlaneDiscoverySynchronizer>(),
                preferredNodeStore = get<PreferredNodeStore>()
            )
        }

        single<TransportConnectionManager> {
            get<DefaultTransportConnectionManager>()
        }

        single<TransportDiagnosticsProvider> {
            get<DefaultTransportConnectionManager>()
        }

        single {
            NodeDirectoryVerifier(
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cryptoHash = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            NodeEndpointSelector(config = get<TransportConfig>())
        }

        single<NodeEndpointResolver> {
            DefaultNodeEndpointResolver(
                source = HttpNodeDirectorySource(httpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER))),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                cache = get(),
                verifier = get(),
                config = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                controlPlaneStatusStore = get<ControlPlaneStatusStore>(),
                endpointSelector = get<NodeEndpointSelector>(),
                controlPlaneDirectorySynchronizer = get<ControlPlaneDirectorySynchronizer>(),
                preferredNodeStore = get<PreferredNodeStore>()
            )
        }

        single<PreferredNodeStore> { DataStorePreferredNodeStore(dataStore = get()) }

        single<NodeDirectorySource> {
            HttpNodeDirectorySource(httpClient = get<HttpClient>())
        }

        single {
            ControlPlaneRequestRouter(
                configuration = get<ControlPlaneConfiguration>(),
                statusStore = get<ControlPlaneStatusStore>()
            )
        }

        single<ControlPlaneHealthMonitor> {
            HttpControlPlaneHealthMonitor(
                httpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                configuration = get<ControlPlaneConfiguration>(),
                statusStore = get<ControlPlaneStatusStore>()
            )
        }

        single {
            SignedControlPlaneDirectoryVerifier(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                hash = get<CryptoHash>()
            )
        }

        single<ControlPlaneDirectorySynchronizer> {
            HttpControlPlaneDirectorySynchronizer(
                httpClient = get<HttpClient>(),
                configuration = get<ControlPlaneConfiguration>(),
                verifier = get<SignedControlPlaneDirectoryVerifier>(),
                dataStore = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<NodeControlPlaneDirectorySource> {
            HttpNodeControlPlaneDirectorySource(
                httpClient = get<HttpClient>()
            )
        }

        single<ControlPlaneCandidateVerifier> {
            SignedDirectoryControlPlaneCandidateVerifier(
                nodeDirectorySource = get<NodeDirectorySource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                verifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            NodeControlPlaneDiscoverySynchronizer(
                source = get<NodeControlPlaneDirectorySource>(),
                candidateVerifier = get<ControlPlaneCandidateVerifier>(),
                configuration = get<ControlPlaneConfiguration>()
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
                                fallback = get<CircuitFallbackStore>(),
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
                        fallback = get<CircuitFallbackStore>(),
                        directClient = get<HttpClient>()
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                creationWork = MailboxCreationWorkSolver(get<CryptoHash>()),
                cryptoHash = get<CryptoHash>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                randomBytes = get<SecureRandomGenerator>().let { random -> { size -> random.generateBytes(size).getOrThrow() } }
            )
        }

        singleOf(::GetGatewayNodesUseCase)
        singleOf(::ObservePreferredNodeUseCase)
        singleOf(::SetPreferredNodeUseCase)
        singleOf(::ObserveCircuitHopCountUseCase)
        singleOf(::SetCircuitHopCountUseCase)
        singleOf(::ObserveCircuitCountUseCase)
        singleOf(::SetCircuitCountUseCase)
        singleOf(::ObserveCircuitStatusUseCase)
        singleOf(::ObserveCircuitFallbackUseCase)
        singleOf(::SetCircuitFallbackUseCase)
        singleOf(::ObserveWaitingForRouteUseCase)
        singleOf(::ObserveConnectionModeUseCase)
        singleOf(::SetConnectionModeUseCase)
        singleOf(::CanConnectWhileClosedUseCase)
        singleOf(::PrepareControlPlaneUseCase)

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                localBootstrapRoutingIdProvider = get<LocalBootstrapRoutingIdProvider>(),
                transportConfig = get<TransportConfig>(),
                mailboxRouteRepository = get(),
                mailboxGateway = get<MailboxGateway>(),
                sendingPolicy = get<DirectSendingPolicy>()
            )
        }
    }

internal const val GATEWAY_JSON_QUALIFIER = "GatewayJson"
internal const val CONTROL_PLANE_HTTP_CLIENT_QUALIFIER = "ControlPlaneHttpClient"
