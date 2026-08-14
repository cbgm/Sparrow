package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.core.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.connection.DefaultTransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.controlplane.HttpControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.controlplane.HttpControlPlaneHealthMonitor
import com.cbgm.sparrow.feature.transport.controlplane.HttpNodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.controlplane.SignedDirectoryControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.discovery.DefaultNodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.HttpNodeDirectorySource
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.registerPlatformNodeDirectoryCache
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.mailbox.HttpMailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.presence.ClientPresenceRouteManager
import com.cbgm.sparrow.feature.transport.presence.ClientRouteRegistrationFactory
import com.cbgm.sparrow.feature.transport.push.HttpPushTokenRegistrationGateway
import com.cbgm.sparrow.feature.transport.push.PushTokenRegistrationGateway
import com.cbgm.sparrow.feature.transport.push.inbox.HttpPendingEnvelopeGateway
import com.cbgm.sparrow.feature.transport.push.inbox.PendingEnvelopeGateway
import com.cbgm.sparrow.feature.transport.routing.DefaultLocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.DefaultLocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.routing.Sha256RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.sender.WebSocketOutgoingWireSender
import com.cbgm.sparrow.feature.transport.websocket.DefaultWebSocketTransportClient
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.sparrow.feature.transport.websocket.createPlatformHttpClient
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val transportModule =
    module {
        single {
            TransportConfig()
        }

        registerPlatformNodeDirectoryCache()

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

        single<WebSocketTransportClient> {
            DefaultWebSocketTransportClient(
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                presenceRouteManager = get<ClientPresenceRouteManager>()
            )
        }

        single<ClientRouteRegistrationFactory> {
            ClientRouteRegistrationFactory(
                signingKeyPairProvider = get<LocalSigningKeyPairProvider>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            ClientPresenceRouteManager(
                httpClient = get<HttpClient>(),
                registrationFactory = get<ClientRouteRegistrationFactory>(),
                localBootstrapRoutingIdProvider = get<LocalBootstrapRoutingIdProvider>()
            )
        }

        single {
            DefaultTransportConnectionManager(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                transportConfig = get<TransportConfig>(),
                nodeEndpointResolver = get<NodeEndpointResolver>(),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                controlPlaneDiscoverySynchronizer = get<NodeControlPlaneDiscoverySynchronizer>()
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

        single<NodeEndpointResolver> {
            DefaultNodeEndpointResolver(
                source = get<NodeDirectorySource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                cache = get(),
                verifier = get(),
                config = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneConfiguration>(),
                controlPlaneStatusStore = get<ControlPlaneStatusStore>()
            )
        }

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
                httpClient = get<HttpClient>(),
                configuration = get<ControlPlaneConfiguration>(),
                statusStore = get<ControlPlaneStatusStore>()
            )
        }

        single<ControlPlaneDirectorySynchronizer> {
            HttpControlPlaneDirectorySynchronizer(
                httpClient = get<HttpClient>(),
                configuration = get<ControlPlaneConfiguration>(),
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

        single<PendingEnvelopeGateway> {
            HttpPendingEnvelopeGateway(
                httpClient = get<HttpClient>(),
                controlPlaneRequestRouter = get<ControlPlaneRequestRouter>()
            )
        }

        single<MailboxGateway> {
            HttpMailboxGateway(httpClient = get<HttpClient>())
        }

        single<PushTokenRegistrationGateway> {
            HttpPushTokenRegistrationGateway(
                httpClient = get<HttpClient>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                controlPlaneRequestRouter = get<ControlPlaneRequestRouter>()
            )
        }

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRoutingIdProvider = get<LocalRoutingIdProvider>(),
                localBootstrapRoutingIdProvider = get<LocalBootstrapRoutingIdProvider>(),
                transportConfig = get<TransportConfig>(),
                mailboxRouteRepository = get()
            )
        }
    }

internal const val GATEWAY_JSON_QUALIFIER = "GatewayJson"
