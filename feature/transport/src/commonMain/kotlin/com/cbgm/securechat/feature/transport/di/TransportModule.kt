package com.cbgm.securechat.feature.transport.di

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.core.security.RegistryTrustRoot
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.securechat.core.transport.ControlPlaneHealthMonitor
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.core.transport.TransportDiagnosticsProvider
import com.cbgm.securechat.feature.transport.config.TransportConfig
import com.cbgm.securechat.feature.transport.connection.DefaultTransportConnectionManager
import com.cbgm.securechat.feature.transport.connection.TransportConnectionManager
import com.cbgm.securechat.feature.transport.controlplane.ControlPlaneCandidateVerifier
import com.cbgm.securechat.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.securechat.feature.transport.controlplane.HttpControlPlaneDirectorySynchronizer
import com.cbgm.securechat.feature.transport.controlplane.HttpControlPlaneHealthMonitor
import com.cbgm.securechat.feature.transport.controlplane.HttpNodeControlPlaneDirectorySource
import com.cbgm.securechat.feature.transport.controlplane.NodeControlPlaneDirectorySource
import com.cbgm.securechat.feature.transport.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.securechat.feature.transport.controlplane.SignedDirectoryControlPlaneCandidateVerifier
import com.cbgm.securechat.feature.transport.discovery.DefaultNodeEndpointResolver
import com.cbgm.securechat.feature.transport.discovery.HttpNodeDirectorySource
import com.cbgm.securechat.feature.transport.discovery.NodeDirectorySource
import com.cbgm.securechat.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.securechat.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.securechat.feature.transport.discovery.registerPlatformNodeDirectoryCache
import com.cbgm.securechat.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.securechat.feature.transport.mailbox.HttpMailboxGateway
import com.cbgm.securechat.feature.transport.mailbox.MailboxGateway
import com.cbgm.securechat.feature.transport.presence.ClientPresenceRouteManager
import com.cbgm.securechat.feature.transport.presence.ClientRouteRegistrationFactory
import com.cbgm.securechat.feature.transport.push.HttpPushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.push.PushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.push.inbox.HttpPendingEnvelopeGateway
import com.cbgm.securechat.feature.transport.push.inbox.PendingEnvelopeGateway
import com.cbgm.securechat.feature.transport.routing.DefaultLocalBootstrapRoutingIdProvider
import com.cbgm.securechat.feature.transport.routing.DefaultLocalRoutingIdProvider
import com.cbgm.securechat.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.securechat.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.securechat.feature.transport.routing.RoutingIdGenerator
import com.cbgm.securechat.feature.transport.routing.Sha256RoutingIdGenerator
import com.cbgm.securechat.feature.transport.sender.WebSocketOutgoingWireSender
import com.cbgm.securechat.feature.transport.websocket.DefaultWebSocketTransportClient
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.feature.transport.websocket.createPlatformHttpClient
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val transportModule =
    module {
        single {
            TransportConfig(
                trustedRegistryRootNodeId = RegistryTrustRoot.NODE_ID
            )
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
                configuration = get<ControlPlaneConfiguration>()
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
