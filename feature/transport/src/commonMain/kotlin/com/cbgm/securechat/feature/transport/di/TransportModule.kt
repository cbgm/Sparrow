package com.cbgm.securechat.feature.transport.di

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.core.transport.TransportDiagnosticsProvider
import com.cbgm.securechat.feature.transport.connection.DefaultRelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.discovery.DefaultNodeEndpointResolver
import com.cbgm.securechat.feature.transport.discovery.HttpNodeDirectorySource
import com.cbgm.securechat.feature.transport.discovery.NodeDirectorySource
import com.cbgm.securechat.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.securechat.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.securechat.feature.transport.discovery.registerPlatformNodeDirectoryCache
import com.cbgm.securechat.feature.transport.mailbox.HttpMailboxGateway
import com.cbgm.securechat.feature.transport.mailbox.MailboxGateway
import com.cbgm.securechat.feature.transport.push.HttpPushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.push.PushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.DefaultLocalBootstrapRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.DefaultLocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalBootstrapRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.feature.transport.relay.identity.Sha256RelayIdGenerator
import com.cbgm.securechat.feature.transport.relay.inbox.HttpPendingRelayEnvelopeGateway
import com.cbgm.securechat.feature.transport.relay.inbox.PendingRelayEnvelopeGateway
import com.cbgm.securechat.feature.transport.relay.presence.ClientPresenceRouteManager
import com.cbgm.securechat.feature.transport.relay.presence.ClientRouteRegistrationFactory
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

        registerPlatformNodeDirectoryCache()

        single<HttpClient> {
            createPlatformHttpClient(
                json = get(qualifier = named(RELAY_JSON_QUALIFIER))
            )
        }

        single<Json>(qualifier = named(RELAY_JSON_QUALIFIER)) {
            createRelayJson()
        }

        single<RelayIdGenerator> {
            Sha256RelayIdGenerator(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single<LocalRelayIdProvider> {
            DefaultLocalRelayIdProvider(
                localSigningPublicKeyProvider = get<LocalSigningPublicKeyProvider>(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<LocalBootstrapRelayIdProvider> {
            DefaultLocalBootstrapRelayIdProvider(
                localPhoneNumberProvider = get(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<WebSocketTransportClient> {
            DefaultWebSocketTransportClient(
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(RELAY_JSON_QUALIFIER)),
                presenceRouteManager = get<ClientPresenceRouteManager>()
            )
        }

        single<ClientRouteRegistrationFactory> {
            ClientRouteRegistrationFactory(
                signingKeyPairProvider = get<LocalSigningKeyPairProvider>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                json = get(qualifier = named(RELAY_JSON_QUALIFIER))
            )
        }

        single {
            ClientPresenceRouteManager(
                httpClient = get<HttpClient>(),
                registrationFactory = get<ClientRouteRegistrationFactory>(),
                localBootstrapRelayIdProvider = get<LocalBootstrapRelayIdProvider>()
            )
        }

        single {
            DefaultRelayConnectionManager(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>(),
                nodeEndpointResolver = get<NodeEndpointResolver>()
            )
        }

        single<RelayConnectionManager> {
            get<DefaultRelayConnectionManager>()
        }

        single<TransportDiagnosticsProvider> {
            get<DefaultRelayConnectionManager>()
        }

        single {
            NodeDirectoryVerifier(
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cryptoHash = get(),
                json = get(qualifier = named(RELAY_JSON_QUALIFIER))
            )
        }

        single<NodeEndpointResolver> {
            DefaultNodeEndpointResolver(
                source = get<NodeDirectorySource>(),
                json = get(qualifier = named(RELAY_JSON_QUALIFIER)),
                cache = get(),
                verifier = get(),
                config = get<RelayTransportConfig>()
            )
        }

        single<NodeDirectorySource> {
            HttpNodeDirectorySource(httpClient = get<HttpClient>())
        }

        single<PendingRelayEnvelopeGateway> {
            HttpPendingRelayEnvelopeGateway(
                httpClient = get<HttpClient>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<MailboxGateway> {
            HttpMailboxGateway(httpClient = get<HttpClient>())
        }

        single<PushTokenRegistrationGateway> {
            HttpPushTokenRegistrationGateway(
                httpClient = get<HttpClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                localBootstrapRelayIdProvider = get<LocalBootstrapRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>(),
                mailboxRouteRepository = get()
            )
        }
    }

internal const val RELAY_JSON_QUALIFIER = "RelayJson"
